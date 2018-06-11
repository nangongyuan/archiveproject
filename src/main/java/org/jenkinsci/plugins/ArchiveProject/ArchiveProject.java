package org.jenkinsci.plugins.ArchiveProject;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.model.TransientActionFactory;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
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

    /*
    private static void copyFolder(File sourceFolder, File destinationFolder) throws IOException
    {
        CopyOption[] copyOptions = new CopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
                LinkOption.NOFOLLOW_LINKS
        };
        if (sourceFolder.isDirectory()) {
            if (!destinationFolder.exists()){
                destinationFolder.mkdir();
            }

            String files[] = sourceFolder.list();
            for (String file: files) {
                File scrFile = new File(sourceFolder, file);
                File destFile = new File(destinationFolder, file);

                copyFolder(scrFile, destFile);
            }
        } else {
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), copyOptions);
        }
    }
    */

    public HttpResponse doArchiveProject() throws IOException, InterruptedException {

        Jenkins.getInstance().checkPermission( Permission.CONFIGURE );

        File backupDir = new File(Jenkins.getInstance().getRootDir(), "backup");
        File backupProjectDir = new File(backupDir, project.getName());
        backupProjectDir.mkdirs();
        //Files.copy(project.getRootDir().toPath(), backupProjectDir.toPath(), copyOptions);
        // FileUtils.copyDirectory(project.getRootDir(), backupProjectDir);
        // copyFolder(project.getRootDir(), backupProjectDir);
        Files.walkFileTree(project.getRootDir().toPath(), new CopyDir(project.getRootDir().toPath(), backupProjectDir.toPath()));
        File backupProjectBuildsDir = new File(backupProjectDir, "builds");

        if (backupProjectBuildsDir.exists()) {
            new DiscardOldBuilds(getNumToKeep()).perform(project);
        }

        return HttpResponses.redirectTo(".");
    }

    public class CopyDir extends SimpleFileVisitor<Path> {
        private Path sourceDir;
        private Path targetDir;

        public CopyDir(Path sourceDir, Path targetDir) {
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes basicFileAttributes) {
            try {
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));
                CopyOption[] copyOptions = new CopyOption[] {
                        StandardCopyOption.REPLACE_EXISTING,
                        LinkOption.NOFOLLOW_LINKS
                };
                Files.copy(file, targetFile, copyOptions);
            } catch (IOException ex) {
                System.err.println(ex);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes basicFileAttributes) {
            try {
                Path newDir = targetDir.resolve(sourceDir.relativize(dir));
                Files.createDirectory(newDir);
            } catch (IOException ex) {
                System.err.println(ex);
            }

            return FileVisitResult.CONTINUE;
        }
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
