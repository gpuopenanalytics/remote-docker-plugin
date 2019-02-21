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
import com.gpuopenanalytics.jenkins.remotedocker.config.ConfigItem;
import com.gpuopenanalytics.jenkins.remotedocker.config.VolumeConfiguration;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class DockerFileConfiguration extends DockerConfiguration {

    private String dockerFile;
    private String dockerFileText;
    private String context;

    private String buildArgs;
    private boolean forcePull;
    private boolean squash;
    private String tag;

    //This is calculated at build time, so don't persist it
    private transient String image;

    public DockerFileConfiguration(List<ConfigItem> configItemList,
                                   List<VolumeConfiguration> volumes,
                                   String dockerFile,
                                   String dockerFileText,
                                   String context,
                                   String buildArgs,
                                   boolean forcePull,
                                   boolean squash,
                                   String tag) {
        super(configItemList, volumes);
        this.dockerFile = dockerFile;
        this.dockerFileText = dockerFileText;
        this.context = context;
        this.buildArgs = buildArgs;
        this.forcePull = forcePull;
        this.squash = squash;
        this.tag = tag;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        //TODO Use https://github.com/asottile/dockerfile to validate the contents?
        if (StringUtils.isEmpty(dockerFile)
                && StringUtils.isEmpty(dockerFileText)) {
            throw new Descriptor.FormException(
                    "You must specify a Dockerfile to use",
                    "dockerFile");
        }
        try {
            if (StringUtils.isNotEmpty(buildArgs)) {
                parsePropertiesString(buildArgs);
            }
        } catch (IOException e) {
            throw new Descriptor.FormException(e.getMessage(), "environment");
        }
        for (ConfigItem item : getConfigItemList()) {
            item.validate();
        }
        for (VolumeConfiguration volume : getVolumes()) {
            volume.validate();
        }
    }

    @Override
    public void setupImage(DockerLauncher launcher) throws IOException, InterruptedException {
        AbstractBuild build = launcher.getBuild();
        ArgumentListBuilder args = new ArgumentListBuilder("build");
        if (forcePull) {
            args.add("--pull");
        }
        if (squash) {
            args.add("--squash");
        }
        if (StringUtils.isNotEmpty(buildArgs)) {
            Properties props = parsePropertiesString(buildArgs);
            for (String key : props.stringPropertyNames()) {
                String value = Utils.resolveVariables(build,
                                                      props.getProperty(key));
                args.add("--build-arg");
                args.addKeyValuePair("", key, value, false);
            }
        }
        if (StringUtils.isNotEmpty(tag)) {
            image = Utils.resolveVariables(build, tag);
        } else {
            image = UUID.randomUUID().toString();
        }
        args.add("-t", image);

        if (StringUtils.isNotEmpty(dockerFile)) {
            args.add("-f", Utils.resolveVariables(build, dockerFile));
        }
        if (StringUtils.isNotEmpty(context)) {
            args.add(Utils.resolveVariables(build, context));
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
    public void addCreateArgs(ArgumentListBuilder args, AbstractBuild build) {
        getConfigItemList().stream()
                .forEach(item -> item.addCreateArgs(args, build));
        getVolumes().stream()
                .forEach(item -> item.addArgs(args, build));
        args.add(image);
    }

    @Extension
    public static class DescriptorImpl extends DockerConfigurationDescriptor {

        @Override
        public String getDisplayName() {
            return "Build Dockerfile";
        }
    }

    private Properties parsePropertiesString(String s) throws IOException {
        final Properties p = new Properties();
        p.load(new StringReader(s));
        return p;
    }
}
