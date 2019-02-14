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

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * Defines which driver capabilities are passed into the container. Passes
 * <code>-e NVIDIA_VISIBLE_DEVICES=value</code>
 */
public class NvidiaDriverAbilityConfigItem extends ConfigItem {

    private static final String ENV_VAR_NAME = "NVIDIA_VISIBLE_DEVICES";

    private boolean compute;
    private boolean compat32;
    private boolean graphics;
    private boolean utility;
    private boolean video;

    @DataBoundConstructor
    public NvidiaDriverAbilityConfigItem(boolean compute,
                                         boolean compat32,
                                         boolean graphics,
                                         boolean utility,
                                         boolean video) {
        this.compute = compute;
        this.compat32 = compat32;
        this.graphics = graphics;
        this.utility = utility;
        this.video = video;
    }

    public boolean isCompute() {
        return compute;
    }

    public boolean isCompat32() {
        return compat32;
    }

    public boolean isGraphics() {
        return graphics;
    }

    public boolean isUtility() {
        return utility;
    }

    public boolean isVideo() {
        return video;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        //No-op
    }

    @Override
    public void addArgs(ArgumentListBuilder args, AbstractBuild build) {
        List<String> abilities = Lists.newArrayList();
        if (compute) {
            abilities.add("compute");
        }
        if (compat32) {
            abilities.add("compat32");
        }
        if (graphics) {
            abilities.add("graphics");
        }
        if (utility) {
            abilities.add("compute");
        }
        if (video) {
            abilities.add("video");
        }

        String value = String.join(",", abilities);
        args.add("-e");
        args.addKeyValuePair("", ENV_VAR_NAME, value, false);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "NVIDIA Driver Settings";
        }
    }

}
