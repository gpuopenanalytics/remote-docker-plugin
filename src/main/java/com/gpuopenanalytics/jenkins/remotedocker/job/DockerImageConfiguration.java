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
import com.gpuopenanalytics.jenkins.remotedocker.config.ConfigItem;
import com.gpuopenanalytics.jenkins.remotedocker.config.VolumeConfiguration;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * A {@link AbstractDockerConfiguration} created from an existing docker image.
 */
public class DockerImageConfiguration extends AbstractDockerConfiguration {

    private final String image;
    private final String maxRetries;
    private final boolean pullImage;

    @DataBoundConstructor
    public DockerImageConfiguration(List<ConfigItem> configItemList,
                                    List<VolumeConfiguration> volumes,
                                    String image,
                                    ForcePull forcePull) {
        super(configItemList, volumes);
        this.image = image;
        if (forcePull != null ) {
            this.maxRetries = forcePull.maxRetries;
            this.pullImage = true;
        }
        else {
            this.maxRetries = "0";
            this.pullImage = false;
        }
    }
    
    public static class ForcePull {
        private String maxRetries;

        @DataBoundConstructor
        public ForcePull(String maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    public boolean isForcePull() {
        return pullImage;
    }

    public String getImage() {
        return image;
    }

    public String getRetries() {
        return maxRetries;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (StringUtils.isEmpty(image)) {
            throw new Descriptor.FormException("Docker image cannot be empty",
                                               "image");
        }
        for (ConfigItem item : getConfigItemList()) {
            item.validate();
        }
        for (VolumeConfiguration volume : getVolumes()) {
            volume.validate();
        }
	if (StringUtils.isEmpty(maxRetries)) {
            throw new Descriptor.FormException("Max Retries cannot be empty", "maxRetries");
	}
	if (!StringUtils.isNumeric(maxRetries)) {
	    throw new Descriptor.FormException("Max Retries must be an integer", "maxRetries");
	}
    }

    @Override
    public void setupImage(AbstractDockerLauncher launcher,
                           String localWorkspace) throws IOException, InterruptedException {
        if (isForcePull()) {
            ArgumentListBuilder args = new ArgumentListBuilder();
            String image = Utils.resolveVariables(launcher, getImage());
            String maxRetries = Utils.resolveVariables(launcher, getRetries());
            args.add("docker", "pull", image);
            int numRetries = Integer.parseInt(maxRetries);
            int retries = 0;
            int status;

            Launcher.ProcStarter proc = launcher.executeCommand(args)
                        .stderr(launcher.getListener().getLogger())
                        .stdout(launcher.getListener());
            status = proc.join();

            while (retries < numRetries && status != 0) {
                launcher.getListener().getLogger().println("Docker pull failed, retrying...");
                retries += 1;

                proc = launcher.executeCommand(args)
                        .stderr(launcher.getListener().getLogger())
                        .stdout(launcher.getListener());
                status = proc.join();
            }

            if (status != 0) {
                throw new IOException("Could not pull image: " + image);
            }
        }
    }

    @Override
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        getConfigItemList().stream()
                .forEach(item -> item.addCreateArgs(launcher, args));
        getVolumes().stream()
                .forEach(item -> item.addArgs(args, launcher));

        args.add(Utils.resolveVariables(launcher, getImage()));
    }

    @Symbol("image")
    @Extension
    public static class DescriptorImpl extends AbstractDockerConfigurationDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Docker Image";
        }
    }

}
