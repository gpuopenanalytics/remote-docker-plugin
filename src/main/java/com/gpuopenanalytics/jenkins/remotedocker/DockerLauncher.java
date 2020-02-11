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

import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.job.SideDockerConfiguration;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Jenkins {@link Launcher} that delegates into a running docker container
 */
public class DockerLauncher extends Launcher {

    private boolean debug;
    private Launcher delegate;
    private TaskListener listener;
    private RemoteDockerBuildWrapper buildWrapper;
    private AbstractBuild build;
    private DockerVersion version;

    private transient Optional<DockerNetwork> network = Optional.empty();
    private transient List<String> containerIds = new ArrayList<>();
    private transient String mainContainerId;

    /**
     * @param build        the specific build job that this container is
     *                     running for
     * @param delegate     the launcher on the node executing the job
     * @param listener     listener for logging
     * @param buildWrapper the {@link RemoteDockerBuildWrapper} currently
     *                     running
     */
    public DockerLauncher(boolean debug,
                          AbstractBuild build,
                          Launcher delegate,
                          TaskListener listener,
                          RemoteDockerBuildWrapper buildWrapper) {
        super(delegate);
        this.debug = debug;
        this.build = build;
        this.delegate = delegate;
        this.listener = listener;
        this.buildWrapper = buildWrapper;
        try {
            this.version = parseVersion();
            getListener().getLogger().println(this.version);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
    public void launchContainers() throws IOException, InterruptedException {
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
        String workspacePath = build.getWorkspace().getRemote();
        String workspaceTarget = Optional.ofNullable(
                buildWrapper.getWorkspaceOverride())
                .orElse(workspacePath);
        //Fully resolve the source workspace
        String workspaceSrc = Paths.get(workspacePath)
                .toAbsolutePath()
                .toString();

        config.setupImage(this, workspaceSrc);

        Computer computer = build.getWorkspace().toComputer();
        String tmpDest = computer.getSystemProperties().get("java.io.tmpdir")
                .toString();
        Path tmpSrcPath = Paths.get(tmpDest);
        if (computer instanceof Jenkins.MasterComputer
                && Files.exists(tmpSrcPath)) {
            //This is a workaround on macOS where /var is a link to /private/var
            // but the symbolic link is not passed into the docker VM
            tmpSrcPath = tmpSrcPath.toRealPath();
        }
        String tmpSrc = tmpSrcPath.toAbsolutePath()
                .toString();
        
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("run", "-t", "-d")
                //Add bridge network for internet access
                .add("--network", "bridge");
        //Add inter-container network if needed
        network.ifPresent(net -> net.addArgs(args));

        if (isMain) {
            //Start a shell to block the container, overriding the entrypoint in case the image already defines that
            args.add("--entrypoint", "/bin/sh")
                    .add("--workdir", workspaceTarget)
                    .add("-v", workspaceSrc + ":" + workspaceTarget)
                    //Jenkins puts scripts here
                    .add("-v", tmpSrc + ":" + tmpDest);
        }
        config.addCreateArgs(this, args, build);
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
        config.postCreate(this, build);
    }

    /**
     * Execute a docker command
     *
     * @param args
     * @return
     */
    public Launcher.ProcStarter executeCommand(ArgumentListBuilder args) {
        if (args.toList().isEmpty()) {
            throw new IllegalArgumentException("No args given");
        }
        if (!"docker".equals(args.toList().get(0))) {
            args.prepend("docker");
        }
        return delegate.launch()
                //TODO I think we should pass something here
                //.envs()
                .cmds(args)
                .quiet(!debug);
    }

    /**
     * Invoke <code>docker exec</code> on the already created container.
     *
     * @param args
     * @param addRunArgs
     * @return
     * @throws IOException
     */
    public Proc dockerExec(ArgumentListBuilder args,
                           boolean addRunArgs) throws IOException {
        Launcher.ProcStarter starter = this.new ProcStarter();
        starter.stderr(listener.getLogger());
        starter.cmds(args);
        return dockerExec(starter, addRunArgs);
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
            String path = Optional.ofNullable(
                    buildWrapper.getWorkspaceOverride())
                    .orElse(starter.pwd().getRemote());
            args.add("--workdir", path);
        }
        if (addRunArgs) {
            buildWrapper.getDockerConfiguration().addRunArgs(this, args, build);
        }

        args.add(mainContainerId);

        args.add("env").add(starter.envs());
        if (buildWrapper.getWorkspaceOverride() != null) {
            //Override $WORKSPACE inside the container
            args.add("WORKSPACE=" + buildWrapper.getWorkspaceOverride());
        }

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
    public void tearDown(boolean removeContainers) throws IOException, InterruptedException {
        for (String containerId : containerIds) {
            ArgumentListBuilder args = new ArgumentListBuilder();
            if (removeContainers) {
                args.add("rm", "-f", containerId);
            } else {
                args.add("stop", containerId);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int status = executeCommand(args)
                    .stdout(out)
                    .stderr(this.listener.getLogger())
                    .join();

            if (status != 0) {
                listener.error("Failed to %s container %s",
                               removeContainers ? "remove" : "stop",
                               containerId);
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

    public AbstractBuild getBuild() {
        return build;
    }

    public boolean isDebug() {
        return debug;
    }

    public DockerVersion getVersion() {
        return version;
    }

    private DockerVersion parseVersion() throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder("docker",
                                                           "--version");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int status = executeCommand(args)
                .stdout(baos)
                .stderr(getListener().getLogger())
                .join();

        if (status != 0) {
            throw new IOException("Could not get docker version");
        }
        String versionString = baos.toString(StandardCharsets.UTF_8.name())
                .trim();
        try {
            return DockerVersion.fromVersionString(versionString);
        } catch (DockerVersion.VersionParseException e) {
            getListener().getLogger().println(
                    "WARN - Could not parse docker version");
            e.printStackTrace(getListener().getLogger());
            return DockerVersion.DEFAULT;
        }
    }
}
