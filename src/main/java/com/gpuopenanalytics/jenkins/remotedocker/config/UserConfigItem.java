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

import com.gpuopenanalytics.jenkins.remotedocker.DockerLauncher;
import com.gpuopenanalytics.jenkins.remotedocker.Utils;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

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
                && !("jenkins".equals(username) || "root".equals(username));
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
        if (!isExisting()) {
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
    public void addCreateArgs(DockerLauncher launcher,
                              ArgumentListBuilder args,
                              AbstractBuild build) {
        //No-op
    }

    @Override
    public void postCreate(DockerLauncher launcher,
                           AbstractBuild build) throws IOException, InterruptedException {
        if (!isExisting() && !"root".equals(username)) {
            VariableResolver r = build.getBuildVariableResolver();
            String gid = Utils.resolveVariables(r, this.gid);
            String uid = Utils.resolveVariables(r, this.uid);
            String username = Utils.resolveVariables(r,
                                                     this.username);

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
    }

    @Override
    public void addRunArgs(DockerLauncher launcher,
                           ArgumentListBuilder args,
                           AbstractBuild build) {
        args.add("--user", Utils.resolveVariables(
                build.getBuildVariableResolver(), username));
    }

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
