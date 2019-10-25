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

package com.gpuopenanalytics.jenkins.remotedocker;

import com.gpuopenanalytics.jenkins.remotedocker.job.AbstractDockerConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.job.AbstractDockerConfigurationDescriptor;
import com.gpuopenanalytics.jenkins.remotedocker.job.DockerImageConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.job.SideDockerConfiguration;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Main entrypoint for the plugin. This wraps a build, decorating the {@link
 * Launcher} to a {@link DockerLauncher} so that commands from the Jenkins
 * master are run into of the docker container.
 */
public class RemoteDockerBuildWrapper extends BuildWrapper {

    private boolean debug;
    private Boolean removeContainers = true;
    private AbstractDockerConfiguration dockerConfiguration;
    private List<SideDockerConfiguration> sideDockerConfigurations;

    @DataBoundConstructor
    public RemoteDockerBuildWrapper(boolean debug,
                                    AbstractDockerConfiguration dockerConfiguration,
                                    List<SideDockerConfiguration> sideDockerConfigurations) {
        this.debug = debug;
        this.dockerConfiguration = dockerConfiguration;
        this.sideDockerConfigurations = Optional.ofNullable(
                sideDockerConfigurations)
                .orElse(Collections.emptyList());
    }

    public boolean isDebug() {
        return debug;
    }

    @DataBoundSetter
    public void setRemoveContainers(Boolean removeContainers) {
        this.removeContainers = removeContainers;
    }

    public Boolean isRemoveContainers() {
        return removeContainers != null ? removeContainers : true;
    }

    public AbstractDockerConfiguration getDockerConfiguration() {
        return dockerConfiguration;
    }

    public List<SideDockerConfiguration> getSideDockerConfigurations() {
        return sideDockerConfigurations;
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build,
                                     Launcher launcher,
                                     BuildListener listener) throws Run.RunnerAbortedException {
        return new DockerLauncher(debug, build, launcher, listener, this);
    }

    @Override
    public Environment setUp(AbstractBuild build,
                             Launcher launcher,
                             BuildListener listener) throws IOException, InterruptedException {
        build.addAction(new DockerAction());
        try {
            ((DockerLauncher) launcher).launchContainers();
            return new DockerEnvironment((DockerLauncher) launcher, removeContainers);
        } catch (IOException | InterruptedException e) {
            //Attempt tearDown in case we partially started some containers
            ((DockerLauncher) launcher).tearDown(true);
            throw e;
        }
    }

    /**
     * Simple wrapper to allow for tearDown
     */
    private class DockerEnvironment extends BuildWrapper.Environment {

        private DockerLauncher launcher;
        private boolean removeContainers;

        public DockerEnvironment(DockerLauncher launcher, boolean removeContainers) {
            this.launcher = launcher;
            this.removeContainers=removeContainers;
        }

        @Override
        public boolean tearDown(AbstractBuild build,
                                BuildListener listener) throws IOException, InterruptedException {
            this.launcher.tearDown(removeContainers);
            return true;
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Run build inside a Docker container";
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
            for (SideDockerConfiguration side : wrapper.sideDockerConfigurations) {
                side.validate();
            }
            return wrapper;
        }

        public Collection<AbstractDockerConfigurationDescriptor> getDockerConfigurationItemDescriptors() {
            return AbstractDockerConfigurationDescriptor.all();
        }

        public Descriptor getDefaultDockerConfigurationDescriptor() {
            return Jenkins.get().getDescriptorOrDie(
                    DockerImageConfiguration.class);
        }
    }

}
