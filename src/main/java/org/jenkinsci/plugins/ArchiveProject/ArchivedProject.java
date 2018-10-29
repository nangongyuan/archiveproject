package org.jenkinsci.plugins.ArchiveProject;

import com.jcabi.xml.XMLDocument;
import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.model.TransientActionFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.jcabi.xml.XML;
import org.jenkinsci.plugins.ArchiveProject.bean.Backup;
import org.jenkinsci.plugins.ArchiveProject.enums.Type;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.jenkinsci.plugins.ArchiveProject.consts.Const.*;
import static org.jenkinsci.plugins.ArchiveProject.util.Util.parseXml;

public class ArchivedProject implements Action {

    private AbstractProject project;

    public ArchivedProject(AbstractProject project) {
        this.project = project;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return DOCUMENT_IOC;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return ARCHIVED_PROJECT_DISPLAY_NAME;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return ARCHIVED_PROJECT_URL_NAME;
    }



    /**
    * @Description: 获取备份列表
    * @Param:
    * @return:
    * @Author: yuan
    * @Date: 2018/10/28 0028
    */
    public List<Backup> getBackupList() {
        Jenkins.getInstance().checkPermission(Permission.READ);
        List<Backup> backupsList = new ArrayList<Backup>();

        final File archivedProjectsDir = new File(Jenkins.getInstance().getRootDir(), BACKUP_ROOT_DIR_NAME+"/" + project.getDisplayName() + "/"+PROJECT_BUILDS_DIR_NAME);
        File[] archivedProjects = archivedProjectsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (archivedProjects==null){
            return backupsList;
        }
        for (File item:archivedProjects) {
            File buildXml = new File(item, PROJECT_BUILD_XML_NAME);
            if (!buildXml.exists()) {
                continue;
            }
            Backup backup = parseXml(buildXml);
            backupsList.add(backup);
        }
        return backupsList;
    }

    /** 
    * @Description: js调用获取参数 
    * @Param:  
    * @return:  
    * @Author: yuan
    * @Date: 2018/10/28 0028 
    */ 
    @JavaScriptMethod
    public List<Map<String, String>> GetParameters(String build_fileName){
        final File archivedProjectsDir = new File(Jenkins.getInstance().getRootDir(), BACKUP_ROOT_DIR_NAME+"/" + project.getDisplayName() + "/"+PROJECT_BUILDS_DIR_NAME);
        List<Map<String, String>> parameterList = new ArrayList<Map<String, String>>();
        if (build_fileName== null || build_fileName.equals("")){
            if (archivedProjectsDir!=null && archivedProjectsDir.listFiles().length>0){
                build_fileName = archivedProjectsDir.listFiles()[0].getName();
            }else{
                return parameterList;
            }
        }
        File buildXml = new File(archivedProjectsDir, build_fileName + "/"+PROJECT_BUILD_XML_NAME);

        try {
            if (buildXml.exists()) {
               Backup backup = parseXml(buildXml);
                Map<String, String> map = new HashMap<>();
                map.put("number", backup.getNumber());
                map.put("user",backup.getUser());
                map.put("date", backup.getDate());
                map.put("result", backup.getResult());
                map.put("duration", String.valueOf(backup.getDuration()));
                map.put("workspace", backup.getWorkspace());
                map.put("fileName", backup.getFileName());
                parameterList.add(map);
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
        return parameterList;
    }



    /**
    * @Description: 处理rebuild请求
    * @Param:
    * @return:
    * @Author: yuan
    * @Date: 2018/10/28 0028
    */
    public void doBuildSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException {
        Job project = this.project;
        if (project == null) {
            return;
        }
        project.checkPermission(Item.BUILD);
        if (isRebuildAvailable()) {
            if (!req.getMethod().equals("POST")) {
                req.getView(this, "index.jelly").forward(req, rsp);
                return;
            }
            JSONObject formData = req.getSubmittedForm();
            List<ParameterValue> values = new ArrayList<ParameterValue>();
            if (!formData.isEmpty()) {
                JSONArray a = JSONArray.fromObject(formData.get("parameter"));
                for (Object o : a) {
                    JSONObject jo = (JSONObject)o;
                    //判断参数类型
                    switch (Type.valueOf(jo.getString("type").split("\\.")[2])) {
                        case BooleanParameterValue:
                            BooleanParameterDefinition booleanParameterDefinition = new BooleanParameterDefinition(jo.getString("name"), false, "");
                            ParameterValue parameterBoolValue = booleanParameterDefinition.createValue(jo.getString("value"));
                            values.add(parameterBoolValue);
                            break;
                        case StringParameterValue:
                            StringParameterDefinition stringParameterDefinition = new StringParameterDefinition(jo.getString("name"), "", "");
                            ParameterValue parameterStringValue = stringParameterDefinition.createValue(jo.getString("value"));
                            values.add(parameterStringValue);
                            break;
                        case PasswordParameterValue:
                            PasswordParameterDefinition passwordParameterDefinition = new PasswordParameterDefinition(jo.getString("name"), "", "");
                            ParameterValue parameterPasswordValue = passwordParameterDefinition.createValue(jo.getString("value"));
                            values.add(parameterPasswordValue);
                            break;
                        case TextParameterValue:
                            TextParameterDefinition textParameterDefinition = new TextParameterDefinition(jo.getString("name"), "", "");
                            ParameterValue parameterTextValue = textParameterDefinition.createValue(jo.getString("value"));
                            values.add(parameterTextValue);
                            break;
                        case FileParameterValue:
                            FileParameterDefinition fileParameterDefinition = new FileParameterDefinition(jo.getString("name"), "");
                            ParameterValue parameterFileValue = fileParameterDefinition.createValue(req);
                            values.add(parameterFileValue);
                            break;
                        case NOVALUE:
                            StringParameterDefinition stringParameterDefinition1 = new StringParameterDefinition(jo.getString("name"), "", "");
                            ParameterValue parameterString1Value = stringParameterDefinition1.createValue(jo.getString("value"));
                            values.add(parameterString1Value);
                            break;
                    }
                }
            }
            List<Action> actions = new ArrayList<Action>();
            ParametersAction paramAction = new ParametersAction(values);
            if (paramAction != null) {
                actions.add(paramAction);
            }
            ((AbstractProject) project).scheduleBuild2(0, null, actions);
            rsp.sendRedirect("../");
        }
    }



    private boolean isRebuildAvailable() {
        Job project = this.project;
        return project != null
                && project.hasPermission(Item.BUILD)
                && project.isBuildable()
                && project instanceof hudson.model.Queue.Task;
    }

    @Extension
    public static class ArchivedProjectFactory extends TransientActionFactory<AbstractProject> {

        @Override
        public Class<AbstractProject> type() {
            return AbstractProject.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull AbstractProject project) {
            return Collections.singleton(new ArchivedProject(project));
        }
    }
}
