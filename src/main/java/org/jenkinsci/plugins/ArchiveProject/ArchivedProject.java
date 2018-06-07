package org.jenkinsci.plugins.ArchiveProject;

import com.jcabi.xml.XMLDocument;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.model.TransientActionFactory;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.*;

import com.jcabi.xml.XML;
import org.kohsuke.stapler.bind.JavaScriptMethod;

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
        Jenkins.getInstance().checkPermission(Permission.READ);

        final File archivedProjectsDir = new File(Jenkins.getInstance().getRootDir(), "backup/" + project.getDisplayName() + "/builds");

        File[] archivedProjects = archivedProjectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);

        //Collection<File> allBuilds = new ArrayList<File>();

        List<String> allBuilds = new LinkedList<String>();

        for (File p:archivedProjects) {
            allBuilds.add(p.getName());
        }
        Collections.reverse(allBuilds);
        return allBuilds;
    }

    public ArchiveWidget getArchiveWidget() {
        Jenkins.getInstance().checkPermission(Permission.READ);
        List<ArchiveWidget.Backups> backupsList = new ArrayList<ArchiveWidget.Backups>();
        /*
        for (int i=1; i < 5; i++) {
            ArchiveWidget.Backups backups = new ArchiveWidget.Backups();
            backups.setNumber(Integer.toString(i));
            backups.setName("The #" + Integer.toString(i) + " builds...");
            backups.setDuration(Double.toString(Math.random()*i));
            backupsList.add(backups);
        }*/

        final File archivedProjectsDir = new File(Jenkins.getInstance().getRootDir(), "backup/" + project.getDisplayName() + "/builds");
        File[] archivedProjects = archivedProjectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);

        List<Integer> allBuilds = new LinkedList<Integer>();
        for (File p:archivedProjects) {
            allBuilds.add(Integer.parseInt(p.getName()));
        }
        Collections.sort(allBuilds);
        Collections.reverse(allBuilds);

        for (Integer b:allBuilds) {
            ArchiveWidget.Backups backups = new ArchiveWidget.Backups();
            backups.setNumber(Integer.toString(b));
            File buildXml = new File(archivedProjectsDir, Integer.toString(b) + "/build.xml");
            if (! buildXml.exists()) {
                System.exit(1);
            }

            SAXReader reader = new SAXReader();
            try {
                Document document = reader.read(buildXml);
                Element root = document.getRootElement();
                try {
                    Element userId = root.element("actions").element("hudson.model.CauseAction").element("causes").element("hudson.model.Cause_-UserIdCause").element("userId");
                    backups.setUser(userId.getText());
                } catch (NullPointerException e) {
                    backups.setUser("Timer");
                }
                Element startTime = root.element("startTime");
                String startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(startTime.getText())));
                backups.setDate(startDate);
                Element result = root.element("result");
                backups.setResult(result.getText());

            } catch (DocumentException e) {
                backups.setUser("anonymous");
                backups.setDate("1900-01-01");
                backups.setResult("N/A");
                //e.printStackTrace();
            }
            backupsList.add(backups);
        }
        ArchiveWidget archiveWidget = new ArchiveWidget();
        archiveWidget.setBackupsList(backupsList);
        return archiveWidget;
    }

    @JavaScriptMethod
    public Map GetParameters(String build_number){
        final File archivedProjectsDir = new File(Jenkins.getInstance().getRootDir(), "backup/" + project.getDisplayName() + "/builds");
        File buildXml = new File(archivedProjectsDir, build_number + "/build.xml");
        Map parameter_map = new HashMap();
        try {
            if (buildXml.exists()) {
                System.out.println(build_number);
                XML build_xml_obj = new XMLDocument(buildXml);
                int loop_number = 0;
                String map_key="";
                for (String text: build_xml_obj.xpath("/build/actions/hudson.model.ParametersAction/parameters/*/name/text() | /build/actions/hudson.model.ParametersAction/parameters/*/value/text()")){
                    loop_number++;
                    if ( loop_number%2 == 1) {
                        map_key = text;
                    } else {
                        parameter_map.put(map_key, text);
                    }
                }
            }
        } catch (Exception e) {
            return parameter_map;
        }
        return parameter_map;
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
