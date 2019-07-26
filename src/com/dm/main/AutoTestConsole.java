
package com.dm.main;
import java.util.ArrayList;
import java.io.IOException;
import com.dm.db.MyConnManager;
import com.dm.file.ConfigInfo;
import com.dm.file.GetXmlPath;
import com.dm.mylog.ErrorLog;
import com.dm.mylog.ExceptionLog;
import com.dm.mylog.MyLog;
import com.dm.xml.XmlExecutor;

/**
 * @author 李鹏
 *
 */
public class AutoTestConsole {

	public static String driver;
	public static String testCaseFile;
	public static String logFile;
	public static String exlogFile;
	public static int threads;
	public static boolean usePool;
	public static int initConnect;
	public static int maxConnect;
	public static String server;
	public static String[] otherServer;
	public static String port;
	public static String[] otherPort;
	public static String database;
	public static String schame;
	public static String uid;
	public static String pwd;
	
	public static String iniName;
	public static ArrayList<String> xmlPathList=new ArrayList<String>();// 待处理测试用例文件路径列表
	public static Integer fileIndex;								// 互斥变量，文件列表索引
	public static Integer fileListLen;								// 文件列表长度
	
	public static MyConnManager connManager;
	
	public static ErrorLog elog;
	public static ExceptionLog exlog;
	public static MyLog mlog;
	public static ConfigInfo config = new ConfigInfo();
	public static String fileSQL;
	
	public static String curFilePath;							// 当前测例绝对路径
	public static String[][] threads_count=new String[config.getThreads()][2];
	
	public static void main(String[] args){
		init();
		if(args.length ==0)
		{
			String[] filePath= config.getFilePath().split(" ");
			for(int i=0;i<filePath.length;i++){
				if(filePath[i].endsWith(".xml")||filePath[i].endsWith(".XML"))
				{
					xmlPathList.add(filePath[i]);
				}
				else
				{
					try {
						xmlPathList.addAll(GetXmlPath.getxmlpath(filePath[i]));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
				
		}
		else 
		{
			int filenum=args.length;
			for(int i=0;i<filenum;i++)
			{
				if(args[i].endsWith(".xml")||args[i].endsWith(".XML"))
				{
					xmlPathList.add(args[i]);
				}
				else
				{
					try {
						xmlPathList.addAll(GetXmlPath.getxmlpath(args[i]));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		//2011-3-29 增加xmlPathList为空处理
		if(xmlPathList.size()==0){
			System.out.println("没有可运行的XML文件");
			}else{
		AutoTest.init1();
		AutoTest.mlog.errorWithTime("\r");
		AutoTest.exlog.errorWithTime("\r");
		int i=0;
		Thread threads[]=new Thread[config.getThreads()];
		if(config.getRandom()){
			while(true){
				try {
					Thread.currentThread().sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(threads_count[i][1]=="done"){
					threads_count[i][1]="running";
					int randomfileindex=(int)(Math.random()*(AutoTest.fileListLen));
					curFilePath = AutoTest.xmlPathList.get(randomfileindex);
					threads[i]=new Thread(new XmlExecutor(curFilePath,i));
					threads[i].start();
					System.out.println("\r\n--********************\r\n--execute ："+curFilePath);
					}
				
				i++;
				if(i>=config.getThreads()){
					i=0;
					}
				}	
			}
		else{
			if(config.getLoop()){
				while(true){
				    if(AutoTest.fileIndex<AutoTest.fileListLen){
				    curFilePath = AutoTest.xmlPathList.get(AutoTest.fileIndex);
				    if(threads_count[i][1]=="done"){
				    	threads_count[i][1]="running";
				    	threads[i]=new Thread(new XmlExecutor(curFilePath,i));
						threads[i].start();
					    System.out.println("\r\n--********************\r\n--execute ："+curFilePath);
					    AutoTest.fileIndex++;
					    }
				    try {
						Thread.currentThread().sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    i++;
				    if(i>=config.getThreads()){
				    	i=0;
				    	}
				    }
				    if(AutoTest.fileIndex>AutoTest.fileListLen)
				    	AutoTest.fileIndex=0;
				    }
				
			}else{
				while(true){
					if(threads_count[i][1]=="done"){
						if(AutoTest.fileIndex<AutoTest.fileListLen){
							curFilePath = AutoTest.xmlPathList.get(AutoTest.fileIndex);
							threads_count[i][1]="running";
							threads[i]=new Thread(new XmlExecutor(curFilePath,i));
							threads[i].start();
							System.out.println("\r\n--********************\r\n--execute ："+curFilePath);
						    AutoTest.fileIndex++;
						    }
							
					}
					try {
						Thread.currentThread().sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					i++;
					if(i>=config.getThreads()){
						i=0;
						}
				    if(AutoTest.fileIndex>=AutoTest.fileListLen){ 
				    	for(int j=0;j<config.getThreads();j++){
				    		try {
								threads[j].join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				    	}
				    	break;
				    	} 
				    }
				}
			}
		AutoTest.mlog.close();
		AutoTest.exlog.close();
			}
		}
	
	private static  void init() {
		// 初始化线程调度数组
		for(int i=0;i<config.getThreads();i++){
			threads_count[i][0]=String.valueOf(i);
			threads_count[i][1]="done";
		}
	}

	
	
}
