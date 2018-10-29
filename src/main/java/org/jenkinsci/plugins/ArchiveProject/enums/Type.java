/**
 * Copyright (C), 2015-2018, XXX有限公司
 * FileName: Type
 * Author:   Administrator
 * Date:     2018/10/28 0028 11:20
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package org.jenkinsci.plugins.ArchiveProject.enums;



/**
 * 〈〉
 *
 * @author Administrator
 * @create 2018/10/28 0028
 * @since 1.0.0
 */
public enum  Type {
	BooleanParameterValue, StringParameterValue, PasswordParameterValue, TextParameterValue, NOVALUE;
	public static Type toType(String string) {
		try{
			return valueOf(string);
		} catch (Exception e) {
			return NOVALUE;
		}
	}
}