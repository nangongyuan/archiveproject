/**
 * Copyright (C), 2015-2018, XXX有限公司
 * FileName: Util
 * Author:   Administrator
 * Date:     2018/10/29 0029 14:50
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package org.jenkinsci.plugins.ArchiveProject.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jenkinsci.plugins.ArchiveProject.bean.Backup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 〈〉
 *
 * @author Administrator
 * @create 2018/10/29 0029
 * @since 1.0.0
 */
public class Util {

	public static boolean deleteFile(File dirFile) {
		// 如果dir对应的文件不存在，则退出
		if (!dirFile.exists()) {
			return false;
		}

		if (dirFile.isFile()) {
			return dirFile.delete();
		} else {

			for (File file : dirFile.listFiles()) {
				deleteFile(file);
			}
		}

		return dirFile.delete();
	}


	/**
	 * @Description: 解析xml
	 * @Param:
	 * @return:
	 * @Author: yuan
	 * @Date: 2018/10/28 0028
	 */
	public static Backup parseXml(File buildXml){
		Backup backup = new Backup();
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(buildXml);
			Element root = document.getRootElement();
			try {
				Element userId = root.element("actions").element("hudson.model.CauseAction").element("causes").element("hudson.model.Cause_-UserIdCause").element("userId");
				backup.setUser(userId.getText());
			} catch (NullPointerException e) {
				backup.setUser("anonymous");
			}
			backup.setNumber(root.element("number").getTextTrim());
			Element startTime = root.element("startTime");
			String startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong(startTime.getText())));
			backup.setDate(startDate);
			Element result = root.element("result");
			backup.setResult(result.getText());
			backup.setDuration(Integer.parseInt(root.element("duration").getTextTrim())/1000);
			backup.setWorkspace(root.elementText("workspace"));
			backup.setFileName(buildXml.getParentFile().getName());
		} catch (DocumentException e) {
			backup.setUser("anonymous");
			backup.setDate("1900-01-01");
			backup.setResult("N/A");
		}
		return backup;
	}
}