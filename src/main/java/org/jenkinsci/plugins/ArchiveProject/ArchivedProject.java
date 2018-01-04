package org.jenkinsci.plugins.ArchiveProject;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.security.Permission;
import hudson.widgets.Widget;
import jenkins.model.TransientActionFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileFilter;
import java.util.*;

public class ArchivedProject implements Action {

    private Project project;

    public ArchivedProject(Project project) {
        this.project = project;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Archived Project";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "archived";
    }

    public List<String> getArchivedProjects() {
        Hudson.getInstanceOrNull().checkPermission(Permission.READ);

        final File archivedProjectsDir = new File(Hudson.getInstanceOrNull().getRootDir(), "backup/" + project.getDisplayName() + "/builds");

        File[] archivedProjects = archivedProjectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);

        //Collection<File> allBuilds = new ArrayList<File>();

        List<String> allBuilds = new LinkedList<String>();

        for (File p:archivedProjects) {
            allBuilds.add(p.getName());
        }
        return allBuilds;
    }

    public ArchiveWidget getArchiveWidget() {
        List<ArchiveWidget.Backups> backupsList = new ArrayList<ArchiveWidget.Backups>();
        for (int i=1; i < 5; i++) {
            ArchiveWidget.Backups backups = new ArchiveWidget.Backups();
            backups.setNumber(Integer.toString(i));
            backups.setName("The #" + Integer.toString(i) + " builds...");
            backups.setDuration(Double.toString(Math.random()*i));
            backupsList.add(backups);
        }
        ArchiveWidget archiveWidget = new ArchiveWidget();
        archiveWidget.setBackupsList(backupsList);
        return archiveWidget;
    }

    @Extension
    public static class ArchivedProjectFactory extends TransientActionFactory<Project> {

        @Override
        public Class<Project> type() {
            return Project.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull Project project) {
            return Collections.singleton(new ArchivedProject(project));
        }
    }
}
