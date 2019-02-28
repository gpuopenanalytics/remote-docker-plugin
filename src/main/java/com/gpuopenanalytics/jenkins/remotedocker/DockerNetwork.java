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

import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Represents a bridge network created for docker
 */
public class DockerNetwork {

    private String id;

    private DockerNetwork(String id) {
        this.id = id;
    }

    /**
     * Create a bridge network using the specified {@link DockerLauncher}
     *
     * @param launcher
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static DockerNetwork create(DockerLauncher launcher) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("docker", "network", "create", UUID.randomUUID().toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int status = launcher.executeCommand(args)
                .stdout(baos)
                .stderr(launcher.getListener().getLogger())
                .join();

        if (status != 0) {
            throw new IOException("Could not create network");
        }
        String id = baos.toString(StandardCharsets.UTF_8.name()).trim();

        return new DockerNetwork(id);
    }

    /**
     * Adds this network to the argument list for <code>docker run</code>
     *
     * @param args
     */
    public void addArgs(ArgumentListBuilder args) {
        args.add("--network", id);
    }

    public void tearDown(DockerLauncher launcher) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("docker", "network", "rm", id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int status = launcher.executeCommand(args)
                .stdout(launcher.getListener())
                .stderr(launcher.getListener().getLogger())
                .join();

        if (status != 0) {
            throw new IOException("Could not remove network");
        }
    }

}
