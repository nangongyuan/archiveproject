package org.jenkinsci.plugins.ArchiveProject;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.TransientActionFactory;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class ArchiveProject implements Action {

    private Project project;

    public ArchiveProject(Project project) {
        this.project = project;
    }

    public int getNumToKeep() {
        ArchiveProjectProperty archiveProjectProperty = (ArchiveProjectProperty)project.getProperty(ArchiveProjectProperty.class);
        return archiveProjectProperty != null ? archiveProjectProperty.getNumToKeep() : 500;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Archive Project";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "archive";
    }


    public AbstractProject getProject() {
        return project;
    }

    public HttpResponse doArchiveProject() throws IOException, InterruptedException {

        Hudson.getInstanceOrNull().checkPermission( Permission.CONFIGURE );

        File backupDir = new File(Hudson.getInstanceOrNull().getRootDir(), "backup");
        File backupProjectDir = new File(backupDir, project.getName());
        backupProjectDir.mkdirs();
        FileUtils.copyDirectory(project.getRootDir(), backupProjectDir);
        File backupProjectBuildsDir = new File(backupProjectDir, "builds");

        if (backupProjectBuildsDir.exists()) {
            new DiscardOldBuilds(getNumToKeep()).perform(project);
        }

        return HttpResponses.redirectTo(".");
    }

    @Extension
    public static class ArchiveProjectFactory extends TransientActionFactory<Project> {

        @Override
        public Class<Project> type() {
            return Project.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull Project project) {
            return Collections.singleton(new ArchiveProject(project));
        }
    }
}
