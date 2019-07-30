package com.dm.main;


import java.sql.*;
import java.util.ArrayList;
import com.dm.common.InetOperator;
import com.dm.connect.ConnectionContext;
import com.dm.db.MyConnManager;
import com.dm.mylog.ErrorLog;
import com.dm.mylog.ExceptionLog;
import com.dm.mylog.MyLog;
import com.dm.file.ConfigInfo;
import com.gdm.driver.GdmConnection;

/**
 * @author ����
 *
 */
public class AutoTest {

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
	public static ArrayList<String> xmlPathList;					// ��������������ļ�·���б�
	public static Integer fileIndex;								// ����������ļ��б�����
	public static Integer fileListLen;								// �ļ��б���
	
	public static MyConnManager connManager;
	
	public static ErrorLog elog;
	public static ExceptionLog exlog;
	public static MyLog mlog;

	public static void init1() {
		 GdmConnection conn = ConnectionContext.getConnection("usertable");

		// TODO Auto-generated method stub
		elog = new ErrorLog();
		mlog = new MyLog("TestLog.log");
		exlog = new ExceptionLog("Exception.log");
		xmlPathList=AutoTestConsole.xmlPathList;
		fileIndex = 0;
		fileListLen = xmlPathList.size();
//		try {
//			Class.forName(ConfigInfo.getDriver());
//		} catch (ClassNotFoundException e) {
//			elog.error("�޷��ҵ����ʵ�JDBC����");
//		}
//		try {
//			connManager = new MyConnManager("jdbc:mysql://" + ConfigInfo.getServer()
//					+ ":" + ConfigInfo.getPort() + "/" + ConfigInfo.getDatabase(),
//					ConfigInfo.getUid(), ConfigInfo.getPwd());
//		} catch (SQLException e) {
//			//2010-12-22-YJM�ȴ�����������
//			boolean flag = false;
//			if(e.getMessage().equals("����ͨ���쳣")) {
//				while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
//					InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
//					try {
//						flag = true;
//						Thread.sleep(4000);
//					} catch (InterruptedException e1) {
//						e1.printStackTrace();
//					}
//				}
//				if(flag) {
//					try {
//						connManager = new MyConnManager("jdbc:mysql://" + ConfigInfo.getServer()
//								+ ":" + ConfigInfo.getPort() + "/" + ConfigInfo.getDatabase(),
//								ConfigInfo.getUid(), ConfigInfo.getPwd());
//					} catch (SQLException e1) {
//						e1.printStackTrace();
//					}
//				}
//			}
//			//2010-12-22-YJM�ȴ�����������
//			elog.error(e.getMessage());
//		}
//		try {
//			connManager.getConnection("jdbc:mysql://" + ConfigInfo.getServer()
//									+ ":" + ConfigInfo.getPort() + "/" + ConfigInfo.getDatabase(),
//									ConfigInfo.getUid(), ConfigInfo.getPwd()).close();
//		} catch (SQLException e) {
//
//		} catch (InterruptedException e) {
//
//		}
	}
}
