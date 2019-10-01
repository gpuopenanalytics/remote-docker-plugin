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

import com.google.common.collect.Lists;
import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

/**
 * Defines which driver capabilities are passed into the container. Passes
 * <code>-e NVIDIA_VISIBLE_DEVICES=value</code>
 */
public class NvidiaDriverAbilityConfigItem extends ConfigItem {

    private static final String ENV_VAR_NAME = "NVIDIA_DRIVER_CAPABILITIES";

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
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
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
            abilities.add("utility");
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
            return "NVIDIA Driver Capabilities";
        }
    }

}
