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

import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
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

    private String getDockerArgument(AbstractBuild build) {
        String readType = readOnly ? READ_ONLY_FLAG : READ_WRITE_FLAG;
        return String.join(":", Utils.resolveVariables(build, hostPath),
                           Utils.resolveVariables(build, destPath),
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

    public void addArgs(ArgumentListBuilder args, AbstractBuild build) {
        args.add("-v", getDockerArgument(build));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<VolumeConfiguration> {

        @Override
        public String getDisplayName() {
            return "Add Docker Volume";
        }
    }

}
