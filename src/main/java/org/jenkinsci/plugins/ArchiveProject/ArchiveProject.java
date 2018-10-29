package org.jenkinsci.plugins.ArchiveProject;


import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.model.TransientActionFactory;
import org.kohsuke.stapler.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.jenkinsci.plugins.ArchiveProject.consts.Const.*;


public class ArchiveProject implements Action {

    private AbstractProject project;

    public ArchiveProject(AbstractProject project) {
        this.project = project;
    }



    @CheckForNull
    @Override
    public String getIconFileName() {
        return NEW_DOCUMENT_IOC;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return ARCHIVE_PROJECT_DISPLAY_NAME;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return ARCHIVE_PROJECT_URL_NAME;
    }


    public AbstractProject getProject() {
        return project;
    }


    /**
    * @Description: 处理备份请求
    * @Param:
    * @return:
    * @Author: yuan
    * @Date: 2018/10/27 0027
    */
    public HttpRedirect doArchiveProject(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        //获取getNumToKeep
        int numToKeep = Integer.parseInt(req.getParameter(REQUEST_PARAMETER_NUMTOKEEP));

        //检查权限
        try{
            Jenkins.getInstance().checkPermission( Permission.CONFIGURE );
        }catch (Exception e){
            return HttpResponses.redirectTo("./");
        }

        //获取要备份的历史并排除最后一个成功构建和最后一次失败
        Run lsb = project.getLastSuccessfulBuild();
        Run lstb = project.getLastFailedBuild();

        List<? extends Run<?,?>> builds = project.getBuilds();
        List<? extends Run<?,?>> backups= new ArrayList<>(builds.subList(numToKeep > builds.size()-1 ? builds.size() : numToKeep,builds.size()));

        for (int i=backups.size()-1; i>=0; i--)
        {
            Run r = backups.get(i);
            if (r==lsb || r==lstb || r.isBuilding() || r.isKeepLog()){
                backups.remove(i);
            }
        }

        //备份历史
        File backupDir = new File(Jenkins.getInstance().getRootDir(), BACKUP_ROOT_DIR_NAME);
        File backupProjectDir = new File(backupDir, project.getName());
        backupProjectDir.mkdirs();
        try {
            Files.walkFileTree(project.getRootDir().toPath(), new CopyDir(project.getRootDir().toPath(), backupProjectDir.toPath(),backups));
        } catch (IOException e) {
            return HttpResponses.redirectTo("./");
        }

        //删除历史记录
        for (Run item : backups){
            item.delete();
        }

        return HttpResponses.redirectTo("./");
    }

    /** 
    * @Description: 递归复制
    * @Param:  
    * @return:  
    * @Author: yuan
    * @Date: 2018/10/27 0027 
    */ 
    public class CopyDir extends SimpleFileVisitor<Path> {
        private Path sourceDir;
        private Path targetDir;
        private List<? extends Run<?,?>> backups;

        public CopyDir(Path sourceDir, Path targetDir,List<? extends Run<?,?>> backups) {
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
            this.backups = backups;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes basicFileAttributes) {
            //排序不需要备份的文件
            if (file.getParent().getParent().getFileName().toString().equals(PROJECT_BUILDS_DIR_NAME)){
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
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes basicFileAttributes) {
            if (dir.toString().equals(sourceDir.toString()) || dir.getFileName().toString().equals(PROJECT_BUILDS_DIR_NAME)){
                createDir(dir);
                return FileVisitResult.CONTINUE;
            }
            //将要备份的目录创建并进入
            for (Run item : backups) {
                if (!Files.isSymbolicLink(dir) && item.getRootDir().getName().equals(dir.getFileName().toString())){
                    createDir(dir);
                    return FileVisitResult.CONTINUE;
                }
            }
            return FileVisitResult.SKIP_SUBTREE ;
        }

        private void createDir(Path dir){
            try {
                Path newDir = targetDir.resolve(sourceDir.relativize(dir));
                Files.createDirectory(newDir);
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    /** 
    * @Description: 扩展点 
    * @Param:  
    * @return:  
    * @Author: yuan
    * @Date: 2018/10/27 0027 
    */ 
    @Extension
    public static class ArchiveProjectFactory extends TransientActionFactory<AbstractProject> {

        @Override
        public Class<AbstractProject> type() {
            return AbstractProject.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull AbstractProject project) {
            return Collections.singleton(new ArchiveProject(project));
        }
    }
}
