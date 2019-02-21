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

import com.google.common.collect.Lists;
import com.gpuopenanalytics.jenkins.remotedocker.DockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.config.ConfigItem;
import com.gpuopenanalytics.jenkins.remotedocker.config.VolumeConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Represents a method of creating a docker container
 */
public abstract class DockerConfiguration extends AbstractDescribableImpl<DockerConfiguration> {

    protected List<ConfigItem> configItemList;
    protected List<VolumeConfiguration> volumes;

    public DockerConfiguration(List<ConfigItem> configItemList,
                               List<VolumeConfiguration> volumes) {
        this.configItemList = configItemList == null ? Lists.newArrayList() : configItemList;
        this.volumes = volumes == null ? Lists.newArrayList() : volumes;
    }

    public List<ConfigItem> getConfigItemList() {
        return configItemList;
    }

    public List<VolumeConfiguration> getVolumes() {
        return volumes;
    }

    /**
     * Validate the correctness of the configuration. Subclasses should validate
     * their {@link ConfigItem} list.
     *
     * @throws Descriptor.FormException
     */
    public abstract void validate() throws Descriptor.FormException;

    /**
     * Called before the container is started.. This allows for sub classes to
     * perform setup tasks such as building an image.
     *
     * @param launcher
     */
    public abstract void setupImage(DockerLauncher launcher) throws IOException, InterruptedException;

    /**
     * Build up the <code>docker create</code> argument list
     *
     * @param args
     * @param build
     */
    public abstract void addCreateArgs(ArgumentListBuilder args,
                                       AbstractBuild build);

    /**
     * Runs after the container is running, but before the build executes
     *
     * @param launcher
     * @param build
     */
    public void postCreate(DockerLauncher launcher,
                           AbstractBuild build) throws IOException, InterruptedException {
        for (ConfigItem item : configItemList) {
            item.postCreate(launcher, build);
        }
    }

    /**
     * Add the arguments to <code>docker exec</code> command that actually
     * executes the build
     *
     * @param args
     */
    public void addRunArgs(ArgumentListBuilder args, AbstractBuild build) {
        for (ConfigItem item : configItemList) {
            item.addRunArgs(args, build);
        }
    }


}
