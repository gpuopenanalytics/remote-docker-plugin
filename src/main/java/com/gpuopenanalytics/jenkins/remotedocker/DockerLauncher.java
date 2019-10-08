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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * A Jenkins {@link Launcher} that delegates into a running docker container
 */
public class DockerLauncher extends AbstractDockerLauncher {

    private boolean debug;
    private Launcher delegate;
    private TaskListener listener;
    private RemoteDockerBuildWrapper buildWrapper;
    private AbstractBuild build;

    /**
     * @param debug
     * @param build
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
     * Invoke <code>docker exec</code> on the already created container.
     *
     * @param starter
     * @param addRunArgs
     * @return
     * @throws IOException
     */
    public Proc dockerExec(Launcher.ProcStarter starter,
                           boolean addRunArgs) throws IOException {
        if (getDockerState().getContainerIds().isEmpty()) {
            throw new IllegalStateException("The container has not been launched.");
        }
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("exec");
        if (starter.pwd() != null) {
            args.add("--workdir", starter.pwd().getRemote());
        }
        if (addRunArgs) {
            buildWrapper.getDockerConfiguration().addRunArgs(this, args);
        }

        args.add(getDockerState().getMainContainerId());

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


    @Nonnull
    @Override
    public TaskListener getListener() {
        return listener;
    }

    public EnvVars getEnvironment() {
        try {
            return build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDebug() {
        return debug;
    }

}
