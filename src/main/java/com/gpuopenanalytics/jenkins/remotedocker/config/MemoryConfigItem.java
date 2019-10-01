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
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.regex.Pattern;

/**
 * Memory restriction passed as <code>-m memory</code>
 */
public class MemoryConfigItem extends ConfigItem {

    private static final Pattern VALIDATION_PATTERN = Pattern.compile(
            "(\\d+)([bkmgt])", Pattern.CASE_INSENSITIVE);


    private String memory;

    @DataBoundConstructor
    public MemoryConfigItem(String memory) {
        this.memory = memory;
    }

    public String getMemory() {
        return memory;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (!Utils.hasVariablesToResolve(memory)) {
            if (!VALIDATION_PATTERN.matcher(memory).matches()) {
                throw new Descriptor.FormException("Memory value is not valid",
                                                   "memory");
            }
            //TODO Minimum value
        }
    }

    @Override
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        args.add("-m", Utils.resolveVariables(launcher, memory).toUpperCase());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "Memory";
        }
    }
}
