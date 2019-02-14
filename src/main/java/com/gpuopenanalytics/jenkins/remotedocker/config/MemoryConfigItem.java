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

package com.gpuopenanalytics.jenkins.remotedocker.config;

import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import hudson.Extension;
import hudson.model.AbstractBuild;
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

    @Override
    public void validate() throws Descriptor.FormException {
        if (!VALIDATION_PATTERN.matcher(memory).matches()) {
            throw new Descriptor.FormException("Memory value is not valid",
                                               "memory");
        }
        //TODO Minimum value
    }

    @Override
    public void addArgs(ArgumentListBuilder args, AbstractBuild build) {
        args.add("-m", DockerConfiguration.resolveVariables(
                build.getBuildVariableResolver(), memory).toUpperCase());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "Memory";
        }
    }
}
