/**
 * @author		王巍
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.mylog;

import java.io.FileWriter;
import java.io.IOException;

/**
 * @author 王巍
 *
 */
public class ErrorLog {
	private FileWriter fw;
	public ErrorLog(){
		try {
			fw =new FileWriter("error.log",true);
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
	public void error(String msg){
		try {
			fw.append(msg+"\r\n");
			fw.close();
			System.out.println("程序异常退出");
			System.exit(1);
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
	
}
