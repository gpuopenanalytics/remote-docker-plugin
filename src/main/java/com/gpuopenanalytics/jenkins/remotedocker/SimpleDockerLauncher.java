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

import javax.annotation.Nonnull;
import java.io.IOException;

public class SimpleDockerLauncher extends AbstractDockerLauncher {

    private boolean debug;
    private EnvVars environment;

    public SimpleDockerLauncher(@Nonnull Launcher launcher,
                                boolean debug,
                                EnvVars environment) {
        super(launcher);
        this.debug = debug;
        this.environment = environment;
    }

    @Override
    public Proc dockerExec(ProcStarter starter,
                           boolean addRunArgs) throws IOException {
        throw new UnsupportedOperationException(
                "SimpleDockerLauncher cannot execute commands");
    }

    @Override
    public EnvVars getEnvironment() {
        return environment;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }
}
