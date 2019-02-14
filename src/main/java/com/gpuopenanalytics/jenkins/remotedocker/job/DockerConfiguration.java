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
import com.gpuopenanalytics.jenkins.remotedocker.config.ConfigItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a method of creating a docker container
 */
public abstract class DockerConfiguration extends AbstractDescribableImpl<DockerConfiguration> {

    private static final Pattern VAR_REGEX = Pattern.compile(
            "\\$(\\w+)|\\$\\{([^}]+)}");

    protected List<ConfigItem> configItemList;

    public DockerConfiguration(List<ConfigItem> configItemList) {
        this.configItemList = configItemList == null ? Lists.newArrayList() : configItemList;
    }

    public List<ConfigItem> getConfigItemList() {
        return configItemList;
    }

    /**
     * Validate the correctness of the configuration. Subclasses should validate
     * their {@link ConfigItem} list.
     *
     * @throws Descriptor.FormException
     */
    public abstract void validate() throws Descriptor.FormException;

    /**
     * Build up the <code>docker create</code> argument list
     * @param args
     * @param build
     */
    public abstract void addArgs(ArgumentListBuilder args, AbstractBuild build);

    /**
     * Finds <code>$VAR</code> or <code>${VAR}</code> in the specified string
     * that exist in the resolver and resolves them. If the variable is not
     * resolved, it is left unchanged.
     *
     * @param resolver
     * @param s
     * @return
     */
    public static String resolveVariables(VariableResolver<String> resolver,
                                          String s) {
        //Matcher requires StringBuffer :(
        StringBuffer sb = new StringBuffer();
        Matcher m = VAR_REGEX.matcher(s);
        while (m.find()) {
            String varName = java.util.Optional.ofNullable(m.group(1))
                    .orElseGet(() -> m.group(2));
            Optional<String> newValue = Optional.ofNullable(
                    resolver.resolve(varName));
            m.appendReplacement(sb, newValue.orElseGet(() -> "\\$" + varName));
        }
        m.appendTail(sb);
        return sb.toString();
    }

}
