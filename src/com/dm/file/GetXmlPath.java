/**
 * @author		王巍
 * @date		2010-3-25
 * TODO
 * @modify		@name 		@date
 */
package com.dm.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author 李鹏
 *
 */
public class GetXmlPath {

	public static FileWriter fw;
	public static InputStreamReader stdin;
	public static BufferedReader bf;
	
	static boolean isLinuxOs;
	static char filePathSeprator;
	
	static ArrayList<String> xmlPathList = new ArrayList<String>();
	/**
	 * @param args
	 * @throws IOException 
	 */
	
	/*2011-3-8 添加新接口
	 * */
	public static ArrayList<String> getxmlpath(String xmlFilePath) throws IOException {
		isLinuxOs = System.getProperty("os.name").equals("Linux");
		filePathSeprator = isLinuxOs ? '/' : '\\';
		//String file = getStdin();
		//String file="TEMP_XML_Test.ini";
		//fw = new FileWriter(file);
		list(xmlFilePath);
		//fw.close();
		//stdin.close();
		//bf.close();
		return xmlPathList;
	}

	
	private static void list(String filePath) throws IOException {
		File file = new File(filePath);
		String[] filelist;
		

		if (file.isDirectory() && filePath.indexOf(".svn") == -1) {
			filelist = file.list();
			for (int i = 0; i < filelist.length; i++) {
				File listfile = new File(filePath + filePathSeprator + filelist[i]);
				if (!listfile.isDirectory()) {
					if (listfile.getName().toUpperCase().endsWith(".XML")) {
						//fw.write(filePath + filePathSeprator + listfile.getName()+"\r\n");
						//2011-3-10  添加文件地址链表输出
						xmlPathList.add(filePath + filePathSeprator + listfile.getName());
					}
				} else if (listfile.isDirectory()
						&& listfile.getName().indexOf(".svn") == -1)
					//fw.write("#<dir>" + filePath + filePathSeprator+ listfile.getName()+"\r\n");
				list(listfile.getAbsolutePath());
			}
			//fw.write("\r\n");
		}
	}
}
