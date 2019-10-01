/*
 * The MIT License
 *
 * Copyright (c) 2019, NVIDIA CORPORATION.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.gpuopenanalytics.jenkins.remotedocker;

import com.google.common.collect.ImmutableList;
import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.job.SideDockerConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.pipeline.DockerState;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.WorkspaceList;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Jenkins {@link Launcher} that delegates into a running docker container
 */
public class DockerLauncher extends AbstractDockerLauncher {

    private boolean debug;
    private Launcher delegate;
    private TaskListener listener;
    private RemoteDockerBuildWrapper buildWrapper;
    private EnvVars environment;
    private Computer node;
    private FilePath workspace;

    private transient Optional<DockerNetwork> network = Optional.empty();
    private transient List<String> containerIds = new ArrayList<>();
    private transient String mainContainerId;

    /**
     * @param environment
     * @param node
     * @param workspace
     * @param delegate     the launcher on the node executing the job
     * @param listener     listener for logging
     * @param buildWrapper the {@link RemoteDockerBuildWrapper} currently
     *                     running
     */
    public DockerLauncher(boolean debug,
                          EnvVars environment,
                          Computer node,
                          FilePath workspace,
                          Launcher delegate,
                          TaskListener listener,
                          RemoteDockerBuildWrapper buildWrapper) {
        super(delegate);
        this.debug = debug;
        this.environment = environment;
        this.node = node;
        this.workspace = workspace;
        this.delegate = delegate;
        this.listener = listener;
        this.buildWrapper = buildWrapper;
    }

    public static DockerLauncher decorateExistingContainers(DockerLauncher launcher,
                                                            String mainContainerId,
                                                            Collection<String> containerIds,
                                                            Optional<String> networkId) {
        launcher.mainContainerId = mainContainerId;
        launcher.containerIds = new ArrayList<>(containerIds);
        launcher.network = networkId.map(DockerNetwork::fromExisting);
        return launcher;
    }

    @Override
    public Proc launch(@Nonnull ProcStarter starter) throws IOException {
        return dockerExec(starter, true);
    }

    @Override
    public Channel launchChannel(@Nonnull String[] cmd,
                                 @Nonnull OutputStream out,
                                 @CheckForNull FilePath workDir,
                                 @Nonnull Map<String, String> envVars) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException {
        delegate.kill(modelEnvVars);
    }

    /**
     * Spin up all of the containers
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public DockerState launchContainers() throws IOException, InterruptedException {
        if (!buildWrapper.getSideDockerConfigurations().isEmpty()) {
            //There are side container, so create a network
            network = Optional.of(DockerNetwork.create(this));
        }
        //Launch side containers first
        for (SideDockerConfiguration side : buildWrapper.getSideDockerConfigurations()) {
            launchContainer(side, false);
        }

        //Launch main container
        DockerConfiguration main = buildWrapper.getDockerConfiguration();
        launchContainer(main, true);
        return new DockerState(debug, containerIds, network);
    }

    /**
     * Spin up the container mounting the specified path as a volume mount. This
     * method blocks until the container is started.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private ArgumentListBuilder getlaunchArgs(DockerConfiguration config,
                                              boolean isMain) throws IOException, InterruptedException {
        String workspacePath = workspace.getRemote();
        //Fully resolve the source workspace
        String workspaceSrc = Paths.get(workspacePath)
                .toAbsolutePath()
                .toString();

        config.setupImage(this, workspaceSrc);

        String tmpDest = node.getSystemProperties().get("java.io.tmpdir")
                .toString();
        Path tmpSrcPath = Paths.get(tmpDest);
        if (node instanceof Jenkins.MasterComputer
                && Files.exists(tmpSrcPath)) {
            //This is a workaround on macOS where /var is a link to /private/var
            // but the symbolic link is not passed into the docker VM
            tmpSrcPath = tmpSrcPath.toRealPath();
        }
        String tmpSrc = tmpSrcPath.toAbsolutePath()
                .toString();

        //TODO Set name? Maybe with build.toString().replaceAll("^\\w", "_")
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("run", "-t", "-d")
                .add("--workdir", workspacePath)
                //Add bridge network for internet access
                .add("--network", "bridge");
        //Add inter-container network if needed
        network.ifPresent(net -> net.addArgs(args));

        if (isMain) {
            String secondaryTempPath = WorkspaceList.tempDir(workspace).getRemote();
            String secondaryTempSrc = Paths.get(secondaryTempPath)
                    .toAbsolutePath()
                    .toString();
            //Start a shell to block the container, overriding the entrypoint in case the image already defines that
            args.add("--entrypoint", "/bin/sh")
                    .add("-v", workspaceSrc + ":" + workspacePath)
                    ////Jenkins puts scripts here
                    .add("-v", tmpSrc + ":" + tmpDest)
                    .add("-v", secondaryTempSrc+":"+secondaryTempPath);
        }
        config.addCreateArgs(this, args);
        return args;
    }

    private void launchContainer(DockerConfiguration config,
                                 boolean isMain) throws IOException, InterruptedException {
        ArgumentListBuilder args = getlaunchArgs(config, isMain);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int status = executeCommand(args)
                .stdout(baos)
                .stderr(listener.getLogger())
                .join();

        String containerId = baos.toString(StandardCharsets.UTF_8.name())
                .trim();

        if (status != 0) {
            throw new IOException("Failed to start docker image");
        }
        containerIds.add(containerId);
        if (isMain) {
            mainContainerId = containerId;
        }
        config.postCreate(this);
    }

    /**
     * Invoke <code>docker exec</code> on the already created container.
     *
     * @param starter
     * @param addRunArgs
     * @return
     * @throws IOException
     */
    public Proc dockerExec(Launcher.ProcStarter starter,
                           boolean addRunArgs) throws IOException {
        if (containerIds.isEmpty()) {
            throw new IllegalStateException(
                    "The container has not been launched. Call launcherContainer() first.");
        }
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("exec");
        if (starter.pwd() != null) {
            args.add("--workdir", starter.pwd().getRemote());
        }
        if (addRunArgs) {
            buildWrapper.getDockerConfiguration().addRunArgs(this, args);
        }

        args.add(mainContainerId);

        args.add("env").add(starter.envs());

        List<String> originalCmds = starter.cmds();
        boolean[] originalMask = starter.masks();
        for (int i = 0; i < originalCmds.size(); i++) {
            boolean masked = originalMask == null ? false : i < originalMask.length ? originalMask[i] : false;
            args.add(originalCmds.get(i), masked);
        }
        Launcher.ProcStarter procStarter = executeCommand(args);

        if (starter.stdout() != null) {
            procStarter.stdout(starter.stdout());
        } else {
            procStarter.stdout(listener.getLogger());
        }
        if (starter.stderr() != null) {
            procStarter.stderr(starter.stderr());
        } else {
            procStarter.stderr(listener.getLogger());
        }

        return procStarter.start();
    }

    /**
     * Remove the container
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void tearDown() throws IOException, InterruptedException {
        boolean exception = false;
        for (String containerId : containerIds) {
            ArgumentListBuilder args = new ArgumentListBuilder()
                    .add("rm", "-f", containerId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int status = executeCommand(args)
                    .stdout(out)
                    .stderr(this.listener.getLogger())
                    .join();

            if (status != 0) {
                listener.error("Failed to remove container %s", containerId);
            }
        }
        if (network.isPresent()) {
            network.get().tearDown(this);
        }
    }

    @Nonnull
    @Override
    public TaskListener getListener() {
        return listener;
    }

    public EnvVars getEnvironment() {
        return environment;
    }

    public Computer getNode() {
        return node;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public boolean isDebug() {
        return debug;
    }

    public Optional<DockerNetwork> getNetwork() {
        return network;
    }

    public List<String> getContainerIds() {
        return ImmutableList.copyOf(containerIds);
    }

    public String getMainContainerId() {
        return mainContainerId;
    }
}
