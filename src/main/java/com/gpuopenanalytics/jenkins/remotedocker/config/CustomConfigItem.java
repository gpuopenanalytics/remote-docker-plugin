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

package com.gpuopenanalytics.jenkins.remotedocker.config;

import com.gpuopenanalytics.jenkins.remotedocker.Utils;
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

    public String getResolvedValue(AbstractBuild<?,?> build) {
        return Utils.resolveVariables(build, getValue());
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
