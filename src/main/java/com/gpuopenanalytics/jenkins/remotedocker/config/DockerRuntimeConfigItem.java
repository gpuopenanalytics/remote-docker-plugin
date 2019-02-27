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

import com.gpuopenanalytics.jenkins.remotedocker.DockerLauncher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class DockerRuntimeConfigItem extends CustomConfigItem {

    @DataBoundConstructor
    public DockerRuntimeConfigItem(String dockerRuntime,
                                   String dockerRuntimeCustom) {
        super(dockerRuntime, dockerRuntimeCustom);
    }

    @Override
    public String getDefault() {
        return "runc";
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (isCustom() && StringUtils.isEmpty(getValue())) {
            throw new Descriptor.FormException(
                    "Custom docker runtime cannot be empty", "dockerRuntime");
        } else if (StringUtils.isEmpty(getValue())) {
            throw new Descriptor.FormException("Docker runtime cannot be empty",
                                               "dockerRuntime");
        }

    }

    @Override
    public void addCreateArgs(DockerLauncher launcher,
                              ArgumentListBuilder args,
                              AbstractBuild build) {
        args.addKeyValuePair("", "--runtime", getResolvedValue(build), false);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "Docker runtime";
        }
    }
}
