/**
 * Copyright (C), 2015-2018, XXX有限公司
 * FileName: Backup
 * Author:   Administrator
 * Date:     2018/10/27 0027 23:11
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package org.jenkinsci.plugins.ArchiveProject;

/**
 * 〈〉
 *
 * @author Administrator
 * @create 2018/10/27 0027
 * @since 1.0.0
 */
public class Backup {
	private String number;

	private String user;

	private String date;

	private String result;

	private String fileName;

	private Integer duration;

	private String workspace;

	public Backup(String number, String user, String date, String result, String fileName, Integer duration, String workspace) {
		this.number = number;
		this.user = user;
		this.date = date;
		this.result = result;
		this.fileName = fileName;
		this.duration = duration;
		this.workspace = workspace;
	}

	public Backup() {
		this.number = "0";
		this.user = "anonymous";
		this.date = "1900-01-01";
		this.result = "N/A";
	}

	public String getNumber() {
		return number;
	}

	public String getUser() {
		return user;
	}

	public String getDate() {
		return date;
	}

	public String getResult() {
		return result;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}
}