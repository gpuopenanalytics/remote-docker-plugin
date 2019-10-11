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

package com.gpuopenanalytics.jenkins.remotedocker.pipeline;

import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.DockerState;
import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.LauncherDecorator;
import hudson.Proc;
import hudson.model.Node;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;

/**
 * Decorates a {@link Launcher} into an {@link AbstractDockerLauncher}
 */
public class DockerLauncherDecorator extends LauncherDecorator implements Serializable {

    private boolean debug;
    private DockerState dockerState;
    private DockerConfiguration dockerConfiguration;
    private EnvVars environment;

    public DockerLauncherDecorator(boolean debug,
                                   DockerState dockerState,
                                   DockerConfiguration dockerConfiguration,
                                   EnvVars environment) {
        this.debug = debug;
        this.dockerState = dockerState;
        this.dockerConfiguration = dockerConfiguration;
        this.environment = environment;
    }

    @Nonnull
    @Override
    public Launcher decorate(@Nonnull Launcher launcher, @Nonnull Node node) {
        return new AbstractDockerLauncher(launcher, dockerState) {

            @Override
            public Proc dockerExec(Launcher.ProcStarter starter,
                                   boolean addRunArgs) throws IOException {
                return super.dockerExec(starter, addRunArgs,
                                        dockerConfiguration);
            }

            @Override
            public EnvVars getEnvironment() {
                return environment;
            }

            @Override
            public boolean isDebug() {
                return debug;
            }
        };
    }


}
