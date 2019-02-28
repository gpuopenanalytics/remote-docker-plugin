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

package com.gpuopenanalytics.jenkins.remotedocker.job;

import com.gpuopenanalytics.jenkins.remotedocker.DockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.AbstractBuild;
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
    public void setupImage(DockerLauncher launcher,
                           String localWorkspace) throws IOException, InterruptedException {
        //no-op
    }

    @Override
    public void addCreateArgs(DockerLauncher launcher,
                              ArgumentListBuilder args,
                              AbstractBuild build) {
        args.add("--name", Utils.resolveVariables(build, name));
        dockerConfiguration.addCreateArgs(launcher, args, build);
    }

    @Override
    public void postCreate(DockerLauncher launcher,
                           AbstractBuild build) throws IOException, InterruptedException {
        dockerConfiguration.postCreate(launcher, build);
    }


    @Override
    public void addRunArgs(DockerLauncher launcher,
                           ArgumentListBuilder args,
                           AbstractBuild build) {
        dockerConfiguration.addRunArgs(launcher, args, build);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SideDockerConfiguration> {

        @Override
        public String getDisplayName() {
            return "Side Container";
        }
    }
}
