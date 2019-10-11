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
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.util.ArgumentListBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * Root class for {@link Launcher}s that delegate commands into a docker
 * container
 */
public abstract class AbstractDockerLauncher extends Launcher.DecoratedLauncher {

    private DockerState dockerState;

    protected AbstractDockerLauncher(@Nonnull Launcher launcher) {
        super(launcher);
    }

    protected AbstractDockerLauncher(@Nonnull Launcher launcher,
                                     @Nonnull DockerState dockerState) {
        this(launcher);
        configure(dockerState);
    }

    @Override
    public Proc launch(ProcStarter starter) throws IOException {
        return dockerExec(starter, true);
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
    public abstract Proc dockerExec(Launcher.ProcStarter starter,
                                    boolean addRunArgs) throws IOException;

    /**
     * Invoke <code>docker exec</code> on the already created container.
     *
     * @param starter
     * @param addRunArgs
     * @param dockerConfiguration
     * @return
     */
    protected Proc dockerExec(Launcher.ProcStarter starter,
                              boolean addRunArgs,
                              DockerConfiguration dockerConfiguration) throws IOException {
        if (dockerState == null || dockerState.getMainContainerId() == null) {
            throw new IllegalStateException("Container is not started.");
        }
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("exec");
        if (starter.pwd() != null) {
            args.add("--workdir", starter.pwd().getRemote());
        }
        if (addRunArgs) {
            dockerConfiguration.addRunArgs(this, args);
        }

        args.add(dockerState.getMainContainerId());

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
     * Execute a <code>docker</code> command such as build or pull.
     * <p>For exec, use {@link #dockerExec(ArgumentListBuilder, boolean)} or
     * {@link #dockerExec(ProcStarter, boolean)}
     *
     * @param args
     * @return
     */
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
        return getInner().launch()
                //TODO I think we should pass something here
                //.envs()
                .cmds(args)
                .quiet(!isDebug());
    }

    /**
     * Get the environment associated with this Launcher
     *
     * @return
     */
    public abstract EnvVars getEnvironment();

    /**
     * Whether the launcher should print debug information
     *
     * @return
     */
    public abstract boolean isDebug();

    /**
     * Make this Launcher aware of a set up {@link DockerState}
     *
     * @param dockerState
     */
    void configure(DockerState dockerState) {
        this.dockerState = dockerState;
    }

    /**
     * Get the, possibly null, {@link DockerState} of this Launcher
     *
     * @return
     */
    protected DockerState getDockerState() {
        return dockerState;
    }
}
