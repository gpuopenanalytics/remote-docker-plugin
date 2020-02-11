/*
 * The MIT License
 *
 * Copyright (c) 2020, NVIDIA CORPORATION.
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

import java.io.Serializable;

public class DockerVersion implements Serializable {

    public static final DockerVersion DEFAULT = new DockerVersion(0, 0, 0, null,
                                                                  "0");

    private int major;
    private int minor;
    private int patch;
    private String extra;
    private String build;

    private DockerVersion(int major,
                          int minor,
                          int patch,
                          String extra,
                          String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.extra = extra;
        this.build = build;
    }

    /**
     * Parse from output of <code>docker --version</code> such as <code>Docker
     * version 19.03.5, build 633a0ea</code>
     *
     * @return
     */
    public static DockerVersion fromVersionString(String versionString) throws VersionParseException {
        try {
            String[] split = versionString.split("\\s");
            String build = split[split.length - 1];
            String[] version = split[2].substring(0, split[2].length() - 1)
                    .split(
                            "\\.");
            int major = Integer.parseInt(version[0]);
            int minor = Integer.parseInt(version[1]);
            int patch = 0;
            String extra = null;
            if (version[2].contains("-")) {
                patch = Integer.parseInt(
                        version[2].substring(0, version[2].indexOf('-')));
                extra = version[2].substring(version[2].indexOf('-') + 1);
            } else {
                patch = Integer.parseInt(version[2]);
            }
            return new DockerVersion(major, minor, patch, extra, build);
        } catch (Exception e) {
            throw new VersionParseException(versionString, e);
        }
    }

    public String getVersionString() {
        return String.format("%02d.%02d.%d", major, minor, patch);
    }

    public boolean hasGpuFlag() {
        return "19.03.0".compareTo(getVersionString()) < 0;
    }

    public String toString() {
        if (extra != null) {
            return String.format("Docker version %s-%s, build %s",
                                 getVersionString(),
                                 extra,
                                 build);
        } else {
            return String.format("Docker version %s, build %s",
                                 getVersionString(),
                                 build);
        }
    }

    public static class VersionParseException extends Exception {

        public VersionParseException(String version, Throwable cause) {
            super(String.format("Could not parse '%s'", version), cause);
        }
    }
}
