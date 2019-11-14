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
import hudson.model.AbstractBuild;

import java.io.IOException;

/**
 * A Jenkins {@link Launcher} that delegates into a running docker container
 */
public class DockerLauncher extends AbstractDockerLauncher {

    private boolean debug;
    private RemoteDockerBuildWrapper buildWrapper;
    private AbstractBuild build;

    /**
     * @param debug
     * @param build
     * @param delegate     the launcher on the node executing the job
     * @param buildWrapper the {@link RemoteDockerBuildWrapper} currently
     *                     running
     */
    public DockerLauncher(boolean debug,
                          AbstractBuild build,
                          Launcher delegate,
                          RemoteDockerBuildWrapper buildWrapper) {
        super(delegate);
        this.debug = debug;
        this.build = build;
        this.buildWrapper = buildWrapper;
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
        return super.dockerExec(starter, addRunArgs,
                                buildWrapper.getWorkspaceOverride(),
                                buildWrapper.getDockerConfiguration());
    }

    @Override
    public EnvVars getEnvironment() {
        try {
            return build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

}
