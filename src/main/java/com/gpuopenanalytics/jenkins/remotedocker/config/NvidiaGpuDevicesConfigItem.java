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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String nvidiasmiOutput = executeWithOutput(launcher.getInner(), "nvidia-smi", "-L"); 
            if (isMIG(nvidiasmiOutput)) {
                value = getMIG(nvidiasmiOutput, executorNum);
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

    private boolean isMIG(String output) {
        Pattern pattern = Pattern.compile("(MIG-GPU-[a-f0-9\-\/]+)");
        Matcher m = pattern.matcher(output);

        if (m.find()) {
            return true;
        }
        return false;
    }

    private String getMIG(String output, String executor) {
        List<String> uuids = new ArrayList<String>();
        Pattern pattern = Pattern.compile("(MIG-GPU-[a-f0-9\-\/]+)");
        Matcher m = pattern.matcher(output);

        while (m.find()) {
            uuids.add(m.group());
        }
        return uuids[executor];
    }
}
