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

import com.gpuopenanalytics.jenkins.remotedocker.job.DockerConfiguration;
import hudson.model.AbstractBuild;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ConfigItem} that may have options but also free text entry.
 */
public abstract class CustomConfigItem extends ConfigItem {

    public static final String CUSTOM_VALUE_INDICATOR = "custom";

    private String value;
    private Optional<String> customValue;

    public CustomConfigItem(String value, String customValue) {
        this.value = value;
        if (CUSTOM_VALUE_INDICATOR.equals(value)) {
            this.customValue = Optional.ofNullable(customValue);
        } else {
            this.customValue = Optional.empty();
        }
    }

    /**
     * Whether a custom value is being provided
     *
     * @return
     */
    public boolean isCustom() {
        return customValue.isPresent();
    }

    /**
     * Wether the value specified is the default value
     *
     * @return
     */
    public boolean isDefault() {
        return Objects.equals(getValue(), getDefault());
    }

    /**
     * Return either the custom value or the selected one
     *
     * @return
     */
    public String getValue() {
        return customValue.orElse(value);
    }

    public String getResolvedValue(AbstractBuild build) {
        return DockerConfiguration.resolveVariables(
                build.getBuildVariableResolver(), getValue());
    }

    /**
     * Return the default value for this item
     *
     * @return
     */
    public abstract String getDefault();

    /**
     * Used to get the raw value. Prefer {@link #getValue()} over this.
     */
    public String getRawValue() {
        return value;
    }

    /**
     * Used to get the raw custom value. Prefer {@link #getValue()} over this.
     */
    public Optional<String> getRawCustomValue() {
        return customValue;
    }
}
