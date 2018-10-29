/**
 * Copyright (C), 2015-2018, XXX有限公司
 * FileName: ProjectDelteListener
 * Author:   Administrator
 * Date:     2018/10/29 0029 14:35
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package org.jenkinsci.plugins.ArchiveProject;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ArchiveProject.util.Util;

import java.io.File;

import static org.jenkinsci.plugins.ArchiveProject.consts.Const.BACKUP_ROOT_DIR_NAME;

/**
 * 〈〉
 *
 * @author Administrator
 * @create 2018/10/29 0029
 * @since 1.0.0
 */
@Extension
public class ProjectDelteListener extends ItemListener {

	@Override
	public void onDeleted(Item item) {
		if (item instanceof AbstractProject) {
			AbstractProject p = (AbstractProject) item;
			File backupProject = new File(Jenkins.getInstance().getRootDir(), BACKUP_ROOT_DIR_NAME+"/" + p.getDisplayName());
			if (backupProject!=null && backupProject.exists()){
				Util.deleteFile(backupProject);
			}
		}
	}



}