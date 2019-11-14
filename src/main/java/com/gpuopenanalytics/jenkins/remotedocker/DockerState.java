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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Tracks what containers
 */
public class DockerState implements Serializable {

    private boolean debug;
    private String mainContainerId;
    private ImmutableList<String> containerIds;
    private String networkId;
    private boolean removeContainers;

    public DockerState(boolean debug,
                       String mainContainerId,
                       Collection<String> containerIds,
                       Optional<DockerNetwork> network,
                       boolean removeContainers) {
        this.debug = debug;
        this.mainContainerId = mainContainerId;
        this.containerIds = ImmutableList.copyOf(containerIds);
        this.networkId = network.map(DockerNetwork::getId).orElse(null);
        this.removeContainers = removeContainers;
    }

    private int execute(Launcher launcher,
                        ArgumentListBuilder args) throws IOException, InterruptedException {
        TaskListener listener = launcher.getListener();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int status = launcher.launch()
                .cmds(args)
                .quiet(!debug)
                .stdout(out)
                .stderr(listener.getLogger())
                .join();
        return status;
    }

    public void tearDown(Launcher launcher) throws IOException, InterruptedException {
        if (removeContainers) {
            TaskListener listener = launcher.getListener();
            for (String containerId : containerIds) {
                ArgumentListBuilder args = new ArgumentListBuilder()
                        .add("docker", "rm", "-f", containerId);
                int status = execute(launcher, args);
                if (status != 0) {
                    listener.error("Failed to remove container %s",
                                   containerId);
                }
            }
            if (networkId != null) {
                ArgumentListBuilder args = new ArgumentListBuilder()
                        .add("docker", "network", "rm", networkId);
                int status = execute(launcher, args);
                if (status != 0) {
                    listener.error("Failed to remove network %s", networkId);
                }
            }
        }
    }

    /**
     * Spin up all of the containers
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static DockerState launchContainers(RemoteDockerBuildWrapper buildWrapper,
                                               AbstractDockerLauncher launcher,
                                               FilePath workspace) throws IOException, InterruptedException {
        Optional<DockerNetwork> network = Optional.empty();
        if (!buildWrapper.getSideDockerConfigurations().isEmpty()) {
            //There are side container, so create a network
            network = Optional.of(DockerNetwork.create(launcher));
        }
        List<String> containerIds = new ArrayList<>();
        //Launch side containers first
        for (SideDockerConfiguration side : buildWrapper.getSideDockerConfigurations()) {
            String id = launchContainer(buildWrapper, side, false, launcher, workspace,
                                        network);
            containerIds.add(id);
        }

        //Launch main container
        DockerConfiguration main = buildWrapper.getDockerConfiguration();
        String mainId = launchContainer(buildWrapper, main, true, launcher, workspace,
                                        network);
        containerIds.add(mainId);
        Collections.reverse(containerIds);

        DockerState dockerState = new DockerState(buildWrapper.isDebug(),
                                                  mainId, containerIds,
                                                  network,
                                                  buildWrapper.isRemoveContainers());
        launcher.configure(dockerState);
        return dockerState;
    }

    /**
     * Spin up the container mounting the specified path as a volume mount. This
     * method blocks until the container is started.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private static ArgumentListBuilder getlaunchArgs(RemoteDockerBuildWrapper buildWrapper,
                                                     DockerConfiguration config,
                                                     boolean isMain,
                                                     AbstractDockerLauncher launcher,
                                                     FilePath workspace,
                                                     Optional<DockerNetwork> network) throws IOException, InterruptedException {
        String workspacePath = workspace.getRemote();
        String workspaceTarget = Optional.ofNullable(
                                buildWrapper.getWorkspaceOverride())
                                .orElse(workspacePath);
        //Fully resolve the source workspace
        String workspaceSrc = Paths.get(workspacePath)
                .toAbsolutePath()
                .toString();

        config.setupImage(launcher, workspaceSrc);
        Computer node = workspace.toComputer();
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
                .add("--name", Utils.resolveVariables(launcher, "$BUILD_TAG"))
                //Add bridge network for internet access
                .add("--network", "bridge");
        //Add inter-container network if needed
        network.ifPresent(net -> net.addArgs(args));

        if (isMain) {
            String secondaryTempPath = WorkspaceList.tempDir(workspace)
                    .getRemote();
            String secondaryTempSrc = Paths.get(secondaryTempPath)
                    .toAbsolutePath()
                    .toString();
            //Start a shell to block the container, overriding the entrypoint in case the image already defines that
            args.add("--entrypoint", "/bin/sh")
                    .add("--workdir", workspaceTarget)
                    .add("-v", workspaceSrc + ":" + workspaceTarget)
                    ////Jenkins puts scripts here
                    .add("-v", tmpSrc + ":" + tmpDest)
                    .add("-v", secondaryTempSrc + ":" + secondaryTempPath);
        }
        config.addCreateArgs(launcher, args);
        return args;
    }

    private static String launchContainer(RemoteDockerBuildWrapper buildWrapper,
                                          DockerConfiguration config,
                                          boolean isMain,
                                          AbstractDockerLauncher launcher,
                                          FilePath workspace,
                                          Optional<DockerNetwork> network) throws IOException, InterruptedException {
        ArgumentListBuilder args = getlaunchArgs(buildWrapper, config, isMain, launcher,
                                                 workspace, network);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int status = launcher.executeCommand(args)
                .stdout(baos)
                .stderr(launcher.getListener().getLogger())
                .join();

        String containerId = baos.toString(StandardCharsets.UTF_8.name())
                .trim();

        if (status != 0) {
            throw new IOException("Failed to start docker image");
        }

        DockerState tempState = new DockerState(launcher.isDebug(),
                                                containerId,
                                                ImmutableList.of(containerId),
                                                Optional.empty(),
                                                false);
        launcher.configure(tempState);
        config.postCreate(launcher);
        return containerId;
    }

    public String getMainContainerId() {
        return mainContainerId;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * Gets all of the container IDs both main and side containers
     *
     * @return
     */
    public ImmutableList<String> getContainerIds() {
        return containerIds;
    }

    public Optional<String> getNetworkId() {
        return Optional.ofNullable(networkId);
    }

}
