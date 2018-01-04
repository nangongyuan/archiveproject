package org.jenkinsci.plugins.ArchiveProject;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

public class ArchiveProjectProperty extends JobProperty<Project<?,?>> {

    private int numToKeep;

    @DataBoundConstructor
    public ArchiveProjectProperty(int numToKeep) {
        this.numToKeep = numToKeep;
    }

    public int getNumToKeep() {
        return numToKeep;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        public String getDisplayName() {
            return "Numbers to Keep";
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ArchiveProjectProperty app = req.bindJSON(
                    ArchiveProjectProperty.class,
                    formData.getJSONObject("archiveProject"));
            if (app == null) {
                return null;
            }
            return app;
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return ParameterizedJobMixIn.ParameterizedJob.class.isAssignableFrom(jobType);
        }


    }
}
