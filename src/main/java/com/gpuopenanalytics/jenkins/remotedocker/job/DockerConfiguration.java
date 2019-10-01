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

package com.gpuopenanalytics.jenkins.remotedocker.job;

import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.io.Serializable;

public interface DockerConfiguration extends Serializable {

    /**
     * Validate the correctness of the configuration.
     *
     * @throws Descriptor.FormException
     */
    void validate() throws Descriptor.FormException;

    /**
     * Called before the container is started. This allows for sub classes to
     * perform setup tasks such as building an image.
     *
     * @param launcher
     * @param localWorkspace
     *
     * @throws IOException
     * @throws InterruptedException
     */
    void setupImage(AbstractDockerLauncher launcher,
                    String localWorkspace) throws IOException, InterruptedException;

    /**
     * Add args to the <code>docker create</code>
     * @param launcher
     * @param args
     */
    void addCreateArgs(AbstractDockerLauncher launcher,
                       ArgumentListBuilder args);

    /**
     * Runs after the container is running, but before the build executes
     * @param launcher
     *
     */
    void postCreate(AbstractDockerLauncher launcher) throws IOException, InterruptedException;

    /**
     * Add the arguments to <code>docker exec</code> command that actually
     * executes the build
     *
     * @param launcher
     * @param args
     */
    void addRunArgs(AbstractDockerLauncher launcher,
                    ArgumentListBuilder args);
}
