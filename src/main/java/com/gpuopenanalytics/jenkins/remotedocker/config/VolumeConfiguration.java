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

import com.gpuopenanalytics.jenkins.remotedocker.DockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Hint that allows for custom volumes to mount for Docker container.
 * Needed for testing large datasets, and to save results.
 */
public class VolumeConfiguration extends AbstractDescribableImpl<VolumeConfiguration> implements ExtensionPoint {

    private static final String READ_ONLY_FLAG = "ro";
    private static final String READ_WRITE_FLAG = "rw";

    private final String hostPath;
    private final String destPath;
    private final boolean readOnly;


    @DataBoundConstructor
    public VolumeConfiguration(String hostPath,
                               String destPath,
                               boolean readOnly) {
        this.hostPath = hostPath;
        this.destPath = destPath;
        this.readOnly = readOnly;
    }

    public String getHostPath() {
        return hostPath;
    }

    public String getDestPath() {
        return destPath;
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    private String getDockerArgument(DockerLauncher launcher) {
        String readType = readOnly ? READ_ONLY_FLAG : READ_WRITE_FLAG;
        return String.join(":", Utils.resolveVariables(launcher, hostPath),
                           Utils.resolveVariables(launcher, destPath),
                           readType);
    }

    public void validate() throws Descriptor.FormException {
        if (StringUtils.isEmpty(hostPath)) {
            throw new Descriptor.FormException("Must specify a host path",
                                               "hostPath");
        }
        if (StringUtils.isEmpty(destPath)) {
            throw new Descriptor.FormException(
                    "Must specify a destination path",
                    "destPath");
        }
    }

    public void addArgs(ArgumentListBuilder args, DockerLauncher launcher) {
        args.add("-v", getDockerArgument(launcher));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<VolumeConfiguration> {

        @Override
        public String getDisplayName() {
            return "Add Docker Volume";
        }
    }

}
