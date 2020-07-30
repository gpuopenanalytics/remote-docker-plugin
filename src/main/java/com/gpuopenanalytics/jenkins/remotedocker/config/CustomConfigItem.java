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

import com.gpuopenanalytics.jenkins.remotedocker.AbstractDockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ConfigItem} that may have options but also free text entry.
 */
public abstract class CustomConfigItem extends ConfigItem {

    public static final String CUSTOM_VALUE_INDICATOR = "custom";

    private String value;
    private transient Optional<String> customValue;
    private String customVal;

    public CustomConfigItem(String value, String customValue) {
        this.value = value;
        if (StringUtils.isEmpty(customValue)) {
            this.customVal = null;
        } else {
            this.customVal = customValue;
        }
    }

    /**
     * Whether a custom value is being provided
     *
     * @return
     */
    public boolean isCustom() {
        return CUSTOM_VALUE_INDICATOR.equals(value);
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
        return Optional.ofNullable(customVal).orElse(value);
    }

    public String getResolvedValue(AbstractDockerLauncher launcher) {
        return Utils.resolveVariables(launcher, getValue());
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
        return Optional.ofNullable(customVal);
    }

    protected Object readResolve() {
        if (customValue != null) {
            customVal = customValue.orElse(null);
        }
        return this;
    }
}
