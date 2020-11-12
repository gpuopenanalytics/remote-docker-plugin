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
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        String value;
        if ("executor".equals(getValue())) {
            String executorNum = launcher.getEnvironment().get("EXECUTOR_NUMBER");
            if (isMIG(launcher)) {
                value = getMIG(launcher, executorNum);
            } else {
                value = executorNum;
            }
        } else {
            value = getResolvedValue(launcher);
        }

        if (launcher.getVersion().hasGpuFlag()) {
            args.add("--gpus", "device=" + value);
        } else {
            args.add("-e");
            args.addKeyValuePair("", ENV_VAR_NAME, value, false);
        }
    }

    public String getNvidiaDevices() {
        return getRawValue();
    }

    public String getNvidiaDevicesCustom() {
        return getRawCustomValue().orElse(null);
    }

    @Symbol("gpus")
    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "NVIDIA Device Visibility";
        }
    }

    private String executeWithOutput(Launcher launcher, String... args) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int status = launcher.launch()
                    .cmds(args)
                    .stdout(baos)
                    .stderr(launcher.getListener().getLogger())
                    .join();
            if (status != 0) {
                throw new RuntimeException(
                        "Non-zero status " + status + ": " + Arrays
                                .toString(args));
            }
            return baos.toString(StandardCharsets.UTF_8.name()).trim();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMIG(AbstractDockerLauncher launcher) {
        String uuids = executeWithOutput(launcher.getInner(), "/bin/bash", "-c", "nvidia-smi -L | grep -i MIG");
        if (uuids != "") {
            return true;
        }
        return false;
    }

    private String getMIG(AbstractDockerLauncher launcher, String executor) {
        // Executor 0 will be line 1, Executor 1 is line 2, etc
        executor = String.valueOf((Integer.parseInt(executor)+1));
        String command = "nvidia-smi -L | grep -i MIG | sed -n " + executor +
                         "p | awk '{print $6}' | tr -d \\)";
        String uuid = executeWithOutput(launcher.getInner(), "/bin/bash", "-c", command);
        return uuid;
    }
}
