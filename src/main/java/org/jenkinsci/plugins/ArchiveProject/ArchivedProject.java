package org.jenkinsci.plugins.ArchiveProject;

import com.jcabi.xml.XMLDocument;
import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.model.TransientActionFactory;
import net.sf.json.JSONObject;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Queue;

import com.jcabi.xml.XML;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArchivedProject implements Action {

    private Project project;

    private transient Run<?, ?> build;

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

    public Run<?, ?> getBuild() {
        return build;
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
            if (Files.isSymbolicLink(p.toPath())) {
                // System.out.println(p.getName());
                try {
                    Integer.parseInt(p.getName());
                } catch (NumberFormatException ex) {
                    continue;
                }
                allBuilds.add(Integer.parseInt(p.getName()));
            }
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
    public List<HashMap<String, String>> GetParameters(String build_number){
        final File archivedProjectsDir = new File(Jenkins.getInstance().getRootDir(), "backup/" + project.getDisplayName() + "/builds");
        File buildXml = new File(archivedProjectsDir, build_number + "/build.xml");
        // Map parameter_map = new HashMap();
        // List<HashMap> parameterList = null;
        ArrayList<HashMap<String, String>> parameterList = new ArrayList<HashMap<String, String>>();
        try {
            if (buildXml.exists()) {
                XML build_xml_obj = new XMLDocument(buildXml);
                for (XML paramList: build_xml_obj.nodes("//parameters/*")){
                    HashMap<String, String> buildParameter = new HashMap<String, String>();
                    buildParameter.put("type", paramList.node().getNodeName());
                    NodeList nodeList = paramList.node().getChildNodes();
                    for (int i=0; i<nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        if (node.hasChildNodes()) {
                            buildParameter.put(node.getNodeName(), node.getFirstChild().getNodeValue());
                        }
                    }
                    parameterList.add(buildParameter);
                }
            }
        } catch (Exception e) {
            return parameterList;
        }
        System.out.println(parameterList);
        return parameterList;
    }

    public void doBuildSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException {
        Job project = this.project;
        if (project == null) {
            return;
        }
        project.checkPermission(Item.BUILD);

        System.out.println("Rebuild:");
        System.out.println(isRebuildAvailable());

        if (isRebuildAvailable()) {
            if (!req.getMethod().equals("POST")) {
                req.getView(this, "index.jelly").forward(req, rsp);
                return;
            }
            build = req.findAncestorObject(Run.class);
            //ParametersDefinitionProperty paramDefProp = build.getParent().getProperty(ParametersDefinitionProperty.class);
            JSONObject formData = req.getSubmittedForm();
            System.out.println(formData);
        }
        BooleanParameterDefinition booleanParameterDefinition = new BooleanParameterDefinition("bool", false, "");
        ParameterValue parameterValue = booleanParameterDefinition.createValue("true");
        System.out.println("My bool parametervalue:");
        System.out.println(parameterValue);
        rsp.sendRedirect("../");
    }

    public boolean isRebuildAvailable() {
        Job project = this.project;
        return project != null
                && project.hasPermission(Item.BUILD)
                && project.isBuildable()
                && project instanceof hudson.model.Queue.Task;
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
