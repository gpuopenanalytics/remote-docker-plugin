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

package com.gpuopenanalytics.jenkins.remotedocker.pipeline;

import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.LauncherDecorator;
import hudson.Proc;
import hudson.model.Node;
import hudson.util.ArgumentListBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class DockerLauncherDecorator extends LauncherDecorator implements Serializable {

    private boolean debug;
    private String mainContainerId;
    private DockerConfiguration dockerConfiguration;

    public DockerLauncherDecorator(boolean debug,
                                   String mainContainerId,
                                   DockerConfiguration dockerConfiguration) {
        this.debug = debug;
        this.mainContainerId = mainContainerId;
        this.dockerConfiguration = dockerConfiguration;
    }

    @Nonnull
    @Override
    public Launcher decorate(@Nonnull Launcher launcher, @Nonnull Node node) {

        return new AbstractDockerLauncher(launcher) {

            /**
             * Invoke <code>docker exec</code> on the already created container.
             *
             * @param starter
             * @param addRunArgs
             * @return
             * @throws IOException
             */
            public Proc dockerExec(Launcher.ProcStarter starter,
                                   boolean addRunArgs) throws IOException {
                ArgumentListBuilder args = new ArgumentListBuilder()
                        .add("exec");
                if (starter.pwd() != null) {
                    args.add("--workdir", starter.pwd().getRemote());
                }
                if (addRunArgs) {
                    dockerConfiguration.addRunArgs(this, args);
                }

                args.add(mainContainerId);

                args.add("env").add(starter.envs());

                List<String> originalCmds = starter.cmds();
                boolean[] originalMask = starter.masks();
                for (int i = 0; i < originalCmds.size(); i++) {
                    boolean masked = originalMask == null ? false : i < originalMask.length ? originalMask[i] : false;
                    args.add(originalCmds.get(i), masked);
                }
                Launcher.ProcStarter procStarter = executeCommand(args);

                if (starter.stdout() != null) {
                    procStarter.stdout(starter.stdout());
                } else {
                    procStarter.stdout(listener.getLogger());
                }
                if (starter.stderr() != null) {
                    procStarter.stderr(starter.stderr());
                } else {
                    procStarter.stderr(listener.getLogger());
                }

                return procStarter.start();
            }

            @Override
            public EnvVars getEnvironment() {
                return new EnvVars();
            }

            @Override
            public boolean isDebug() {
                return debug;
            }

            /**
             * Execute a docker command
             *
             * @param args
             * @return
             */
            public Launcher.ProcStarter executeCommand(ArgumentListBuilder args) {
                if (args.toList().isEmpty()) {
                    throw new IllegalArgumentException("No args given");
                }
                if (!"docker".equals(args.toList().get(0))) {
                    args.prepend("docker");
                }
                return getInner().launch()
                        //TODO I think we should pass something here
                        //.envs()
                        .cmds(args)
                        .quiet(!debug);
            }
        };
    }


}
