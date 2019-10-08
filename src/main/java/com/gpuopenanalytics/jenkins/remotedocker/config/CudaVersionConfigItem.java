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

import com.google.common.collect.ImmutableList;
import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
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
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        args.add("-e");
        String cuda = Utils.resolveVariables(launcher, nvidiaCuda);
        args.addKeyValuePair("", ENV_VAR_NAME, "cuda>="+cuda, false);
    }

    @Symbol("cudaVersion")
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
