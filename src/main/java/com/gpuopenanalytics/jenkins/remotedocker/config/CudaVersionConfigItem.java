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

import com.google.common.collect.ImmutableList;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.stream.Collectors;

public class CudaVersionConfigItem extends ConfigItem {

    private static final String ENV_VAR_NAME = "NVIDIA_REQUIRE_CUDA";

    private static final List<String> CUDA_VERSIONS = ImmutableList.of("6.5",
                                                                       "7.0",
                                                                       "7.5",
                                                                       "8.0",
                                                                       "9.0",
                                                                       "9.1",
                                                                       "9.2",
                                                                       "10.0");
    private static ListBoxModel CUDA_OPTIONS = new ListBoxModel(
            CUDA_VERSIONS.stream()
                    .map(ListBoxModel.Option::new)
                    .collect(Collectors.toList()));

    private String nvidiaCuda;

    @DataBoundConstructor
    public CudaVersionConfigItem(String nvidiaCuda) {
        this.nvidiaCuda = nvidiaCuda;
    }

    public String getNvidiaCuda() {
        return nvidiaCuda;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (!Utils.hasVariablesToResolve(nvidiaCuda)
                && !CUDA_VERSIONS.contains(nvidiaCuda)) {
            throw new Descriptor.FormException(
                    "Invalid CUDA version: " + nvidiaCuda, "nvidiaCuda");
        }
    }

    @Override
    public void addCreateArgs(ArgumentListBuilder args, AbstractBuild build) {
        args.add("-e");
        String cuda = Utils.resolveVariables(build, nvidiaCuda);
        args.addKeyValuePair("", ENV_VAR_NAME, cuda, false);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "NVIDIA Minimum CUDA Version";
        }

        public ListBoxModel doFillNvidiaCudaItems() {
            return CUDA_OPTIONS;
        }
    }
}
