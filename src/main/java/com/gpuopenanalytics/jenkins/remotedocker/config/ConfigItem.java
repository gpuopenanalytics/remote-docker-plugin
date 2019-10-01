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

package com.gpuopenanalytics.jenkins.remotedocker.config;

import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents a configuration to the <code>docker create</code> command.
 */
public abstract class ConfigItem extends AbstractDescribableImpl<ConfigItem> implements ExtensionPoint, Serializable {

    /**
     * Validate the input. Throw a {@link Descriptor.FormException} for any
     * configuration errors.
     *
     * @throws Descriptor.FormException
     */
    public abstract void validate() throws Descriptor.FormException;

    /**
     * Add the arguments to <code>docker create</code>
     * @param launcher
     * @param args
     */
    public abstract void addCreateArgs(AbstractDockerLauncher launcher,
                                       ArgumentListBuilder args);

    /**
     * Runs after the container is running, but before the build executes
     *
     * @param launcher
     * @throws IOException
     * @throws InterruptedException
     */
    public void postCreate(AbstractDockerLauncher launcher) throws IOException, InterruptedException {
        //No-op, sub-classes should override
    }

    /**
     * Add the arguments to <code>docker exec</code> command that actually
     * executes the build
     * @param launcher
     * @param args
     */
    public void addRunArgs(AbstractDockerLauncher launcher,
                           ArgumentListBuilder args) {
        //No-op, sub-classes should override
    }

}
