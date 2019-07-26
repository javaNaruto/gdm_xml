/**
 * @author		��Ρ
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.file;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.dm.main.AutoTest;

/**
 * @author ��Ρ
 *
 */
public class ConfigInfo {
	private static String driver;			//JDBC��������
	private static String threads;			//���ö��ٸ��߳�
	private static String infolevel;		//��Ϣ��ʾ����
	private static String server;			//��������ַ
	private static String url;
	private static String[] otherServer;
	private static String random;         //�Ƿ����������������ʱ����loopֵ��Ϊ����ѭ�����У�
	private static String loop;           //�Ƿ�����ѭ������
	private static String filepath;       //Ĭ�ϲ�������·��
	
	private static String port;			  //�������˿�
	
	private  static String dmServerPluginStartPort;   //���������dmserver�˿�
	private static String timeingTime;				  //����������Ӧɱ��������
	private static String dmServerPluginStopPort;     //�������ɱdmserver�˿�
	private static String reStartTime;				  //�������β��ɹ�������initdb
	private static String dmInitPluginStartPort;      //���������initdb�˿�
	
	private static String[] otherPort;
	
	private static String database;
	private static String schema;
	private static String uid;
	private static String pwd;
	
	private static String serverPath;		//��������������Ŀ¼
	
	// dom4j��ز���
	private static SAXReader reader;
	private static Document doc;
	private static FileReader fr;
	private static Element root;
	private static FileWriter fw;
	
	public ConfigInfo(){
		try {
			fw = new FileWriter("error.log",true);
		} catch (IOException e2) {
//			e2.printStackTrace();
		}
		try {
			fr = new FileReader("config.xml");
		} catch (FileNotFoundException e) {
			AutoTest.elog.error("�򿪳��������ļ�����");
		}
		
		reader = new SAXReader();
		try {
			doc = reader.read(fr);
		} catch (DocumentException e) {
			AutoTest.elog.error("config.xml�ļ���������");
		}
		root = doc.getRootElement();
		
		driver = root.elementText("DRIVER");
		check(driver,"DRIVER");
		url = root.elementText("URL");
		check(url,"URL");
		threads = root.elementText("THREADS");
		check(threads,"THREADS");
		server = root.elementText("SERVER");
		check(server,"SERVER");
		port = root.elementText("PORT");
		check(port,"PORT");
		dmServerPluginStartPort = root.elementText("DMSERVERPLUGINSTARTPORT");
		check(dmServerPluginStartPort,"DMSERVERPLUGINSTARTPORT");
		timeingTime = root.elementText("TIMEINGTIME");
		check(timeingTime,"TIMEINGTIME");
		dmServerPluginStopPort = root.elementText("DMSERVERPLUGINSTOPPORT");
		check(dmServerPluginStopPort,"DMSERVERPLUGINSTOPPORT");
		reStartTime = root.elementText("RESTARTTIME");
		check(reStartTime,"RESTARTTIME");
		dmInitPluginStartPort = root.elementText("DMINITPLUGINSTOPPORT");
		check(dmInitPluginStartPort,"DMINITPLUGINSTOPPORT");
		database = root.elementText("DATABASE");
		check(database,"DATABASE");
		schema = root.elementText("SCHEMA");
		check(schema,"SCHEMA");
		uid = root.elementText("UID");
		check(uid, "UID");
		pwd = root.elementText("PWD");
		check(pwd,"PWD");
		serverPath = root.elementText("SERVERPATH");
		check(serverPath,"SERVERPATH");
		infolevel = root.elementText("INFOLEVEL");
		check(serverPath,"INFOLEVEL");
		loop=root.elementText("LOOP");
		check(loop,"LOOP");
		random=root.elementText("RANDOM");
		check(random,"RANDOM");
		filepath=root.elementText("FILEPATH");
		otherServer = new String[10];
		otherPort = new String[10];
		int i=0;
		String tempServer;
		for(i=0;i<10;i++){
			tempServer = root.elementText("SERVER"+i);
			if(tempServer!=null){
				otherServer[i] = tempServer;
			}else{
				otherServer[i] = "";
			}
		}
		
		String tempPort;
		for(i=0;i<10;i++){
			tempPort = root.elementText("PORT"+i);
			if(tempPort!=null){
				otherPort[i] = tempPort;
			}else{
				otherPort[i] = "";
			}
		}
		
		try {
			fr.close();
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}

	public  void check(String str, String str2){
		if(str==null||str.equals("")){
			try {
				System.out.println(str2+"����Ϊ�գ�����test.xml�ļ�");
				fw.write(str2+"����Ϊ�գ�����test.xml�ļ�");
				fw.close();
				System.exit(1);
			} catch (IOException e) {
//				e.printStackTrace();
			}
		
			}
	}
	
	public static String getDriver() {
		return driver;
	}

	public static String getInfolevel(){
		return infolevel;
	}	
    public boolean getRandom(){
    	if (Integer.parseInt(random)==0)
    	    return false;
    	else
    		return true;
    }
    public  boolean getLoop(){
    	if (Integer.parseInt(loop)==0)
    	    return false;
    	else
    		return true;
    }
    public String getFilePath(){
    	return filepath.trim();
    }
	public  int getThreads() {
		//2011-3-11 �޸ķ���ֵ����
		return Integer.parseInt(threads);
	}

	

	public static String getServer() {
		return server;
	}

	public static String[] getOtherServer() {
		return otherServer;
	}

	public static String getPort() {
		return port;
	}

	public static String[] getOtherPort() {
		return otherPort;
	}

	public static String getDatabase() {
		return database;
	}

	public static String getSchema() {
		return schema;
	}

	public static String getUid() {
		return uid;
	}

	public static String getPwd() {
		return pwd;
	}

	public static String getServerPath() {
		return serverPath;
	}
	
	public static String getURL()
	{
		return url;
	}

	public  static String getDmServerPluginStartPort() {
		return dmServerPluginStartPort;
	}

	public static void setDmServerPluginStartPort(String dmServerPluginStartPort) {
		ConfigInfo.dmServerPluginStartPort = dmServerPluginStartPort;
	}

	public static String getTimeingTime() {
		return timeingTime;
	}

	public  void setTimeingTime(String timeingTime) {
		ConfigInfo.timeingTime = timeingTime;
	}
	
	public  String getDmServerPluginStopPort() {
		return dmServerPluginStopPort;
	}

	public  void setDmServerPluginStopPort(String dmServerPluginStopPort) {
		ConfigInfo.dmServerPluginStopPort = dmServerPluginStopPort;
	}

	public  String getReStartTime() {
		return reStartTime;
	}

	public  void setReStartTime(String reStartTime) {
		ConfigInfo.reStartTime = reStartTime;
	}
	
	public  String getDmInitPluginStartPort() {
		return dmInitPluginStartPort;
	}

	public  void setDmInitPluginStartPort(String dmInitPluginStartPort) {
		ConfigInfo.dmInitPluginStartPort = dmInitPluginStartPort;
	}
}
