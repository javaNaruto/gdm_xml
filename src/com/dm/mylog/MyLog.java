/**
 * @author		王巍
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.mylog;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dm.main.AutoTest;

/**
 * @author 王巍
 *
 */
public class MyLog {
	private String filePath;
	private FileWriter fw;
	private Date date;
	private SimpleDateFormat dateFormat;
	
	public MyLog(String filePath){
		this.filePath = filePath;
		try {
			fw = new FileWriter(filePath,true);
		} catch (IOException e) {
			AutoTest.elog.error("无法打开文件"+filePath);
		}
	}

	public void error(String msg){
		// 测试用
//		if(msg==null){
//			System.out.println("ss");
//		}
		try {
			fw.write(msg+"\r\n");
			fw.flush();
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
	
	public void errorConn(String msg, String curServer, String curPort,
			String curDatabase, String curUid, String curPwd) {
		error(msg + "\r\n" + "server:" + curServer + "\tport:" + curPort
				+ "\tdatabase:" + curDatabase + "\tuid:" + curUid + "\tpwd:"
				+ curPwd);
	}
	
	public void errorWithTime(String msg){
		date = new Date();
		dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE HH:mm:ss");
		error("\t"+dateFormat.format(date)+"\r\n\t"+msg);
	}
	
	
	public void close(){
		try {
			fw.close();
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
	
	public String getFilePath(){
		return this.filePath;
	}
	
	
}
