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
     * Create a bridge network using the specified {@link AbstractDockerLauncher}
     *
     * @param launcher
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static DockerNetwork create(AbstractDockerLauncher launcher) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("docker", "network", "create", "-d", "bridge",
                 UUID.randomUUID().toString());

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
     * Create a {@link DockerNetwork} if you already have an ID
     *
     * @param id
     * @return
     */
    public static DockerNetwork fromExisting(String id) {
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

    public String getId() {
        return id;
    }
}
