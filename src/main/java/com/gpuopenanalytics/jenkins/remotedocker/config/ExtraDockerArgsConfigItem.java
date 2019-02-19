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

package com.gpuopenanalytics.jenkins.remotedocker.config;

import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.QuotedStringTokenizer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Allows user to add arbitrary docker arguments to <code>docker run</code>
 */
public class ExtraDockerArgsConfigItem extends ConfigItem {

    private String extraArgs;

    @DataBoundConstructor
    public ExtraDockerArgsConfigItem(String extraArgs) {
        this.extraArgs = extraArgs;
    }

    @Override
    public void validate() throws Descriptor.FormException {

    }

    @Override
    public void addCreateArgs(ArgumentListBuilder args, AbstractBuild build) {
        List<String> newArgs = Stream.of(
                QuotedStringTokenizer.tokenize(extraArgs))
                .map(s -> Utils.resolveVariables(
                        build.getBuildVariableResolver(), s))
                .collect(Collectors.toList());
        args.add(newArgs);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "Extra docker arguments";
        }
    }

}
