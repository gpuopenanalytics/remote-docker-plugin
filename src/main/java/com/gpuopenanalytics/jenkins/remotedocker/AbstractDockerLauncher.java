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
import hudson.Launcher;
import hudson.Proc;
import hudson.util.ArgumentListBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;

public abstract class AbstractDockerLauncher extends Launcher.DecoratedLauncher {

    private DockerState dockerState;

    protected AbstractDockerLauncher(@Nonnull Launcher launcher) {
        super(launcher);
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

    public abstract EnvVars getEnvironment();

    public abstract boolean isDebug();

    void configure(DockerState dockerState) {
        this.dockerState = dockerState;
    }

    protected DockerState getDockerState() {
        return dockerState;
    }
}
