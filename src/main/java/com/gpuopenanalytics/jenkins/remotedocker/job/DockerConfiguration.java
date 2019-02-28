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
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;

public interface DockerConfiguration {

    /**
     * Validate the correctness of the configuration.
     *
     * @throws Descriptor.FormException
     */
    void validate() throws Descriptor.FormException;

    /**
     * Called before the container is started. This allows for sub classes to
     * perform setup tasks such as building an image.
     *
     * @param launcher
     * @param localWorkspace
     */
    void setupImage(DockerLauncher launcher,
                    String localWorkspace) throws IOException, InterruptedException;

    /**
     * Add args to the <code>docker create</code>
     *
     * @param launcher
     * @param args
     * @param build
     */
    void addCreateArgs(DockerLauncher launcher,
                       ArgumentListBuilder args,
                       AbstractBuild build);

    /**
     * Runs after the container is running, but before the build executes
     *
     * @param launcher
     * @param build
     */
    void postCreate(DockerLauncher launcher,
                    AbstractBuild build) throws IOException, InterruptedException;

    /**
     * Add the arguments to <code>docker exec</code> command that actually
     * executes the build
     *
     * @param args
     */
    void addRunArgs(DockerLauncher launcher,
                    ArgumentListBuilder args,
                    AbstractBuild build);
}
