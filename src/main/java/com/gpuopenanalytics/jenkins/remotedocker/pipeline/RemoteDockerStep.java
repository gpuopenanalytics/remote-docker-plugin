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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.gpuopenanalytics.jenkins.remotedocker.job.AbstractDockerConfiguration;
import com.gpuopenanalytics.jenkins.remotedocker.job.SideDockerConfiguration;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RemoteDockerStep extends Step {

    private boolean debug;
    private AbstractDockerConfiguration main;
    private List<SideDockerConfiguration> sideContainers;

    @DataBoundConstructor
    public RemoteDockerStep(boolean debug,
                            AbstractDockerConfiguration main,
                            List<SideDockerConfiguration> sideContainers) {
        this.debug = debug;
        this.main = main;
        this.sideContainers = ImmutableList.copyOf(
                Optional.ofNullable(sideContainers)
                        .orElse(Collections.emptyList()));
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new RemoteDockerStepExecution(stepContext, this);
    }

    public boolean isDebug() {
        return debug;
    }

    public AbstractDockerConfiguration getMain() {
        return main;
    }

    public List<SideDockerConfiguration> getSideContainers() {
        return sideContainers;
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class,
                                   FilePath.class,
                                   Launcher.class,
                                   EnvVars.class);
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Remote Docker";
        }

        @Override
        public String getFunctionName() {
            return "withRemoteDocker";
        }
    }
}
