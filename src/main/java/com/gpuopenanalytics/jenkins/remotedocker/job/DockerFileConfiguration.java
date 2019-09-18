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

import com.gpuopenanalytics.jenkins.remotedocker.DockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import com.gpuopenanalytics.jenkins.remotedocker.config.ConfigItem;
import com.gpuopenanalytics.jenkins.remotedocker.config.VolumeConfiguration;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class DockerFileConfiguration extends AbstractDockerConfiguration {

    private String dockerFile;
    private String context;

    private String buildArgs;
    private boolean forcePull;
    private boolean forceBuild;
    private boolean squash;
    private String tag;

    //This is calculated at build time, so don't persist it
    private transient String image;

    @DataBoundConstructor
    public DockerFileConfiguration(List<ConfigItem> configItemList,
                                   List<VolumeConfiguration> volumes,
                                   String dockerFile,
                                   String context,
                                   String buildArgs,
                                   boolean forcePull,
                                   boolean forceBuild,
                                   boolean squash,
                                   String tag) {
        super(configItemList, volumes);
        this.dockerFile = dockerFile;
        this.context = context;
        this.buildArgs = buildArgs;
        this.forcePull = forcePull;
        this.forceBuild = forceBuild;
        this.squash = squash;
        this.tag = tag;
    }

    public String getDockerFile() {
        return dockerFile;
    }

    public String getContext() {
        return context;
    }

    public String getBuildArgs() {
        return buildArgs;
    }

    public boolean isForcePull() {
        return forcePull;
    }

    public boolean isForceBuild() {
        return forceBuild;
    }

    public boolean isSquash() {
        return squash;
    }

    public String getTag() {
        return tag;
    }

    public String getImage() {
        return image;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        //TODO Use https://github.com/asottile/dockerfile to validate the contents?
        if (StringUtils.isEmpty(dockerFile)) {
            throw new Descriptor.FormException(
                    "You must specify a Dockerfile to use",
                    "dockerFile");
        }
        for (ConfigItem item : getConfigItemList()) {
            item.validate();
        }
        for (VolumeConfiguration volume : getVolumes()) {
            volume.validate();
        }
    }

    @Override
    public void setupImage(DockerLauncher launcher,
                           String localWorkspace) throws IOException, InterruptedException {
        AbstractBuild build = launcher.getBuild();
        ArgumentListBuilder args = new ArgumentListBuilder("docker", "build");
        if (forcePull) {
            args.add("--pull");
        }
        if (forceBuild) {
            args.add("--no-cache");
        }
        if (squash) {
            args.add("--squash");
        }
        if (StringUtils.isNotEmpty(buildArgs)) {
            Properties props = Utils.parsePropertiesString(buildArgs);
            for (String key : props.stringPropertyNames()) {
                String value = Utils.resolveVariables(launcher,
                                                      props.getProperty(key));
                args.add("--build-arg");
                args.addKeyValuePair("", key, value, false);
            }
        }
        if (StringUtils.isNotEmpty(tag)) {
            image = Utils.resolveVariables(launcher, tag);
        } else {
            image = UUID.randomUUID().toString();
        }
        args.add("-t", image);

        String dockerFilePath = Utils.resolveVariables(launcher, dockerFile);
        Path path = Paths.get(dockerFilePath);
        if (!path.isAbsolute()) {
            path = Paths.get(localWorkspace, dockerFilePath);
        }
        args.add("-f", path.toString());
        if (StringUtils.isNotEmpty(context)) {
            args.add(Utils.resolveVariables(launcher, context));
        } else {
            args.add(build.getWorkspace().getRemote());
        }

        int status = launcher.executeCommand(args)
                .stdout(launcher.getListener().getLogger())
                .stderr(launcher.getListener().getLogger())
                .join();
        if (status != 0) {
            throw new RuntimeException("Docker image failed to build.");
        }
    }

    @Override
    public void addCreateArgs(DockerLauncher launcher,
                              ArgumentListBuilder args,
                              AbstractBuild build) {
        getConfigItemList().stream()
                .forEach(item -> item.addCreateArgs(launcher, args, build));
        getVolumes().stream()
                .forEach(item -> item.addArgs(args, launcher));
        args.add(image);
    }

    @Extension
    public static class DescriptorImpl extends AbstractDockerConfigurationDescriptor {

        @Override
        public String getDisplayName() {
            return "Build Dockerfile";
        }

    }
}
