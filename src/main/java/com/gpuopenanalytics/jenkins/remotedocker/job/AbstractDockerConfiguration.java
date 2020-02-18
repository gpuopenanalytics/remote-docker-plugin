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

import com.google.common.collect.Lists;
import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.config.ConfigItem;
import com.gpuopenanalytics.jenkins.remotedocker.config.VolumeConfiguration;
import hudson.model.AbstractDescribableImpl;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Represents a method of creating a docker container
 */
public abstract class AbstractDockerConfiguration extends AbstractDescribableImpl<AbstractDockerConfiguration> implements DockerConfiguration {

    protected List<ConfigItem> configItemList;
    protected List<VolumeConfiguration> volumes;

    public AbstractDockerConfiguration(List<ConfigItem> configItemList,
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

    @Override
    public void postCreate(AbstractDockerLauncher launcher) throws IOException, InterruptedException {
        for (ConfigItem item : configItemList) {
            item.postCreate(launcher);
        }
    }

    @Override
    public void addRunArgs(AbstractDockerLauncher launcher,
                           ArgumentListBuilder args) {
        for (ConfigItem item : configItemList) {
            item.addRunArgs(launcher, args);
        }
    }


}
