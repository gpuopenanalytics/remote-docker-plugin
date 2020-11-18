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
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class UserConfigItem extends ConfigItem {

    private final boolean custom;
    private final String username;
    private final String uid;
    private final String gid;

    @DataBoundConstructor
    public UserConfigItem(boolean custom,
                          String username,
                          String uid,
                          String gid) {
        this.custom = custom;
        this.username = username;
        this.uid = uid;
        this.gid = gid;
    }

    public boolean isExisting() {
        return !isCustom()
                && !("jenkins".equals(username)
                || "root".equals(username)
                || "current".equals(username));
    }

    public boolean isCurrentUser() {
        return !isCustom() && "current".equals(username);
    }

    public boolean isCustom() {
        return custom;
    }

    public String getUsername() {
        return username;
    }

    public String getUid() {
        return uid;
    }

    public String getGid() {
        return gid;
    }

    @Override
    public void validate() throws Descriptor.FormException {
        if (StringUtils.isEmpty(username)) {
            throw new Descriptor.FormException("Username cannot be empty",
                                               "username");
        }
        if (!isExisting() && !isCurrentUser()) {
            if (StringUtils.isEmpty(uid)) {
                throw new Descriptor.FormException("UID cannot be empty",
                                                   "uid");
            }
            if (StringUtils.isEmpty(gid)) {
                throw new Descriptor.FormException("GID cannot be empty",
                                                   "gid");
            }
        } else {
            //This shouldn't ever happen if using the UI
            if (StringUtils.isNotEmpty(uid)) {
                throw new Descriptor.FormException(
                        "Inconsistent state: using existing user but a non-null uid",
                        "uid");
            }
            if (StringUtils.isNotEmpty(gid)) {
                throw new Descriptor.FormException(
                        "Inconsistent state: using existing user but a non-null gid",
                        "gid");
            }
        }
    }

    @Override
    public void addCreateArgs(AbstractDockerLauncher launcher,
                              ArgumentListBuilder args) {
        //No-op
    }

    @Override
    public void postCreate(AbstractDockerLauncher launcher) throws IOException, InterruptedException {
        if (!isExisting() && !"root".equals(username) && !isCurrentUser()) {
            String gid = Utils.resolveVariables(launcher, this.gid);
            String uid = Utils.resolveVariables(launcher, this.uid);
            String username = Utils.resolveVariables(launcher, this.username);

            createUserAndGroup(launcher, username, uid, gid);
        } else if (isCurrentUser()) {
            String uid = executeWithOutput(launcher, "id", "-u");
            String gid = executeWithOutput(launcher, "id", "-g");
            String username = executeWithOutput(launcher, "id", "-un");

            createUserAndGroup(launcher, username, uid, gid);
        }
    }

    private void createUserAndGroup(AbstractDockerLauncher launcher,
                                    String username,
                                    String uid,
                                    String gid) throws IOException, InterruptedException {
        ArgumentListBuilder groupAddArgs = new ArgumentListBuilder();
        groupAddArgs.add("groupadd", "-g", gid, username);
        int status = launcher.dockerExec(groupAddArgs, false).join();
        if (status != 0) {
            throw new IOException("Failed to create group");
        }

        ArgumentListBuilder userAddArgs = new ArgumentListBuilder();
        userAddArgs.add("useradd", "-g", gid, "-u", uid, username);
        status = launcher.dockerExec(userAddArgs, false).join();
        if (status != 0) {
            throw new IOException("Failed to create user");
        }
    }

    private String executeWithOutput(AbstractDockerLauncher launcher,
                                     String... args) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int status = launcher.getInner().launch()
                    .cmds(args)
                    .stdout(baos)
                    .stderr(launcher.getListener().getLogger())
                    .quiet(!launcher.isDebug())
                    .join();
            if (status != 0) {
                throw new RuntimeException(
                        "Non-zero status " + status + ": " + Arrays
                                .toString(args));
            }
            return baos.toString(StandardCharsets.UTF_8.name()).trim();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addRunArgs(AbstractDockerLauncher launcher,
                           ArgumentListBuilder args) {
        if (!isCurrentUser()) {
            args.add("--user", Utils.resolveVariables(launcher, username));
        } else {
            String uid = executeWithOutput(launcher, "id", "-u");
            String gid = executeWithOutput(launcher, "id", "-g");
            args.add("--user", uid + ":" + gid);
        }
    }

    @Symbol("user")
    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigItem> {

        @Override
        public String getDisplayName() {
            return "Build and run as user";
        }

        @Override
        public ConfigItem newInstance(@Nullable StaplerRequest req,
                                      @Nonnull JSONObject formData) throws FormException {
            String keyCode = formData.getString("user");
            String username = formData.getString(keyCode + "_username");
            String uid = formData.getString(keyCode + "_uid");
            String gid = formData.getString(keyCode + "_gid");
            boolean custom = formData.getBoolean(keyCode + "_custom");
            return new UserConfigItem(custom, username, uid, gid);
        }
    }
}
