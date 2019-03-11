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

import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern VAR_REGEX = Pattern.compile(
            "\\$(\\w+)|\\$\\{([^}]+)}");

    private Utils() {

    }


    /**
     * Finds <code>$VAR</code> or <code>${VAR}</code> in the specified string
     * that exist in the build's resolver and resolves them. If the variable is
     * not resolved, it is left unchanged.
     *
     * @param build
     * @param s
     * @return
     */
    public static String resolveVariables(AbstractBuild build, String s) {
        return resolveVariables(build.getBuildVariableResolver(), s);
    }

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
            String newValue = Optional.ofNullable(resolver.resolve(varName))
                    .orElseGet(() -> "\\${" + varName + "}");
            m.appendReplacement(sb, newValue);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Returns whether the string contains any <code>$VAR</code> or
     * <code>${VAR}</code> that could be replaced.
     *
     * @param s
     * @return
     */
    public static boolean hasVariablesToResolve(String s) {
        Matcher m = VAR_REGEX.matcher(s);
        return m.find();
    }

    /**
     * Parses a String representation of a properties file into a {@link
     * Properties}
     *
     * @param s
     * @return
     */
    public static Properties parsePropertiesString(String s) {
        try {
            final Properties p = new Properties();
            p.load(new StringReader(s));
            return p;
        } catch (IOException e) {
            //This shouldn't happen
            throw new RuntimeException(e);
        }
    }
}
