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

package com.gpuopenanalytics.jenkins.remotedocker;

import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfigurationDescriptor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collection;

/**
 * Main entrypoint for the plugin. This wraps a build, decorating the {@link
 * Launcher} to a {@link DockerLauncher} so that commands from the Jenkins
 * master are run into of the docker container.
 */
public class RemoteDockerBuildWrapper extends BuildWrapper {

    private DockerConfiguration dockerConfiguration;

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Build with Docker on Node";
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req,
                                        JSONObject formData) throws FormException {
            if (formData.isNullObject()) {
                return null;
            }
            RemoteDockerBuildWrapper wrapper = (RemoteDockerBuildWrapper) super.newInstance(
                    req, formData);
            wrapper.dockerConfiguration.validate();
            return wrapper;
        }

        public Collection<DockerConfigurationDescriptor> getDockerConfigurationItemDescriptors() {
            return DockerConfigurationDescriptor.all();
        }
    }

    /**
     * Simple wrapper to allow for tearDown
     */
    private class DockerEnvironment extends BuildWrapper.Environment {

        private DockerLauncher launcher;

        public DockerEnvironment(DockerLauncher launcher) {
            this.launcher = launcher;
        }

        @Override
        public boolean tearDown(AbstractBuild build,
                                BuildListener listener) throws IOException, InterruptedException {
            this.launcher.tearDown();
            return true;
        }
    }

    @DataBoundConstructor
    public RemoteDockerBuildWrapper(DockerConfiguration dockerConfiguration) {
        this.dockerConfiguration = dockerConfiguration;
    }

    public DockerConfiguration getDockerConfiguration() {
        return dockerConfiguration;
    }

    @Override
    public Environment setUp(AbstractBuild build,
                             Launcher launcher,
                             BuildListener listener) throws IOException, InterruptedException {
        String workspacePath = build.getWorkspace().getRemote();
        ((DockerLauncher) launcher).launchContainer(workspacePath);
        return new DockerEnvironment((DockerLauncher) launcher);
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build,
                                     Launcher launcher,
                                     BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return new DockerLauncher(build, launcher, listener, dockerConfiguration);
    }

}
