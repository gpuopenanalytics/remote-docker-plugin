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
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
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
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        args.addKeyValuePair("", "--runtime", getResolvedValue(launcher), false);
    }

    public String getDockerRuntime(){
        return getRawValue();
    }

    public String getDockerRuntimeCustom(){
        return getRawCustomValue().orElse(null);
    }

    @Symbol("runtime")
    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "Docker runtime";
        }
    }
}
