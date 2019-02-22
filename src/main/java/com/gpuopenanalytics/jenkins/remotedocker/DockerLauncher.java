/*
 * Copyright (c) 2019, NVIDIA CORPORATION.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gpuopenanalytics.jenkins.remotedocker;

import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.util.ArgumentListBuilder;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * A Jenkins {@link Launcher} that delegates into a running docker container
 */
public class DockerLauncher extends Launcher {

    private Launcher delegate;
    private TaskListener listener;
    private DockerConfiguration dockerConfiguration;
    private AbstractBuild build;

    private String containerId;

    /**
     * @param build               the specific build job that this container is
     *                            running for
     * @param delegate            the launcher on the node executing the job
     * @param listener            listener for logging
     * @param dockerConfiguration configuration of the container to start up
     */
    public DockerLauncher(AbstractBuild build,
                          Launcher delegate,
                          TaskListener listener,
                          DockerConfiguration dockerConfiguration) {
        super(delegate);
        this.build = build;
        this.delegate = delegate;
        this.listener = listener;
        this.dockerConfiguration = dockerConfiguration;
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
     * Spin up the container mounting the specified path as a volume mount. This
     * method blocks until the container is started.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void launchContainer() throws IOException, InterruptedException {
        EnvVars environment = build.getEnvironment(listener);
        String workspacePath = build.getWorkspace().getRemote();
        //Fully resolve the source workspace
        String workspaceSrc = Paths.get(workspacePath)
                .toRealPath()
                .toAbsolutePath()
                .toString();

        dockerConfiguration.setupImage(this, workspaceSrc);

        //This is a workaround on macOS where /var is a link to /private/var
        // but the symbolic link is not passed into the docker VM
        String tmpDest = System.getProperty("java.io.tmpdir");
        String tmpSrc = Paths.get(tmpDest)
                .toRealPath()
                .toAbsolutePath()
                .toString();

        Launcher launcher = delegate;
        //TODO Set name? Maybe with build.toString().replaceAll("^\\w", "_")
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("run", "-t", "-d")
                .add("--env", "TMPDIR=" + workspacePath + ".tmp")
                .add("--workdir", workspacePath)
                .add("-v", workspaceSrc + ":" + workspacePath)
                .add("-v", tmpSrc + ":" + tmpDest) //Jenkins puts scripts here
                //Start a shell to block the container, overriding the entrypoint in case the image already defines that
                .add("--entrypoint", "/bin/sh");

        dockerConfiguration.addCreateArgs(args, build);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int status = executeCommand(args)
                .stdout(out)
                .stderr(launcher.getListener().getLogger())
                .join();

        final String containerId = out.toString(StandardCharsets.UTF_8.name())
                .trim();

        if (status != 0) {
            throw new IOException("Failed to start docker image");
        }
        this.containerId = containerId;

        dockerConfiguration.postCreate(this, build);
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
                .quiet(false);
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
        if (containerId == null) {
            throw new IllegalStateException(
                    "The container has not been launched. Call launcherContainer() first.");
        }
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("exec");
        if (starter.pwd() != null) {
            args.add("--workdir", starter.pwd().getRemote());
        }
        if (addRunArgs) {
            dockerConfiguration.addRunArgs(args, build);
        }

        args.add(containerId);

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
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("rm", "-f", containerId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int status = executeCommand(args)
                .stdout(out)
                .stderr(this.listener.getLogger())
                .join();

        if (status != 0) {
            throw new IOException("Failed to remove container " + containerId);
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
}
