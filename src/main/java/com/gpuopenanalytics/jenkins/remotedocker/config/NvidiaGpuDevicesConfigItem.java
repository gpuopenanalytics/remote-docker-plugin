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

import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Defines which GPU devices are visible in the container. Passes
 * <code>-e NVIDIA_VISIBLE_DEVICES=value</code>
 */
public class NvidiaGpuDevicesConfigItem extends CustomConfigItem {

    private static final String ENV_VAR_NAME = "NVIDIA_VISIBLE_DEVICES";

    @DataBoundConstructor
    public NvidiaGpuDevicesConfigItem(String nvidiaDevices,
                                      String nvidiaDevicesCustom) {
        super(nvidiaDevices, nvidiaDevicesCustom);
    }

    @Override
    public String getDefault() {
        return "all";
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (isCustom() && StringUtils.isEmpty(getValue())) {
            throw new Descriptor.FormException(
                    "Custom GPU Visibility cannot be empty",
                    "nvidiaDevicesCustom");
        } else if (StringUtils.isEmpty(getValue())) {
            throw new Descriptor.FormException(
                    " GPU Visibility cannot be empty",
                    "nvidiaDevices");
        }
    }

    @Override
    public void addCreateArgs(ArgumentListBuilder args, AbstractBuild build) {
        args.add("-e");
        args.addKeyValuePair("", ENV_VAR_NAME, getResolvedValue(build), false);
    }
}
