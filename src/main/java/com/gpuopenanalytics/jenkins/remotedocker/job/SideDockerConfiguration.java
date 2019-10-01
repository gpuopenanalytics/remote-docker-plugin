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

package com.gpuopenanalytics.jenkins.remotedocker.job;

import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class SideDockerConfiguration extends AbstractDescribableImpl<SideDockerConfiguration> implements DockerConfiguration {

    private String name;
    private AbstractDockerConfiguration dockerConfiguration;

    @DataBoundConstructor
    public SideDockerConfiguration(String name,
                                   AbstractDockerConfiguration dockerConfiguration) {
        this.name = name;
        this.dockerConfiguration = dockerConfiguration;
    }

    public String getName() {
        return name;
    }

    public AbstractDockerConfiguration getDockerConfiguration() {
        return dockerConfiguration;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (StringUtils.isEmpty(name)) {
            throw new Descriptor.FormException(
                    "Side container must have a name", "name");
        }
        dockerConfiguration.validate();
    }

    @Override
    public void setupImage(AbstractDockerLauncher launcher,
                           String localWorkspace) throws IOException, InterruptedException {
        //no-op
    }

    @Override
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        args.add("--name", Utils.resolveVariables(launcher, name));
        dockerConfiguration.addCreateArgs(launcher, args);
    }

    @Override
    public void postCreate(AbstractDockerLauncher launcher) throws IOException, InterruptedException {
        dockerConfiguration.postCreate(launcher);
    }


    @Override
    public void addRunArgs(AbstractDockerLauncher launcher,
                           ArgumentListBuilder args) {
        dockerConfiguration.addRunArgs(launcher, args);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SideDockerConfiguration> {

        @Override
        public String getDisplayName() {
            return "Side Container";
        }
    }
}
