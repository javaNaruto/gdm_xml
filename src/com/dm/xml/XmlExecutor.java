package com.dm.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.dm.connect.ConnectionContext;
import com.gdm.driver.GdmConnection;
import com.gdm.driver.GdmException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
//import org.eclipse.swt.widgets.Display;

import com.dm.common.InetOperator;
import com.dm.common.StreamGobbler;
import com.dm.db.DbOperator;
import com.dm.db.MyConnection;
import com.dm.db.SqlResult;
import com.dm.file.ConfigInfo;
import com.dm.main.AutoTest;
import com.dm.main.AutoTestConsole;
import com.dm.mylog.ExceptionLog;
import com.dm.mylog.MyLog;

import java.applet.*;
/**
 * @author 李鹏
 *
 */
public class XmlExecutor extends Applet implements Runnable {
	private static final long serialVersionUID = 1L;
	public HashMap<String, String> mapForChange;		// 保存@字符串，做替换
	public String curFilePath;							// 当前测例绝对路径
	protected final int TRANS_COUNT=8;                  //目前支持TRANS数为8，即最大TRANS7
	protected final int TEST_POINT_NUM=30;              //目前支持最大测试点数为28
	/**********************************************************************/
	/* <TYPE>类型
	 * 每条SQL语句都绑定有一个TYPE类型
	 * 无<TYPE>节点，按默认方式执行										*/
	protected static final int DIRECT_EXECUTE_IGNORE =1;								// 不关心执行结果
	protected static final int DIRECT_EXECUTE_SUCCESS = 2;							// 预期执行成功
	protected static final int DIRECT_EXECUTE_FAIL = 3;								// 预期执行失败
	protected static final int DIRECT_EXECUTE_SELECT_WITH_RESULT = 4;					// 只比较结果集列
	protected static final int DIRECT_EXECUTE_SELECT_COMPARE_RESULT = 5;				// 比较结果集，按XML文件中的值为准
	protected static final int DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL = 6;			// 比较结果集，必须完全相等
	protected static final int LOGIN_SUCCESS = 7;
	protected static final int LOGIN_FAIL = 8;
	protected int curType = DIRECT_EXECUTE_SUCCESS;

	/*
	 * 2010-11-19-YJM
	 * 对于DISCONNECT节点进行调整，如果text的值为-999999999，则释放所有连接，否则释放最近的SETCONNECTID值的连接
	 */
	protected int CURCONNID = -999999999;				//此处预期不会有人使用-999999999做为连接数
	/**********************************************************************/
	/**
	 * DOM4J相关参数
	 */
	private SAXReader reader;
	private Document doc;
	private FileReader fr;
	private Element root;
	/**********************************************************************/
	protected String curKeyWord;
	// 程序支持的关键字 不在以下则表示新建一个变量
	protected String keyWords = "|SQL|"+"SQL_CASE|"+ "TESTPOINTBEGIN|" + "TYPE|" + "RESULT|"
			+ "TEMPSTR|" + "BEGINTRANS|" + "ENDTRANS|" + "LOOP|" + "TIMES|"
			+ "STARTTIMES|" + "NEWCONNECTEXECUTE|" + "RECONNECT|" + "SERVER|"
			+ "UID|" + "DATABASE|" + "PWD|" + "PROVIDER|" + "POOL|" + "PORT|"
			+ "EXEPROCESS|" + "EXEPROCESSEX|" + "NEWTRANS|" + "EFFECTROWS|"
			+ "MORETHREAD|" + "THREADS|" + "TOGETHER|" + "NOSHOW|"
			+ "SQLSTATE|" + "NERROR|" + "CASEEXPRESULT|" + "COMPARERESULT|"
			+ "SERVERCMD|" + "RUNSERVER|" + "REBOOT|" + "SETDMINI|"
			+ "GETDMINI|" + "COPYFILE|" + "DELFILE|" + "FILESIZE|"
			+ "CREATEFILE|" + "SETVAL|" + "GETVAL|" + "IF|" + "ELSE|" + "FMES|"
			+ "SMES|" + "INITDB|" + "CONTENT|" + "EXEXML|" + "CONNECT|"
			+ "SETCONNECTID|" + "SQLTEST|" + "CLEAR|" + "SLEEP|" + "IGNORE|"
			+ "BREAK|" + "DISCONNECT|" + "WINDOWS|" + "LINUX|" + "RECORD|"
			+ "COLUMN|" + "PARAMETER|" + "CLEARPARAMETERS|" + "OPEN|"
			+ "FETCHNEXT|" + "TIMETICKS|" + "ENTER|" + "EXIT|"
			+ "SQLSTR|" + "SQLSTR1|" + "SQLSTR2|" + "LEVEL|"
			+ "TestPointBegin|" + "RESULTROWS|"
			+ "TRANS0|" + "TRANS1|"+ "TRANS2|"+ "TRANS3|"+ "TRANS4|"+ "TRANS5|"+ "TRANS6|"+"TRANS7|"
			+ "CALC_TIMETICKS|" + "TOTALTIME|"+ "MAXTIME|"+ "MINTIME|"+ "AVGTIME|"+"PSQL|";

	/**********************************************************************/
	/**
	 * 数据库操作对象
	 */
	protected DbOperator dbOperator;
	protected GdmConnection myConn;
	protected String curURL;
	protected String curServer;
	protected String curPort;
	protected String curDatabase;
	protected String curUid;
	protected String curPwd;
	private SqlResult sqlResult;
	private int effectRows;
	/*
	 * 2010-11-19-YJM
	 * DISCONNECT节点中没有值时，或许最后一个SETCONNECTID的值
	 */
	protected int curConnId = CURCONNID;
	/**********************************************************************/
	// 单个测例内存储<CONNECT>信息，执行完后释放所有连接
	protected HashMap<Integer, MyConnection> myConnMap;
	protected int myConnMapSize;
	protected int connectId;
	// 其他数据库连接信息，最后也释放
	protected ArrayList<MyConnection> otherConnList;
	protected int loopCount = 0;				// LOOP循环层数
	protected int[] times=new int[5];			// 每层LOOP循环最大循环数
	protected int[] timesIndex = new int[5]; 	// 每层LOOP循环，当前循环次数

	// 记录SQL_CASE和SQL节点个数，把错误定位到行
//	private int sqlCaseNum = 0;
	public int sqlNum = 0;
	public boolean isInLoop;
	public String curRawSql;					// 记录原始的SQL语句来定位错误行，因为有可能是<SQL>@SQLSTR</SQL>

	protected MyLog mlog = AutoTest.mlog;
	protected ExceptionLog exlog = AutoTest.exlog;

	protected boolean ifIsTrue = true;		// 判断是否执行ELSE

	protected boolean isNewConnectExecute = false;	// 是否是NEWCONNECTEXECUTE节点
	protected boolean isMoreThread = false;			// 作用同上
	protected int fetchIndex = -1; 				// 解决FETCHNEXT
	public String curSql;						// 记录当前SQL语句
	public boolean caseExpResult;				// 解决关键字CASEEXPRESULT
	public ArrayList<String> rubbishMap;		// 清空临时放入mapForChange里的key，防止mapForChange很大

	public boolean isBreak = false;				// 处理LOOP中的BREAK
	//处理事务相关关键字 2010-3-19
	public int transCount = -1;					// TRANS从0开始
	public ArrayList<GdmConnection> transConnList;
	public boolean isTrans = false;
	// 2010-4-7 添加
	//查看工具是否运行在Linux环境下，Linux环境下路径分隔符为"/"与Window下有些差别
	boolean isLinuxOs ;
	public char filePathSepartor;
	// 2010-4-22 添加 处理CLEAR的错误
	public boolean isClear = false;
	//2011-3-8 修改程序输出，设置检查标记。
    public  String Test_Point[][]=new String[TEST_POINT_NUM][2];
	public  int xx=0;
	int running;
	//2011-3-23 修改输出，设SQL_CASE 为一个执行单位点
	public boolean casesqlcase=true;
	public boolean isinsqlcase=false;

	//2011-3-18 重写NEWTRANS方法，创建线程数组存放TRANS线程
	Thread transthread[]=new Thread[TRANS_COUNT];
	TRANSEXecurtor trans[]=new TRANSEXecurtor[TRANS_COUNT];
	//2011-3-29 SQL耗费时间
	private double usedtime=0;

     //2011-3-11 新构造方法
	public XmlExecutor(String curFiString,int running){
		this.curFilePath = curFiString;
		this.running=running;
	}
	public XmlExecutor() {
	}
	public XmlExecutor(String curFiString) {
		// TODO Auto-generated constructor stub
		this.curFilePath = curFiString;
		init();
	}
	// 每个测例执行的开始，包括全局变量的初始话，还有XML文件解析的初始化
	public boolean ExecuteXml(String xmlFilePath){
		init();								// 测试工具配置参数初始化
		if(true == xmlinit()){				// XML解析初始化
			GdmConnection Conn1;
			try {
				Conn1 = ConnectionContext.getConnection("usertable");
				dbOperator = new DbOperator(Conn1);
			} catch (GdmException e) {
				//Message.msgs.offer(e.getMessage());
				//Display.getDefault().asyncExec(new Message());
				System.out.println("初始化申请连接失败--SERVER:"+curServer+"PORT:"+curPort+"DATABASE:"+curDatabase+"UID:"+curUid+"PASSWORD:"+curPwd);
				//mlog.errorConn("connect failed ", curServer, curPort, curDatabase, curUid, curPwd);
				exlog.error(curFilePath+"/n");
				exlog.errorConn("connect failed ", curServer, curPort, curDatabase, curUid, curPwd);
				//XmlExecutor.state = 3;
				return false;
				//System.exit(1);
			} catch (Exception e) {
				//Message.msgs.offer(e.getMessage());
				//Display.getDefault().asyncExec(new Message());
				System.out.println("初始化申请连接失败--SERVER:"+curServer+"PORT:"+curPort+"DATABASE:"+curDatabase+"UID:"+curUid+"PASSWORD:"+curPwd);
				//mlog.errorConn("connect failed", curServer, curPort, curDatabase, curUid, curPwd);
				exlog.error(curFilePath+"/n");
				exlog.errorConn("connect failed", curServer, curPort, curDatabase, curUid, curPwd);
				//XmlExecutor.state = 3;
				return false;
				//System.exit(1);
			}
			//2011-3-9 初始化检查点
			for(int i=0;i<20;i++){
				Test_Point[i][0]="";
				Test_Point[i][1]="";
			}
			xx=0;
			Test_Point[xx][0]="TESTPOINTBEFORE";
			Test_Point[xx][1]="True";
			boolean tempBool = ExecuteElement(root);

			/*
			 * 2010-11-19-YJM
			 * 在一个脚本执行完之后，释放链接之前，把所有的SQL语句都提交
			 */
			try {
				dbOperator.setConn(Conn1);
//				dbOperator.DirectExecute("commit");
				dbOperator.openAutoCommit();
			} catch (GdmException e1) {
				// TODO Auto-generated catch block
				//2010-12-22-YJM等待服务器重启
				boolean flag = false;
				if(e1.getMessage().equals("网络通信异常")) {
					while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
						InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
						try {
							flag = true;
							Thread.sleep(400);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(flag) {
						try {
							Conn1 = ConnectionContext.getConnection("usertable");
							dbOperator.setConn(Conn1);
						} catch (GdmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				//2010-12-22-YJM等待服务器重启
				e1.printStackTrace();
			}

			try {
				Conn1.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		    /*2011-3-9
		     * 打印输出日志
		     * */

			int flag=1;
			for(int i=0;i<=xx;i++){
				if(Test_Point[i][1]=="FAULT")
					flag=0;
			}
			//设置同步块，避免日志输出混乱
			synchronized(exlog){
			  if(flag==0){
				  exlog.error(curFilePath+"   FAULT");

				  for(int i=0;i<=xx;i++){
					  if(Test_Point[i][1]=="FAULT")
					  exlog.error(Test_Point[i][0]+"  FAULT");
					  }
				  }
			}
			synchronized(mlog){
			   if(flag==1){
				   mlog.error(curFilePath+"   PASS");
				   }
			}
			/**/
			mapFree();							// 释放mapForChange里的临时变量
			connFree();                         //用例的所有连接
			transFree();                        //2011-3-18  清理TRANS线程及连接
			return tempBool;
		}else{
			System.out.println("failed in xmlinit()");
			return false;
		}
	}

	public void init(){
		isLinuxOs = System.getProperties().getProperty("os.name").equals("Linux");
		filePathSepartor = isLinuxOs ? '/' : '\\';
		transConnList=new ArrayList<>();
		mapForChange = new HashMap<String, String>();
		otherConnList = new ArrayList<MyConnection>();
		mapForChange.put("@SERVER", ConfigInfo.getServer());
		int i=0;
		for(i=0;i<10;i++){
			mapForChange.put("@SERVER"+i, ConfigInfo.getOtherServer()[i]);
		}
		mapForChange.put("@PORT", ConfigInfo.getPort());
		for(i=0;i<10;i++){
			mapForChange.put("@PORT"+i, ConfigInfo.getOtherPort()[i]);
		}
		mapForChange.put("@DATABASE", ConfigInfo.getDatabase());
		mapForChange.put("@SCHEMA", ConfigInfo.getSchema());
		mapForChange.put("@UID", ConfigInfo.getUid());
		mapForChange.put("@PWD", ConfigInfo.getPwd());
		mapForChange.put("@PATH",curFilePath.substring(0, curFilePath.lastIndexOf(filePathSepartor)));
		mapForChange.put("@SERVERPATH", ConfigInfo.getServerPath());
		loopCount = 0;

		// 使用测试工具设置的连接信息初始化单个测例的连接信息
		curURL = ConfigInfo.getURL();
		curDatabase = ConfigInfo.getDatabase();
		curServer = ConfigInfo.getServer();
		curUid = ConfigInfo.getUid();
		curPort = ConfigInfo.getPort();
		curPwd = ConfigInfo.getPwd();
		// JDBCURL 变为 jdbc:dm://server:port/DATABASE

		ifIsTrue = true;
		isNewConnectExecute = false;
		isMoreThread = false;
		fetchIndex = -1;
		caseExpResult = true;
		rubbishMap = new ArrayList<String>();
		isBreak = false;

		sqlNum = 0;
		isInLoop = false;
		transCount = -1;
		myConnMap = new HashMap<Integer, MyConnection>();   //2010-3-22 添加
		isTrans = false;
		isClear = false;

	}

	public void connFree(){
		int i=0;
		int listLen = otherConnList.size();
	    myConn = myConnMap.get(i);
		while(myConn!=null){
			try {
				myConn.close();
			} catch (GdmException e) {
				// TODO Auto-generated catch block
				exlog.error(curFilePath+"释放连接失败");
				e.printStackTrace();
			}
			i++;
			myConn = myConnMap.get(i);
		}

		for(i=0;i<listLen;i++){
			try {
				otherConnList.get(i).close();
			} catch (GdmException e) {
				exlog.error(curFilePath+"释放连接失败(otherConnList)");
              	e.printStackTrace();
			}
		}
	}
	// 释放mapForChange里的临时key
	public void mapFree(){
		int i=0;
		int len = rubbishMap.size();
		for(i=0;i<len;i++){
			mapForChange.remove("@"+rubbishMap.get(i));
		}
		rubbishMap.clear();
	}

	public boolean xmlinit(){
		try {
			System.out.println("cur file:"+curFilePath);
			fr = new FileReader(curFilePath.trim());
		} catch (FileNotFoundException e) {
			//mlog.error("fail to open file："+curFilePath);

			exlog.error(curFilePath+"不能打开该文件");
//			e.printStackTrace();
			System.out.println("cur file failed 1:"+curFilePath);
			return false;
		}
		reader = new SAXReader();
		try {
			doc = reader.read(fr);
		} catch (DocumentException e) {
//			e.printStackTrace();
			//mlog.error("XML文件语法解析错误："+"\r\n\t"+e.getMessage());
			exlog.error(curFilePath+"XML文件语法解析错误："+"\r\n\t"+e.getMessage());
			return false;
		}
		root = doc.getRootElement();
		if(!(root.getName().equals("SQLTEST"))){
			//mlog.error("测试用例脚本格式错误：没有<SQLTEST>节点");
			exlog.error(curFilePath+"测试用例脚本格式错误：没有<SQLTEST>节点");
			return false;
		}
		return true;
	}
	public void transFree(){
		int i=0;
		int len;
		//设置检查防止数组溢出
		if(transCount>=TRANS_COUNT)
			transCount=TRANS_COUNT-1;
		for(i=0;i<=transCount;i++){
			trans[i].tranover=true;
			 synchronized(trans[i].WaitTrans[i]){
					trans[i].WaitTrans[i].notify();
					}
		}
		for(i=0;i<=transCount;i++)
			trans[i].destroy();
		len = transConnList.size();
		for(i=0;i<len;i++){
			try {
//				transConnList.get(i).close();
			} catch (GdmException e) {
//				e.printStackTrace();
			}
		}
	}
	// dom4j把XML的每一个节点作为一个Element
	// 这个函数是执行每个节点的开始，有可能会嵌套执行。
	@SuppressWarnings("unchecked")
	public boolean ExecuteElement(Element element) {
		ArrayList<Element> elementList = (ArrayList<Element>) element.elements();
		int i = 0, eListSize = elementList.size();
		for (i = 0; i < eListSize; i++) {
			element = elementList.get(i);
			curKeyWord = element.getName().trim();
			System.out.println("--"+curKeyWord);
			if (keyWords.indexOf("|" + curKeyWord + "|") > -1) { // 存在该关键字
				// 处理BREAK 直接跳出for循环
				if(curKeyWord.equals("BREAK")){
					isBreak = true;
					break;
				}
				Operator operator;
				Class<Operator> c;
				operator = new Operator();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				if(curKeyWord.equals("CONNECT")) {
					if(i > 3) {
						if(!elementList.get(i-3).getName().trim().equalsIgnoreCase("UID")) {
							curUid = ConfigInfo.getUid();
						}

						if(!elementList.get(i-2).getName().trim().equalsIgnoreCase("PWD")) {
							curPwd = ConfigInfo.getPwd();
						}

						if(!elementList.get(i-1).getName().trim().equalsIgnoreCase("DATABASE")) {
							curDatabase = ConfigInfo.getDatabase();
						}
					} else {
						curUid = ConfigInfo.getUid();
						curPwd = ConfigInfo.getPwd();
						curDatabase = ConfigInfo.getDatabase();
					}
				}
				// 2010-4-23 添加
				// 处理 TYPE在SQL下面的情况
				// 仅当节点结构为以下时才处理
				// <SQL>
				// <TYPE>
				// <RESULT>
				if (i < eListSize - 2) {
					if (curKeyWord.equals("SQL")) {
						if (elementList.get(i + 1) != null) {
							if (elementList.get(i + 1).getName().trim().equals(
									"TYPE")) {
								if (elementList.get(i + 2) != null) {
									if (elementList.get(i + 2).getName().trim()
											.equals("RESULT")) {
										operator.TYPE(elementList.get(i + 1));
									}
								}
							}
						}
					}
				}
				c = (Class<Operator>) operator.getClass();
				Method m;
				Object obj;
				try {
					m = c.getDeclaredMethod(curKeyWord, Element.class);
				} catch (SecurityException e) {
					//mlog.error(e.getMessage());
					Test_Point[xx][1]="FAULT";
					System.out.println(curFilePath+e.getMessage());
					//exlog.error(curFilePath+e.getMessage());
					return false;
					// e.printStackTrace();
				} catch (NoSuchMethodException e) {
					//mlog.error("函数不存在，函数名：" + curKeyWord);
					Test_Point[xx][1]="FAULT";
					System.out.println(curFilePath+"函数不存在，函数名：" + curKeyWord);
					//exlog.error(curFilePath+"函数不存在，函数名：" + curKeyWord);
					return false;
					// e.printStackTrace();
				}
				try {
					obj = m.invoke(operator, element);
					// IF节点条件为假，且下一个节点不为ELSE，就跳过下一个
					// 靠全局变量ifIsTrue来判断是否执行ELSE节点
//					if (curKeyWord.equals("IF") && ifIsTrue==false
//							&& !elementList.get(i + 1).getText().trim().equals("ELSE")) {
//						i++;
//					}

//					elementList.get(i).getText().trim() getText-->getName
					if (curKeyWord.equals("IF") && ifIsTrue == false) {
						while(i<eListSize&&!(elementList.get(i).getName().trim().equals("ELSE"))){
							i++;
						}
						i = i-1;								// for循环里又多加了一个
//						System.out.println(eListSize+"   "+i);
					}
				} catch (IllegalArgumentException e) {
					Test_Point[xx][1]="FAULT";
					//mlog.error(e.getMessage());
					// e.printStackTrace();
					return false;
				} catch (IllegalAccessException e) {
					Test_Point[xx][1]="FAULT";
					//mlog.error(e.getMessage());
					// e.printStackTrace();
					return false;
				} catch (InvocationTargetException e) {
					Test_Point[xx][1]="FAULT";
					//mlog.error(e.getMessage());
//					 e.printStackTrace();
					return false;
				}
				if(obj.equals(false)){

				}
			} else {
				// 该节点关键字不存在，还要判断是否有<EXP>节点
				String tmpStr = element.getText();
				Element expElement = element.element("EXP");
				if(expElement!=null){
					Operator tmpOperator = new Operator();
					tmpStr = String.valueOf(tmpOperator.EXP(expElement));
				}
				mapForChange.put("@"+curKeyWord, tmpStr.trim());
				rubbishMap.add(tmpStr.trim());
			}
		}
		return true;
	}




	public class Operator{

		public boolean CONTENT(Element element){
			System.out.println("\n--用例描述:"+element.getText()+"\r");
			//Message.msgs.offer("\n--用例描述:"+element.getText()+"\r");
			//Display.getDefault().asyncExec(new Message());
			return true;
		}
		public boolean CONNECT(Element element) {
//			if(isTrans==true){
//				return true;									// NEWTRANS里的连接信息已处理，不再处理
//			}
			if(isNewConnectExecute==true||isMoreThread==true||isTrans==true) // 这三种情况下的CONNECT已处理，不再处理
				return true;
			String value = element.getText().trim();
			/*
			 * 2010-11-22-YJM
			 * 增加对空串的处理
			 */
			if (!value.trim().equals("")) {							// <CONNECT>内有数字
				try {
					connectId = Integer.valueOf(value);
					curConnId = connectId;
				} catch (NumberFormatException e) {
					System.out.println("<CONNECT>节点内使用了非法字符串\r\n\t" + e.getMessage());
					//mlog.error("<CONNECT>节点内使用了非法字符串\r\n\t" + e.getMessage());
					Test_Point[xx][1]="FAULT";
					return false;
				}
				myConnMapSize = myConnMap.size();				// 当前myConnMap内已有连接数
				if (connectId > myConnMapSize-1) { 				// 表示要新建连接放入myConnMap
					return newConnect();
				}else{											// 表示要直接从myConnMap中取连接
					dbOperator.setConn(myConnMap.get(connectId));
					return true;
				}
			}else{												// 也是要新建连接
				connectId = -1;									// 表示不放入myConnMap
				// 2010-3-18 添加下面一行
				myConnMapSize = myConnMap.size();				// 当前myConnMap内已有连接数
				return newConnect();
			}
		}
		/*
		 * 2011-3-17——李鹏
		 * 为满足兼容单向性能测试用例，PROVIDER关键字不处理。即忽略连接OLEDB驱动强制使用JDBC
		   */
		public boolean PROVIDER (Element element){
			return true;
		}
		public boolean DISCONNECT(Element element){
			/*
			 * 2010-11-22-YJM
			 * 对应CONNECT，此处三个地方的DISCONNECT都不再做处理
			 */
			if(isNewConnectExecute==true||isMoreThread==true||isTrans==true) {
				return true;
			}
			if(otherConnList.size()==0&&myConnMap.size()==0){
				return true;
			}
			String value = element.getText();
			if(value!=null&&!value.trim().equals("")){
				value = value.trim();
				int cId = Integer.valueOf(value);
				if(cId==-1){
					int mapsize=myConnMap.size();
					for(int i=0;i<mapsize;i++){
						try {
							myConnMap.get(i).close();
						} catch (GdmException e) {
							Test_Point[xx][1]="FAULT";
							//System.out.println("DISCONNECT所有失败");
						}
					}
				}
				else{
					try {
						myConnMap.get(cId).close();
						} catch (GdmException e) {
							Test_Point[xx][1]="FAULT";
							//mlog.error("DISCONNECT失败");
							return false;
							}
						}
			}else{
				/*
				 * 2010-11-19-YJM
				 * 修改释放连接的策略，防止没有连接还进行操作
				 */
				if(curConnId == CURCONNID) {
					try {
//						dbOperator.close();
					} catch (GdmException e) {
						Test_Point[xx][1]="FAULT";
						//mlog.error("DISCONNECT失败");
						return false;
//						e.printStackTrace();
					}
				} else {
					try {
						/*
						 * 2010-11-22-YJM
						 * 服务器要是异常退出，此处可能连接已经被释放了
						 */
						if(myConnMap.get(curConnId)!= null) {
							myConnMap.get(curConnId).close();
						}
					} catch (GdmException e) {
						Test_Point[xx][1]="FAULT";
						//mlog.error("DISCONNECT失败");
						return false;
//						e.printStackTrace();
					}
				}
			}
			return true;
		}

		// <CONNECT></CONNECT>内容为空时，连接信息不放入myConnMap。
		//但第一个<CONNECT>要当做connectId为0放入myConnMap
		public boolean newConnect()  {
			String dbUrl = "jdbc:dm://" + curServer + ":" + curPort + "/"+ curDatabase;
			try {
				GdmConnection conn = ConnectionContext.getConnection("usertable");
				//MyConnection newConn = AutoTest.connManager.getConnection(dbUrl, curUid, curPwd);
				MyConnection newConn=new MyConnection(conn);
				//MyConnectionPool pool=new MyConnectionPool(dbUrl,curUid,curPwd);
				//MyConnection newConn = new MyConnection(conn,pool);
				dbOperator.setConn(newConn);
				if(0==myConnMapSize){							// 还没有任何连接信息，默认用工具设置的连接信息
					myConnMap.put(0, newConn);
				}
				if(connectId!=-1)
					myConnMap.put(connectId, newConn);
				if(connectId==-1)
					otherConnList.add(newConn);
				return true;
			} catch (GdmException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.errorConn("新建连接时失败\r\n" + e.getMessage(), curServer,curPort, curDatabase, curUid, curPwd);
				return false;
			//	e.printStackTrace();
			}
		}
		public boolean SQL(Element element){
			if(isInLoop==false)
				sqlNum ++;							// 记录SQL节点行数 ， 以定位错误行数
			//2011-3-25 SQL 为空处理
			if(element.getText().trim()==null||element.getText().trim().equals(""))
				return true;
			curRawSql = element.getText();
			String sql = replaceStr(curRawSql);
			curSql = sql;
			/*
			 * YJM-2010-10-27
			 * 调换config.getInfolevel()和"2"的位置，放置空指针异常
			 */
			if(("2").equalsIgnoreCase(ConfigInfo.getInfolevel()))
			{
				System.out.println("\n--sql no "+sqlNum+" :"+"\r"+ sql);
				//显示正在执行的线程
				if(!(running==0))
				System.out.println("--THIS IS THREADS "+running);

				//Message.msgs.offer("\n--sql no "+sqlNum+" :"+"\r"+ sql);
				//Display.getDefault().asyncExec(new Message());
			}

			ExecuteSql(sql);

			return true;
		}

		public boolean TYPE(Element element){
			String str = element.getText().trim();
			curType = getType(str);
			if(curType==-1){
				//2011-3-8 输出方式更改
				//mlog.error("TYPE值有误："+str);
				//Test_Point=false;
				curType = DIRECT_EXECUTE_SUCCESS;
				return false;
			}
			return true;
		}

		@SuppressWarnings("unchecked")
		public boolean RESULT(Element element){
			String RECORDNUMS = element.elementText("RECORDNUMS");
			if(RECORDNUMS!=null){
				RECORDNUMS = replaceStr(RECORDNUMS.trim());
				Element record = element.element("RECORD");
				int recordNums = Integer.valueOf(RECORDNUMS);
				int recordIndex = 1;
				while(recordIndex<recordNums){
					mapForChange.put("@RECORDNUMS", String.valueOf(recordIndex));
					String COLUMNNUMS = record.elementText("COLUMNNUMS");
					if(COLUMNNUMS!=null){
						COLUMNNUMS = replaceStr(COLUMNNUMS.trim());
						int columnNums = Integer.valueOf(COLUMNNUMS);
						int columnIndex = 1;
						while(columnIndex<columnNums){
							mapForChange.put("@COLUMNNUMS", String.valueOf(columnIndex));
							String columnXml = record.elementText("COLUMN").trim();
							columnXml = replaceStr(columnXml);
//							if(!COMPARE(columnXml, recordIndex-1, columnIndex-1)){
//								mlog.error("结果集比较错误");
//							}
							COMPARE(columnXml, recordIndex-1, columnIndex-1);
							columnIndex++;
						}
					}else{
						ArrayList<Element> columnList = (ArrayList<Element>) record.elements("COLUMN");
						int i=0, cLen = columnList.size();
						for(i=0;i<cLen;i++){
							String columnXml = columnList.get(i).getText().trim();
							columnXml = replaceStr(columnXml);
//							if(!COMPARE(columnXml, recordIndex-1, i)){
//								mlog.error("结果集比较错误");
//							}
							COMPARE(columnXml, recordIndex-1, i);
						}
					}
					recordIndex++;
				}
			}else{
				ArrayList<Element> recordList = (ArrayList<Element>) element.elements();
				int rLen = recordList.size();
				int i=0;
				for(i=0;i<rLen;i++){
					Element record = recordList.get(i);
					String COLUMNNUMS = record.elementText("COLUMNNUMS");
					if(COLUMNNUMS!=null){
						COLUMNNUMS = replaceStr(COLUMNNUMS.trim());
						int columnNums = Integer.valueOf(COLUMNNUMS);
						int columnIndex = 1;
						while(columnIndex<columnNums){
							mapForChange.put("@COLUMNNUMS", String.valueOf(columnIndex));
							String columnXml = record.elementText("COLUMN").trim();
							columnXml = replaceStr(columnXml);
//							if(!COMPARE(columnXml, i, columnIndex-1)){
//								mlog.error("结果集比较错误");
//							}
							COMPARE(columnXml, i, columnIndex-1);
							columnIndex++;
						}
					}else{
						ArrayList<Element> columnList = (ArrayList<Element>) record.elements("COLUMN");
						int j=0, cLen = columnList.size();
						for(j=0;j<cLen;j++){
							String columnXml = columnList.get(j).getText().trim();
							columnXml = replaceStr(columnXml);
//							if(!COMPARE(columnXml, i, j)){
//								mlog.error("结果集比较错误");
//							}
							COMPARE(columnXml, i, j);
						}
					}
				}
			}
			return true;
		}

		public boolean COMPARE(String columnXml, int x, int y) {
			String fromServer = "";
			try {
				switch (curType) {

				case DIRECT_EXECUTE_SELECT_WITH_RESULT:
					/*
					 * 2010-11-18-YJM
					 * 处理sqlResult为NULL的情况
					 */
					if(sqlResult!=null) {
					fromServer = sqlResult.getColumns().get(y).trim().toUpperCase();
					}
					if (sqlResult==null||!columnXml.equals(fromServer)) {
						//Message.msgs.offer("比较结果不正确\r\n\tXML值："+ columnXml + "实际值："	+ fromServer);
						//Display.getDefault().asyncExec(new Message());
						System.out.println("比较结果不正确\r\n\tXML值："+ columnXml + "实际值："	+ fromServer);

						//mlog.error("比较结果不正确\r\n\t"+"当前SQL:"+curRawSql+"\r\n\tXML里：" + columnXml + "实际值："
						//		+ fromServer);
						if(!isinsqlcase){
							Test_Point[xx][1]="FAULT";
						   }else{
							   if(caseExpResult)
							   {
								casesqlcase=false;
								}
							   }
						return false;
					}
					break;
				case DIRECT_EXECUTE_SELECT_COMPARE_RESULT:
					/*
					 * 2010-11-18-YJM
					 * 处理sqlResult为NULL的情况
					 */
					if(sqlResult!=null) {
						fromServer = sqlResult.getFullResult().get(x).get(y).trim();
						if (columnXml.startsWith("0x")|| columnXml.startsWith("0X")) {
							columnXml = columnXml.substring(2);
						}
					}
					if (sqlResult==null||!fromServer.startsWith(columnXml)) {
						//Message.msgs.offer("比较结果不正确\r\n\tXML值："+ columnXml + "实际值："	+ fromServer);
						//Display.getDefault().asyncExec(new Message());
						System.out.println("比较结果不正确\r\n\tXML值："+ columnXml + "实际值："	+ fromServer);
						//mlog.error("比较结果不正确\r\n\t"+"当前SQL:"+curRawSql+"\r\n\tXML里：" + columnXml + "实际值："
						//	+ fromServer);
						if(!isinsqlcase){
							Test_Point[xx][1]="FAULT";
						   }else{
							   if(caseExpResult)
							   {
								casesqlcase=false;
								}
							   }
						return false;
					}
					break;
				case DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL:
					/*
					 * 2010-11-18-YJM
					 * 处理sqlResult为NULL的情况
					 */
					if(sqlResult!=null) {
						fromServer = sqlResult.getFullResult().get(x).get(y).trim();
						if (columnXml.startsWith("0x")|| columnXml.startsWith("0X")) {
							columnXml = columnXml.substring(2);
						}
					}
					if (sqlResult==null||!fromServer.equals(columnXml)) {
						//Message.msgs.offer("比较结果不正确\r\n\tXML值："+ columnXml + "实际值："	+ fromServer);
						//Display.getDefault().asyncExec(new Message());
						System.out.println("比较结果不正确\r\n\tXML值："+ columnXml + "实际值："	+ fromServer);
						if(!isinsqlcase){
							Test_Point[xx][1]="FAULT";
						   }else{
							   if(caseExpResult)
							   {
								casesqlcase=false;
								}
							   }
						//mlog.error("比较结果不正确\r\n\t"+"当前SQL:"+curRawSql+"\r\n\tXML里：" + columnXml + "实际值："
						//		+ fromServer);
						return false;
					}
					break;
				default:
					return false;
				}
			} catch (IndexOutOfBoundsException e) {
				//Message.msgs.offer("比较结果失败"+e.getMessage());
				//Display.getDefault().asyncExec(new Message());
				System.out.println("比较结果失败"+e.getMessage());
				if(!isinsqlcase){
					Test_Point[xx][1]="FAULT";
				   }else{
					   if(caseExpResult)
					   {
						casesqlcase=false;
						}
					   }
				//mlog.error("比较结果失败"+e.getMessage()+"\r\n\t当前SQL语句为："+curRawSql);
				return false;
			}
			return true;
		}
		public boolean SERVER(Element element){
			curServer = element.getText().trim();
			return true;
		}

		// 如果是NEWCONNECTEXECUTE、MORETHREAD和NEWTRANS节点，不处理
		public boolean UID(Element element){
			if(isNewConnectExecute||isMoreThread==true||isTrans==true)
				return true;
			curUid = element.getText().trim();
			return true;
		}
		public boolean PWD(Element element){
			if(isNewConnectExecute||isMoreThread==true||isTrans==true)
				return true;
			curPwd = element.getText().trim();
			return true;
		}
		public boolean DATABASE(Element element){
			if(isNewConnectExecute||isMoreThread==true||isTrans==true)
				return true;
			curDatabase = element.getText().trim();
			return true;
		}
		//2011-3-23  支持当前SQL_CASE的CASEEXPTRSULT关键字（只能支持最后一层的SQL_CASE，不能识别嵌套）
		public boolean SQL_CASE(Element element){
			if(isClear == false)
				curType = DIRECT_EXECUTE_SUCCESS;
			caseExpResult = true;
			isinsqlcase=true;
			casesqlcase=true;
			boolean tempcase=ExecuteElement(element);
			if(caseExpResult){
				if(!casesqlcase)
					Test_Point[xx][1]="FAULT";
				}else{
					if(casesqlcase)
						Test_Point[xx][1]="FAULT";
				}
			isinsqlcase=false;
			caseExpResult = true;
			return tempcase;
		}

		public boolean EFFECTROWS(Element element){
			String str = element.getText().trim();
//			System.out.println(str);	// EFFECTROWS.XML打印信息正确
			int tempNum = Integer.valueOf(str);
			if(tempNum!=effectRows){
				if(!isinsqlcase){
					Test_Point[xx][1]="FAULT";
				   }else{
					   if(caseExpResult)
					   {
						casesqlcase=false;
						}
					   }
				System.out.println("EFFECTROWS比较错误，XML里："+tempNum+"实际："+effectRows);
				//mlog.error("EFFECTROWS比较错误，XML里："+tempNum+"实际："+effectRows);
				return false;
			}
			return true;
		}
		public boolean ExecuteSql(String gql){
			// 此时TYPE不会出现其他的值
			switch(curType){
			case DIRECT_EXECUTE_SUCCESS:
				try {
						effectRows = dbOperator.DirectExecute(gql);
					} catch (GdmException e) {
						//2010-12-22-YJM等待服务器重启
						boolean flag = false;
						if(e.getMessage().equals("网络通信异常")) {
							//exlog.errorWithTime("\r\n--********************\r\n--执行测试用例："+curFilePath);
							System.out.println(gql+"网络通信异常");
							Test_Point[xx][1]="FAULT";
							while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
								InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
								try {
									flag = true;
									Thread.sleep(400);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
							if(flag) {
								try {
									myConn = ConnectionContext.getConnection("usertable");
									dbOperator.setConn(myConn);
								} catch (GdmException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
						//2010-12-22-YJM等待服务器重启
						/*
						 * YJM-2010-10-27
						 * 调换config.getInfolevel()和"0","1","2"的位置，防止空指针异常
						 */
						if(("0").equalsIgnoreCase(ConfigInfo.getInfolevel()))
							//mlog.error("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
						else if(("1").equalsIgnoreCase(ConfigInfo.getInfolevel()))
						{
							//Message.msgs.offer("执行SQL语句失败：\r\n\t"+e.getMessage());
							//Display.getDefault().asyncExec(new Message());
							System.out.println("执行SQL语句失败：\r\n\t"+e.getMessage());
							//mlog.error("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
						}
						else if(("2").equalsIgnoreCase(ConfigInfo.getInfolevel()))
						{
							//Message.msgs.offer("执行SQL语句失败：\r\n\t"+e.getMessage());
							//Display.getDefault().asyncExec(new Message());
							System.out.println("执行SQL语句失败：\r\n\t"+e.getMessage());
							//mlog.error("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
						}
						//YJM-2010-10-27
						return false;
//						e.printStackTrace();
					}
				break;
			case DIRECT_EXECUTE_FAIL:
				try {
						dbOperator.DirectExecute(gql);
						/*
						 * YJM-2010-10-27
						 * 调换config.getInfolevel()和"0","1","2"的位置，防止空指针异常
						 */
						if(("0").equalsIgnoreCase(ConfigInfo.getInfolevel()))
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
							//mlog.error("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t"+sql);
						else if(("1").equalsIgnoreCase(ConfigInfo.getInfolevel()))
						{
							//Message.msgs.offer("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t"+sql+"\r\n\t");
							//Display.getDefault().asyncExec(new Message());
							System.out.println("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t"+gql+"\r\n\t");
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
							//mlog.error("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t"+sql+"\r\n\t");
						}
						else if(("2").equalsIgnoreCase(ConfigInfo.getInfolevel()))
						{
							//Message.msgs.offer("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t");
							//Display.getDefault().asyncExec(new Message());
							System.out.println("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t");
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
							//mlog.error("执行SQL语句失败（预期执行失败，实际执行成功）：\r\n\t"+sql+"\r\n\t");
						}
						return false;
					} catch (GdmException e) {
//						//2010-12-22-YJM等待服务器重启
						boolean flag = false;
						if(e.getMessage().equals("网络通信异常")) {
							//exlog.errorWithTime("\r\n--********************\r\n--执行测试用例："+curFilePath);
							System.out.println(gql+"网络通信异常");
							Test_Point[xx][1]="FAULT";
							while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
								InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
								try {
									flag = true;
									Thread.sleep(400);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
							if(flag) {
								try {
									myConn = ConnectionContext.getConnection("usertable");
									dbOperator.setConn(myConn);
								} catch (GdmException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
						//2010-12-22-YJM等待服务器重启
						//e.printStackTrace();
					}
				break;
			case DIRECT_EXECUTE_IGNORE:
				try {
						dbOperator.DirectExecute(gql);
					} catch (GdmException e) {
//						//2010-12-22-YJM等待服务器重启
						boolean flag = false;
						if(e.getMessage().equals("网络通信异常")) {
							//exlog.errorWithTime("\r\n--********************\r\n--执行测试用例："+curFilePath);
							System.out.println(gql+"网络通信异常");
							Test_Point[xx][1]="FAULT";
							while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
								InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
								try {
									flag = true;
									Thread.sleep(400);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
							if(flag) {
								try {
									myConn = ConnectionContext.getConnection("usertable");
									dbOperator.setConn(myConn);
								} catch (GdmException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
						//2010-12-22-YJM等待服务器重启
						//e.printStackTrace();
					}
				break;
//			case DIRECT_EXECUTE_SELECT_COMPARE_RESULT:
//			case DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL:
//			case DIRECT_EXECUTE_SELECT_WITH_RESULT:
			default: try {
						sqlResult = null;
//						sqlResult = dbOperator.ExecuteQuery(sql);
					} catch (GdmException e) {
						//2010-12-22-YJM等待服务器重启
						boolean flag = false;
						if(e.getMessage().equals("网络通信异常")) {
							//exlog.errorWithTime("\r\n--********************\r\n--执行测试用例："+curFilePath);
							//exlog.error(sql);
							Test_Point[xx][1]="FAULT";
							while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
								InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
								try {
									flag = true;
									Thread.sleep(400);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
							if(flag) {
								try {
									myConn = ConnectionContext.getConnection("usertable");
									dbOperator = new DbOperator(myConn);
								} catch (GdmException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
						//2010-12-22-YJM等待服务器重启
						/*
						 * YJM-2010-10-27
						 * 调换config.getInfolevel()和"0","1","2"的位置，防止空指针异常
						 */
						if(("0").equalsIgnoreCase(ConfigInfo.getInfolevel()))
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
							//mlog.error("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
						else if(("1").equalsIgnoreCase(ConfigInfo.getInfolevel()))
						{
							//Message.msgs.offer("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
							//Display.getDefault().asyncExec(new Message());
							System.out.println("执行SQL语句失败：\r\n\t"+gql+"\r\n\t"+e.getMessage());
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
							//mlog.error("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
						}
						else if(("2").equalsIgnoreCase(ConfigInfo.getInfolevel()))
						{
							//Message.msgs.offer("执行SQL语句失败：\r\n\t"+e.getMessage());
							//Display.getDefault().asyncExec(new Message());
							System.out.println("执行SQL语句失败：\r\n\t"+e.getMessage());
							if(!isinsqlcase){
								Test_Point[xx][1]="FAULT";
							   }else{
								   if(caseExpResult)
								   {
									casesqlcase=false;
									}
								   }
							//mlog.error("执行SQL语句失败：\r\n\t"+sql+"\r\n\t"+e.getMessage());
						}
//						e.printStackTrace();
						return false;
					}
				break;
			}
			return true;
		}
		public boolean CLEAR(Element element){
			// 2010-4-22 添加 CLEAR 中忽略错误
			isClear = true;
			curType = DIRECT_EXECUTE_IGNORE;

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
			xx++;
			Test_Point[xx][0]="CLEAR";
			Test_Point[xx][1]="True";
			ExecuteElement(element);
			return true;
		}

		public boolean GETDMINI(Element element){
			String iniPath = element.elementText("INIPATH").trim();
			iniPath = replaceStr(iniPath);
			String valName = element.elementText("VALNAME").trim();
			String value = null;
			try {
				value = getdmini(iniPath, valName);
				if(value==null){
					Test_Point[xx][1]="FAULT";
					//mlog.error("获取dm.ini参数"+valName+"错误");
					return false;
				}
			} catch (IOException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("获取dm.ini参数"+valName+"错误");
				return false;
//				e.printStackTrace();
			}
			String val = element.elementText("VAL").trim();
			mapForChange.put(val, value);
			return true;
		}

		// 不支持多重<IF><ELSE>嵌套
		public boolean IF(Element element){
			String str = element.getText().trim();
			String sql = "";
			if(str.startsWith("BOOL:")){
				str = str.substring("BOOL:".length());
				str = replaceStr(str);
				sql = "SELECT 1 WHERE "+ str+";";
				try {
					ifIsTrue = dbOperator.DirectExecute(sql) == 1 ? true
							: false;
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
					//mlog.error("IF节点表达式求值错误："+str);
//					e.printStackTrace();
				}
			}else if(str.startsWith("FromSql:")){
				str = str.substring("FromSql:".length());
				str = replaceStr(str);
				try {
					ifIsTrue = dbOperator.DirectExecute(str)==1?true:false;
				} catch (GdmException e) {
					// 执行错误认为IF条件为假
					ifIsTrue = false;
					Test_Point[xx][1]="FAULT";
					//mlog.error("表达式求值错误："+str);
//					e.printStackTrace();
				}
			}else{
				str = replaceStr(str);
				sql = "SELECT 1 WHERE "+str+";";
				try {
					ifIsTrue = dbOperator.DirectExecute(sql) == 1 ? true
							: false;
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
					//mlog.error("IF节点表达式求值错误："+str);
//					e.printStackTrace();
				}
			}
			return true;
		}

		public boolean ELSE(Element element){
			if(false == ifIsTrue){
				ExecuteElement(element);
				ifIsTrue = true;
			}
			return true;
		}
		//2011-3-24  修改fromSql功能，支持fromSQL中有@替换的参数
		public boolean TEMPSTR(Element element){
			String str = element.getText().trim();
			if(str.equals("")){
				mapForChange.put("@TEMPSTR", "");
				return true;
			}
			String relStr = mapForChange.get("@TEMPSTR");
			if(relStr==null){
				relStr = "";
			}
			mapForChange.put("@TEMPSTR", relStr+str);
			str=mapForChange.get("@TEMPSTR");
			if(str.startsWith("FromSql:")){
				str = str.substring("FromSql:".length());
				try {
//					str = dbOperator.getExp(str);
				} catch (GdmException e) {
//					e.printStackTrace();
				}
				if (str == null || str.equals("")) {
					mapForChange.put("@TEMPSTR", "");
				} else {
					mapForChange.put("@TEMPSTR",str);
				}
			}
			return true;
		}
		//2011-3-24  修改fromSql功能，支持fromSQL中有@替换的参数
		public boolean SQLSTR(Element element) {
			Element exp = element.element("EXP");
			if(exp!=null){
				String str = String.valueOf(EXP(exp));
				mapForChange.put("@SQLSTR", str);
				return true;
			}
			String str = element.getText().trim();
			String relStr = "";
			if(str.equals("@")){
				str = replaceStr(mapForChange.get("@SQLSTR"));
				mapForChange.put("@SQLSTR", str);
				return true;
			}
			str = replaceStr(str);
			if (str == null || str.equals("")) {
				mapForChange.put("@SQLSTR", "");
			} else {
				relStr = mapForChange.get("@SQLSTR");
				if(relStr==null){
					relStr = "";
				}
				relStr = relStr + str;
				mapForChange.put("@SQLSTR", relStr);
			}
			str=mapForChange.get("@SQLSTR");
			if(str.startsWith("FromSql:")){
				str = str.substring("FromSql:".length());
				try {
//					str = dbOperator.getExp(str);
				} catch (GdmException e) {
//					e.printStackTrace();
				}
				if (str == null || str.equals("")) {
					mapForChange.put("@SQLSTR", "");
				} else {
					mapForChange.put("@SQLSTR",str);
				}

			}
			return true;
		}
		public boolean SQLSTR1(Element element) {
			Element exp = element.element("EXP");
			if(exp!=null){
				String str = String.valueOf(EXP(exp));
				mapForChange.put("@SQLSTR1", str);
				return true;
			}
			String str = element.getText().trim();
			String relStr = "";
			if(str.equals("@")){
				str = replaceStr(mapForChange.get("@SQLSTR1"));
				mapForChange.put("@SQLSTR1", str);
				return true;
			}
			str = replaceStr(str);
			if (str == null || str.equals("")) {
				mapForChange.put("@SQLSTR1", "");
			} else {
				relStr = mapForChange.get("@SQLSTR1");
				if(relStr==null){
					relStr = "";
				}
				relStr = relStr + str;
				mapForChange.put("@SQLSTR1", relStr);
			}
			str=mapForChange.get("@SQLSTR1");
			if(str.startsWith("FromSql:")){
				str = str.substring("FromSql:".length());
				try {
//					str = dbOperator.getExp(str);
				} catch (GdmException e) {
//					e.printStackTrace();
				}
				if (str == null || str.equals("")) {
					mapForChange.put("@SQLSTR1", "");
				} else {
					mapForChange.put("@SQLSTR1",str);
				}

			}
			return true;
		}
		public boolean SQLSTR2(Element element) {
			Element exp = element.element("EXP");
			if(exp!=null){
				String str = String.valueOf(EXP(exp));
				mapForChange.put("@SQLSTR2", str);
				return true;
			}
			String str = element.getText().trim();
			String relStr = "";
			if(str.equals("@")){
				str = replaceStr(mapForChange.get("@SQLSTR2"));
				mapForChange.put("@SQLSTR2",str);
				return true;
			}
			str = replaceStr(str);
			if (str == null || str.equals("")) {
				mapForChange.put("@SQLSTR2", "");
			} else {
				relStr = mapForChange.get("@SQLSTR2");
				if(relStr==null){
					relStr = "";
				}
				relStr = relStr + str;
				mapForChange.put("@SQLSTR2", relStr);
			}
			str=mapForChange.get("@SQLSTR2");
			if(str.startsWith("FromSql:")){
				str = str.substring("FromSql:".length());
				try {
//					str = dbOperator.getExp(str);
				} catch (GdmException e) {
//					e.printStackTrace();
				}
				if (str == null || str.equals("")) {
					mapForChange.put("@SQLSTR2","");
				} else {
					mapForChange.put("@SQLSTR2",str);
				}

			}
			return true;
		}

		public boolean EXEXML(Element element){
			String str = element.getText().trim();
			String temp = curFilePath.substring(0, curFilePath.lastIndexOf(filePathSepartor));
			// <EXEXML>\SEC_40011.TXT</EXEXML>
			// 按文档要求应该直接写文件名，但有的带\号了，所以处理一下
			if(str.startsWith("\\")){
				str = temp+str;
			}else{
				str = temp + filePathSepartor + str;
			}
			XmlExecutor txtExecuter = new XmlExecutor(str);
			//Message.msgs.offer("嵌套调用测试用例："+str);
			//Display.getDefault().asyncExec(new Message());
			//System.out.println("嵌套调用测试用例："+str);
			txtExecuter.ExecuteXml(str);
			return true;
		}

		public boolean EXEPROCESS(Element element){
			String str = element.getText().trim();
			String temp = curFilePath.substring(0, curFilePath.lastIndexOf(filePathSepartor));
			// 按文档要求应该直接写文件名，但有的带\号了，所以处理一下
			if(str.startsWith("\\")){
				str = temp+str;
			}else{
				str = temp + filePathSepartor + str;
			}
			Runtime runtime = Runtime.getRuntime();
			Process proc = null;
			// 暂时不知道传什么参数
			String[] command = new String[6];
			command[0] = str;
			command[1] = "";
			command[2] = "";
			command[3] = "";
			command[4] = "";
			command[5] = "";

			try {
				proc = runtime.exec(command);
				StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());
				StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());
				errorGobbler.start();
				outputGobbler.start();
//				int exitVal = proc.waitFor();
				proc.waitFor();
			} catch (IOException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("调用外部EXE程序错误\r\n\t"+e.getMessage());
//				e.printStackTrace();
			} catch (InterruptedException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("调用外部EXE程序错误\r\n\t"+e.getMessage());
//				e.printStackTrace();
			}
			return true;
		}

		/*
		* 2010-11-16-YJM
		* 解决问题：函数不存在，函数名：EXEPROCESSEX
		* 用这个关键字调用的程序，如果程序输出消息前面带0，就表示这个程序内部产生了错误，否则，便认定为正常消息
		* 它和EXEPROCESS的区别在于，EXEPROCESS是通过程序执行完以后返回值来判断执行是否成功，而它是通过判断输出消息是否带0前缀来判断执行是否成功
		*/
		public boolean EXEPROCESSEX(Element element){
			String str = element.getText().trim();
			String temp = curFilePath.substring(0, curFilePath.lastIndexOf(filePathSepartor));
			// 按文档要求应该直接写文件名，但有的带\号了，所以处理一下
			if(str.startsWith("\\")){
				str = temp+str;
			}else{
				str = temp + filePathSepartor + str;
			}
			Runtime runtime = Runtime.getRuntime();
			Process proc = null;
			// 暂时不知道传什么参数
			String[] command = new String[6];
			command[0] = str;
			command[1] = "";
			command[2] = "";
			command[3] = "";
			command[4] = "";
			command[5] = "";

			try {
				proc = runtime.exec(command);
				StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());
				StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());
				errorGobbler.start();
				outputGobbler.start();
//				int exitVal = proc.waitFor();
				proc.waitFor();
			} catch (IOException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("调用外部EXE程序错误\r\n\t"+e.getMessage());
				e.printStackTrace();
			} catch (InterruptedException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("调用外部EXE程序错误\r\n\t"+e.getMessage());
				e.printStackTrace();
			}
			return true;
		}

		public boolean IGNORE(Element element){
			return true;
		}

		public boolean LOOP(Element element) {
			isInLoop = true;
			loopCount++;
			timesIndex[loopCount] = 1;
			String startTime = element.elementText("STARTTIMES");
			int startTimeInt = -1000;
			if (startTime != null) {
				startTime = replaceStr(startTime.trim());
				try {
					startTimeInt = Integer.valueOf(startTime);
					timesIndex[loopCount] = startTimeInt;
				} catch (NumberFormatException e) {
					Test_Point[xx][1]="FAULT";
					//mlog.error("STARTTIME数值转换错误");
				}
			}
			Element timesE = element.element("TIMES");
			if (timesE != null) {
				Element exp = timesE.element("EXP");
				if (exp != null) {
					times[loopCount] = EXP(exp);
				} else {
					String tempStr = timesE.getText();
					tempStr = replaceStr(tempStr);
					times[loopCount] = Integer.valueOf(tempStr.trim());
				}
			} else {
				times[loopCount] = 10000; // 认为没有超过10000的循环，模拟无限循环
			}

			if (startTimeInt != -1000 && startTimeInt < 0) {
				timesIndex[loopCount] = times[loopCount];
				while (timesIndex[loopCount] >= 1) {
					mapForChange.put("@" + loopCount + "TIMES", String.valueOf(timesIndex[loopCount]));
					// 2010-3-23 注释掉
					// 为了E:\testroot\dmserver1\for_xml\00-security\1-SACL\TP\TP_F_01.XML
//					curType = DIRECT_EXECUTE_SUCCESS;
					ExecuteElement(element);
					if(isBreak==true){
						isBreak = false;
						break;
					}
					timesIndex[loopCount]--;
				}
			} else {
//				timesIndex[loopCount] = Integer.valueOf(startTime);
				while (timesIndex[loopCount] <= times[loopCount]) {
					mapForChange.put("@" + loopCount + "TIMES", String
							.valueOf(timesIndex[loopCount]));
//					curType = DIRECT_EXECUTE_SUCCESS;
					ExecuteElement(element);
					if(isBreak==true){
						isBreak = false;
						break;
					}
					timesIndex[loopCount]++;
				}
			}
			loopCount--;
			isInLoop = false;
			return true;
		}

		public int EXP(Element element){
			String expStr = element.getText().trim();
			expStr = replaceStr(expStr);
			String temp;
			int tempInt = 0;
			try {
//				temp = dbOperator.getExp("SELECT "+expStr);
//				int tempDot = temp.indexOf('.');
//				if(tempDot!=-1){
//					temp = temp.substring(0,tempDot);
//				}
//				tempInt = Integer.valueOf(temp);
			} catch (GdmException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("EXP计算错误");
				return -1;
//				e.printStackTrace();
			} catch(NumberFormatException e){
				Test_Point[xx][1]="FAULT";
				//mlog.error("EXP计算错误");
				return -1;
			}
			return tempInt;
		}

		// 在LOOP内部处理了
		public boolean TIMES(Element element){
			return true;
		}

		public boolean MORETHREAD(Element element){
			isMoreThread = true;
			String THREADS = element.elementText("THREADS");
			String TIMES = element.elementText("TIMES");
			GdmConnection oldConn = dbOperator.getConn();
			if(THREADS==null){
				/*
				 * 2010-11-19-YJM
				 * 处理TYPE为LOGIN_FAIL的情况
				 */
				String str = element.elementText("TYPE");
				if(str == null)
				{
					curType = DIRECT_EXECUTE_SUCCESS;
				} else {
					curType = getType(str.trim());
				}

				String strUid = element.elementText("UID");
				/*
				 * 2010-11-18-YJM
				 * 考虑测试用例里<MORETHREAD>节点下没有<UID>、<PWD>、<DATABASE>这些节点的情况
				 */
				if(strUid == null) {
					strUid = ConfigInfo.getUid();
				}
				strUid = strUid.trim();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				curUid = strUid;

				String strPwd = element.elementText("PWD");
				if(strPwd == null) {
					strPwd = ConfigInfo.getPwd();
				}
				/*
				 * 2010-11-18-YJM
				 * 考虑strPwd为空，所以把trim操作放在此处执行
				 */
				strPwd = strPwd.trim();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				curPwd = strPwd;

				String strDb = element.elementText("DATABASE");
				if(strDb == null) {
					strDb = ConfigInfo.getDatabase();
				}
				strDb = strDb.trim();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				int i = curURL.lastIndexOf("/");
				curURL = curURL.substring(0, i+1) + strDb;
				/*
				 * 2010-11-19-YJM
				 * 考虑MORETHREAD节点里有CONNECT，且CONNECT节点里有值的情况
				 */
				String curId = element.elementText("CONNECT");
				/*
				 * 2010-11-22-YJM
				 * 增加对空串的处理
				 */
				if(curId != null && !(curId.trim().equals(""))) {
					/*
					 * 2010-11-22-YJM
					 * 增加trim操作
					 * <CONNECT> 0 </CONNECT>
					 */
					curConnId = Integer.valueOf(curId.trim());
				}

				strUid = replaceStr(strUid);
				strPwd = replaceStr(strPwd);
				strDb = replaceStr(strDb);					// 2010-3-24添加 有@DATABASE

				MyConnection newConn = null;

				/*
				 * 2010-11-19-YJM
				 * 考虑MORETHREAD节点里有CONNECT，且CONNECT节点里有值的情况
				 */
				try {
					newConn = AutoTest.connManager.getConnection("usertable");
					dbOperator.setConn(newConn);
					if(curId != null && !(curId.equals(""))) {
						if(0==myConnMapSize){							// 还没有任何连接信息，默认用工具设置的连接信息
							myConnMap.put(0, newConn);
						}
						if(connectId!=-1)
							myConnMap.put(connectId, newConn);
					}
				} catch (GdmException e) {
					/*
					 * 2010-11-19-YJM
					 * 处理TYPE为LOGIN_FAIL的情况
					 */
					if(curType == LOGIN_FAIL) {
//						e.printStackTrace();
					} else {
						Test_Point[xx][1]="FAULT";
						//mlog.error("MORETHREAD节点执行错误:" + e.getMessage());
					}
					// e.printStackTrace();
				} catch (InterruptedException e) {
					/*
					 * 2010-11-19-YJM
					 * 处理TYPE为LOGIN_FAIL的情况
					 */
					if(curType == LOGIN_FAIL) {
//						e.printStackTrace();
					} else {
						Test_Point[xx][1]="FAULT";
						//mlog.error("MORETHREAD节点执行错误:" + e.getMessage());
					}
					// e.printStackTrace();
				}

				boolean b = ExecuteElement(element);

				try {
					/*
					 * 2010-11-18-YJM
					 * 处理conn为空的情况
					 */
					if(null != newConn) {
						newConn.close();
					}
					dbOperator.setConn(oldConn);
				} catch (GdmException e) {
//						e.printStackTrace();
				}
				isMoreThread = false;
				return b;
			}else{
				THREADS = replaceStr(THREADS.trim());
				TIMES = replaceStr(TIMES.trim());
				int threads = Integer.valueOf(THREADS);
				int times = Integer.valueOf(TIMES);
				int i = 0;
				// 启动threads个线程，把times参数和element当参数传进去
				for(i=0;i<threads;i++){
					NewThreads newThreads = new NewThreads(times, element);
					Thread thread = new Thread(newThreads);
					thread.run();
				}
				// 主线程继续运行
			}
			return true;
		}
		public boolean THREADS(Element element){
			return true;
		}
		public class NewThreads implements Runnable{
			int i;
			int times;
			Element element;
			public NewThreads(int times, Element element){
				this.times = times;
				this.element = element;
			}

			public void run() {
				for(i=0;i<times;i++){
					XmlExecutor xmlExecutor = new XmlExecutor(curFilePath);
					xmlExecutor.dbOperator = dbOperator;
					xmlExecutor.ExecuteElement(element);
				}
			}
		}

		public boolean STARTTIMES(Element element){
			return true;
		}

		//2011-3-25 NEWCONNECTEXECUTE重写连接和释放方法
		public boolean NEWCONNECTEXECUTE(Element element){
			String str = element.elementText("TYPE");
			if(str == null)
			{
				curType = DIRECT_EXECUTE_SUCCESS;	// 2010-3-19 添加
			} else {
				curType = getType(str.trim());
			}

			GdmConnection oldConn = dbOperator.getConn();
			isNewConnectExecute = true;
			// 2010-3-19 添加trim()
			String tempUid = element.elementText("UID");
			if (tempUid == null) {
				tempUid = ConfigInfo.getUid();
			} else
				tempUid = tempUid.trim();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				curUid = tempUid;
			String tempPwd = element.elementText("PWD");
			if (tempPwd == null) {
				tempPwd = ConfigInfo.getPwd();
			} else
				tempPwd = tempPwd.trim();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				curPwd = tempPwd;
			String tempDatabase = element.elementText("DATABASE");
			if (tempDatabase == null) {
				tempDatabase = ConfigInfo.getDatabase();
			} else
				tempDatabase = tempDatabase.trim();
				//2010-12-23-YJM重启服务器后用户名和密码的重置
				int i = curURL.lastIndexOf("/");
				curURL = curURL.substring(0, i+1) + tempDatabase;
			/*
			 * 2010-11-19-YJM
			 * 考虑MORETHREAD节点里有CONNECT，且CONNECT节点里有值的情况
			 */
			String curId = element.elementText("CONNECT");
			/*
			 * 2010-11-22-YJM
			 * 增加对空串的处理
			 */
			if(curId != null && !(curId.trim().equals(""))) {
				curConnId = Integer.valueOf(curId.trim());
			}
			// 2010-3-22 添加
			tempUid = replaceStr(tempUid);
			tempPwd = replaceStr(tempPwd);
			tempDatabase = replaceStr(tempDatabase);

			MyConnection newConn=null;

			/*
			 * 2010-11-19-YJM
			 * 考虑MORETHREAD节点里有CONNECT，且CONNECT节点里有值的情况
			 */
			try {
				GdmConnection Conn = AutoTest.connManager.getConnection("usertable");
				newConn=new MyConnection(Conn);
				dbOperator.setConn(newConn);
				if(curId != null && !(curId.equals(""))) {
					if(0==myConnMapSize){							// 还没有任何连接信息，默认用工具设置的连接信息
						myConnMap.put(0, newConn);
					}
					if(connectId!=-1)
						myConnMap.put(connectId, newConn);
				}

				ExecuteElement(element);

				} catch (GdmException e) {
					/*
					 * 2010-11-19-YJM
					 * 处理TYPE为LOGIN_FAIL的情况
					 */
					if(curType == LOGIN_FAIL) {
//						e.printStackTrace();
					} else {
						Test_Point[xx][1]="FAULT";
						//mlog.error("MORETHREAD节点执行错误:" + e.getMessage());
					}
					// e.printStackTrace();
				} catch (InterruptedException e) {
					/*
					 * 2010-11-19-YJM
					 * 处理TYPE为LOGIN_FAIL的情况
					 */
					if(curType == LOGIN_FAIL) {
//						e.printStackTrace();
					} else {
						Test_Point[xx][1]="FAULT";
						//mlog.error("MORETHREAD节点执行错误:" + e.getMessage());
					}
					// e.printStackTrace();
				}
			try {
				if(null != newConn) {
					newConn.close();
				}
			} catch (GdmException e) {
				e.printStackTrace();
			}
			//2011-3-25 BUG修改：避免释放资源缓慢，造成数据库对象被占用
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {

			}
			dbOperator.setConn(oldConn);
			isNewConnectExecute = false;
			return true;
		}

		public boolean OPEN(Element element){
			try {
				// 再执行一遍刚才的SQL语句 记录结果集
//				sqlResult = dbOperator.ExecuteQuery(curSql);
			} catch (GdmException e) {
//				e.printStackTrace();
			}
				ExecuteElement(element);
				/*
				 * 2010-11-22-YJM
				 * 考虑空结果集的情况
				 * <OPEN> <FETCHNEXT>  </FETCHNEXT> </OPEN>
				 */
				if(sqlResult != null) {
					ArrayList<String> columnList = sqlResult.getColumns();
					int i;
					int columnLen = columnList.size();
					for(i=0;i<columnLen;i++){
	//					mapForChange.remove("@"+columnList.get(i));
						rubbishMap.add(columnList.get(i));
				}
			}
			fetchIndex = -1;
			return true;
		}

		public boolean FETCHNEXT(Element element){
			fetchIndex++;
			mapForChange.put("@FETCHNEXT", String.valueOf(fetchIndex+1));
			/*
			 * 2010-11-22-YJM
			 * 考虑空结果集的情况
			 * <OPEN> <FETCHNEXT>  </FETCHNEXT> </OPEN>
			 */
			if(sqlResult != null) {
				ArrayList<String> columnList = sqlResult.getColumns();
				ArrayList<ArrayList<String>> fullList = sqlResult.getFullResult();
				ArrayList<String> curResult = null;
				try{
					curResult = fullList.get(fetchIndex);
				}catch(IndexOutOfBoundsException e){
					mapForChange.put("@FETCHNEXT", "0");
					return true;
				}


				int i;
				int columnLen = columnList.size();
				for(i=0;i<columnLen;i++){
					mapForChange.put("@"+columnList.get(i), curResult.get(i));
				}
			}
			return true;
		}

		public boolean FMES(Element element){
			//String str = element.getText();
			//2011-3-11FMES暂时被屏蔽
			//mlog.error(str);
			return true;
		}

		public boolean TOGETHER(Element element){
			return true;
		}

		public boolean NOSHOW(Element element){
			return true;
		}

		public boolean SETCONNECTID(Element element){
			curConnId = Integer.valueOf(element.getText().trim());
			myConn = myConnMap.get(curConnId);
			dbOperator.setConn(myConn);
			return true;
		}

		public boolean RECONNECT(Element element){
			return true;
		}
		//2011-3-23 添加CASEEXPRESULT方法
		public boolean CASEEXPRESULT(Element element){
			String str = element.getText().trim().toUpperCase();
			if(str.equals("TRUE")|| str.equals("DIRECT_EXECUTE_SUCCESS")){
				caseExpResult=true;
				}
			else{
				System.out.println("此SQL_CASE预期执行失败");
				caseExpResult = false;
				}
			return true;
		}

		public boolean SQLSTATE(Element element){
			return true;
		}
		public boolean NERROR(Element element){
			return true;
		}

		public boolean SMES(Element element){
			String str = element.getText();
			str = replaceStr(str);
			System.out.println(str);
			return true;
		}

		// Date.getTime() 得到的是13位的一个数，为与原XML工具兼容这里添4个0，得到秒的话除以10^7
		public boolean TIMETICKS(Element element){
			String str = element.getText().trim();
			Date date = new Date();
			mapForChange.put("@"+str, String.valueOf(date.getTime())+"0000");
			rubbishMap.add(str);
			return true;
		}
		//2011-3-17
		//重写NEWTRANS方法，支持四个隔离级的设定
		public boolean BEGINTRANS(Element element){
			try {
				dbOperator.closeAutoCommit();
//				dbOperator.DirectExecute("commit");
				dbOperator.openAutoCommit();
			} catch (GdmException e) {
				//mlog.error("设定非自动提交属性失败");
				System.out.println("设定非自动提交属性失败");
				Test_Point[xx][1]="FAULT";
//				e.printStackTrace();
			}
			String str = element.getText().trim();
			if(str.equals("")){
				//继续使用当前隔离级
				;
			}else if(str.toLowerCase().equals("readcommitted")){
				try {
					dbOperator.DirectExecute("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
				} catch (GdmException e) {
					System.out.println("设定事务隔离级读提交失败");
					Test_Point[xx][1]="FAULT";
					//e.printStackTrace();
				}
			}else if(str.toLowerCase().equals("readuncommitted")){
				try {
					dbOperator.DirectExecute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
				} catch (GdmException e) {
					System.out.println("设定事务隔离级脏读失败");
					Test_Point[xx][1]="FAULT";
					//e.printStackTrace();
				}
			}else if(str.toLowerCase().equals("serializable")){
				try {
					dbOperator.DirectExecute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE");
				} catch (GdmException e) {
					System.out.println("设定事务隔离级可串行化失败");
					Test_Point[xx][1]="FAULT";
					//e.printStackTrace();
				}
			}else if(str.toLowerCase().equals("repeatableread")){
				try {
					dbOperator.DirectExecute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");
				} catch (GdmException e) {
					System.out.println("设定事务隔离级可重复读失败");
					Test_Point[xx][1]="FAULT";
					//e.printStackTrace();
				}
			}else{
				System.out.println("非法关键字");
				Test_Point[xx][1]="FAULT";
			}

			return true;
		}

		public boolean ENDTRANS(Element element){
			String str = element.getText().trim();
			try {
				/*
				 * 2010-11-22-YJM
				 * 当ENDTRANS节点里为空时，默认为COMMIT
				 * <ENDTRANS></ENDTRANS>
				 */
				if (str.toUpperCase().equals("COMMIT")||str.equals("")||str.toUpperCase().equals("COMMIT;")) {
					dbOperator.commit();
				} else if (str.toUpperCase().equals("ROLLBACK")||str.toUpperCase().equals("ROLLBACK;")) {
					dbOperator.rollback();
				} else {
					Test_Point[xx][1]="FAULT";
					System.out.println("ENDTRANS节点关键字错误：" + str);
					//mlog.error("ENDTRANS节点关键字错误：" + str);
					return false;
				}
			} catch (GdmException e) {
				Test_Point[xx][1]="FAULT";
				System.out.println(str+"错误");
				//mlog.error(str+"错误");
				return false;
			}

			try {
				dbOperator.openAutoCommit();
			} catch (GdmException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("关闭自动提交属性失败");
//				e.printStackTrace();
			}
			return true;
		}

		public boolean LEVEL(Element element){
			return true;
		}

		// TYPE: "IN", "OUT", "IN OUT", "RETURN TYPE"
		public boolean PARAMETER(Element element){

			return true;
		}

		public boolean delFile(File f) {
  		 	if(f.isDirectory()) {
  		 		for(File ft : f.listFiles())
  		 			delFile(ft);
  		 	}
  		 	return f.delete();
		}

		public boolean DELFILE(Element element){
			String str = element.getText().trim();
			str = replaceStr(str);
			boolean b = true;
			File file = new File(str);
			b = delFile(file);
			if (b == false&&curType==DIRECT_EXECUTE_SUCCESS) {
				if(!isinsqlcase){
					Test_Point[xx][1]="FAULT";
				   }else{
					   if(caseExpResult)
					   {
						casesqlcase=false;
						}
					   }
				System.out.println("删除文件错误：" + str);
				//mlog.error("删除文件错误：" + str);
				return false;
			}
			return true;
		}

		public boolean CREATEFILE(Element element){
			String fileName = element.elementText("FILENAME").trim();
			fileName = replaceStr(fileName);
			String writeFlag = element.elementText("WRITEFLAG");
			String val = element.elementText("VAL");
			try {
				if(writeFlag !=null){
					if (writeFlag.trim().equals("Coverage")) {
						FileWriter fw = new FileWriter(fileName, false);
						fw.write(val);
						fw.close();
					} else if (writeFlag.trim().equals("Additional")) {
						FileWriter fw = new FileWriter(fileName, true);
						fw.write(val);
						fw.close();
					}
				}else{
					FileWriter fw = new FileWriter(fileName, false);
					fw.write(val);
					fw.close();
				}

			} catch (IOException e) {
				Test_Point[xx][1]="FAULT";
				//mlog.error("创建文件失败："+e.getMessage());
//				e.printStackTrace();
			}
			return true;
		}
		public boolean COPYFILE(Element element){
			String oldFile = element.elementText("OLDFILE").trim();
			oldFile = replaceStr(oldFile);
			String newFile = element.elementText("NEWFILE").trim();
			newFile = replaceStr(newFile);
			try{
				FileInputStream oldfi = new FileInputStream(oldFile);
				File newf = new File(newFile);
				FileOutputStream newfi = new FileOutputStream(newf);
				byte[] buffer = new byte[1024];
				int count=0;

				while((count = oldfi.read(buffer))!=-1){
					newfi.write(buffer,0,count);
//
				}
				oldfi.close();
				newfi.close();
			}catch(FileNotFoundException e){
				Test_Point[xx][1]="FAULT";
				System.out.println("文件不存在："+oldFile);
				//mlog.error("文件不存在："+oldFile);
			} catch (IOException e) {
//				e.printStackTrace();
			}

			return true;
		}
		public boolean TestPointBegin(Element element){
			//2011-3-8
			xx++;
			Test_Point[xx][0]=element.getText();
			Test_Point[xx][1]="True";
			System.out.println(element.getText().trim());
			return true;
		}

		/*
		 * YJM-2010-10-27
		 * 针对log文件里有这样的报错：TESTPOINTBEGIN-->在XmlExecutor.java里添加函数TESTPOINTBEGIN
		 */
		public boolean TESTPOINTBEGIN(Element element){
			//2011-3-8
			xx++;
			Test_Point[xx][0]=element.getText();
			Test_Point[xx][1]="True";

			return true;
		}
		//YJM-2010-10-27
		public boolean RESULTROWS(Element element){
			String str = element.getText().trim();
			int resrows = Integer.valueOf(str);
			try {
				int count = dbOperator.GetResultRows(curSql);
				if(resrows!=count){
					if(!isinsqlcase)
						   Test_Point[xx][1]="FAULT";
						else
							casesqlcase=false;
					System.out.println("结果集比较错误XML："+str+"实际："+count);
					//mlog.error("结果集比较错误XML："+str+"实际："+count);
					return false;
				}
			} catch (GdmException e) {
				//2010-12-22-YJM等待服务器重启
				boolean flag = false;
				if(e.getMessage().equals("网络通信异常")) {
					//exlog.errorWithTime("\r\n--********************\r\n--执行测试用例："+curFilePath);
					//exlog.error(curSql);
					Test_Point[xx][1]="FAULT";
					while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
						InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
						try {
							flag = true;
							Thread.sleep(400);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					if(flag) {
						try {
							myConn = ConnectionContext.getConnection("usertable");
							dbOperator = new DbOperator(myConn);
						} catch (GdmException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				//2010-12-22-YJM等待服务器重启
				e.printStackTrace();
			}
			return true;
		}

		//2010-3-19添加
		// element 为空
		public boolean NEWTRANS(Element element){
			transCount++;
			//当新起线程超过程序允许时不处理
			if(transCount>=TRANS_COUNT){
				System.out.println(transCount+"NEWTRANS过多");
				return true;
				}

			try {
//				Connection conn = DriverManager.getConnection("jdbc:dm://" + curServer + ":" + curPort + "/"
//								+ curDatabase, curUid, curPwd);
				GdmConnection conn = ConnectionContext.getConnection("usertable");

				transConnList.add(conn);
				DbOperator dbOperator = new DbOperator(conn);
				trans[transCount]=new TRANSEXecurtor(dbOperator,true);
				transthread[transCount]=new Thread(trans[transCount]);
			} catch (GdmException e) {
				Test_Point[xx][1]="FAULT";
				System.out.println("NEWTRANS中申请连接失败");
				//mlog.error("NEWTRANS中申请连接失败");
				e.printStackTrace();
			}
			trans[transCount].waitnum=transCount;
			transthread[transCount].start();
			try {
				Thread.currentThread().sleep(2000);
			} catch (InterruptedException e1) {

				e1.printStackTrace();
			}
			return true;
		}
		public boolean TRANS0(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[0].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[0].setElement(element);
			   synchronized(trans[0].WaitTrans[0]){
				trans[0].WaitTrans[0].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}

			if(!trans[0].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS1(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[1].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[1].setElement(element);
			   synchronized(trans[1].WaitTrans[1]){
				trans[1].WaitTrans[1].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			   /*
			try {
				transthread[1].join();
			} catch (InterruptedException e) {
				System.out.println("TRANS同步异常");
				Test_Point[xx][1]="FAULT";
				//e.printStackTrace();
			}
			*/
			if(!trans[1].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS2(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[2].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[2].setElement(element);
			   synchronized(trans[2].WaitTrans[2]){
				trans[2].WaitTrans[2].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			   /*
			try {
				transthread[2].join();
			} catch (InterruptedException e) {
				System.out.println("TRANS同步异常");
				Test_Point[xx][1]="FAULT";
				//e.printStackTrace();
			}
			*/
			if(!trans[2].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS3(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[3].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[3].setElement(element);

			   synchronized(trans[3].WaitTrans[3]){
				trans[3].WaitTrans[3].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			   /*
			try {
				transthread[3].join();
			} catch (InterruptedException e) {
				System.out.println("TRANS同步异常");
				Test_Point[xx][1]="FAULT";
				//e.printStackTrace();
			}
			*/
			if(!trans[3].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS4(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[4].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[4].setElement(element);

			   synchronized(trans[4].WaitTrans[4]){
				trans[4].WaitTrans[4].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			   /*
			try {
				transthread[4].join();
			} catch (InterruptedException e) {
				System.out.println("TRANS同步异常");
				Test_Point[xx][1]="FAULT";
				//e.printStackTrace();
			}
			*/
			if(!trans[4].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS5(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[5].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[5].setElement(element);

			   synchronized(trans[5].WaitTrans[5]){
				trans[5].WaitTrans[5].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			   /*
			try {
				transthread[5].join();
			} catch (InterruptedException e) {
				System.out.println("TRANS同步异常");
				Test_Point[xx][1]="FAULT";
				//e.printStackTrace();
			}
			*/
			if(!trans[5].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS6(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[6].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[6].setElement(element);

			   synchronized(trans[6].WaitTrans[6]){
				trans[6].WaitTrans[6].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			   /*
			try {
				transthread[6].join();
			} catch (InterruptedException e) {
				System.out.println("TRANS同步异常");
				Test_Point[xx][1]="FAULT";
				//e.printStackTrace();
			}*/
			if(!trans[6].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean TRANS7(Element element){
			String strUid = element.elementText("UID");
			if(strUid!=null){
				strUid = strUid.trim();
				String strPwd = element.elementText("PWD").trim();
				String strDb = element.elementText("DATABASE").trim();
				try {
					GdmConnection conn = ConnectionContext.getConnection("usertable");
					transConnList.add(conn);
					DbOperator dbOperator = new DbOperator(conn);
					trans[7].setDbOperator(dbOperator);
				} catch (GdmException e) {
					Test_Point[xx][1]="FAULT";
				}
			}
			trans[7].setElement(element);
			   synchronized(trans[7].WaitTrans[7]){
				trans[7].WaitTrans[7].notify();
				}
			   try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {

					e1.printStackTrace();
				}
			if(!trans[7].getreault())
				Test_Point[xx][1]="FAULT";
			return true;
		}
		public boolean SLEEP(Element element){
			String str = element.getText().trim();
			long time = Long.valueOf(str);
			try {
				Thread.sleep(time*5);
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
			return true;
		}
		// 2010-4-7添加
		public boolean WINDOWS(Element element){
			if(isLinuxOs==false){
				return ExecuteElement(element);
			}
			return true;
		}

		public boolean LINUX(Element element){
			if(isLinuxOs==true){
				return ExecuteElement(element);
			}
			return true;
		}
		//2011-3-29 性能测试需求，添加新关键字PSQL支持
		public boolean PSQL(Element element){
			String sql = element.elementText("SQL");
			if(sql.trim()==null||sql.trim().equals(""))
				return true;
			String cursql=replaceStr(sql);
			//显示正在执行的线程
			if(!(running==0))
			System.out.println("--THIS IS THREADS "+running);
			try {
				long starttime=System.nanoTime();
				effectRows = dbOperator.DirectExecute(cursql);
				usedtime=((double)(System.nanoTime()-starttime))/1000000;
				System.out.println("--sql:"+"\r"+ sql+"\n--影响了"+effectRows+"行");
				System.out.println("--耗费了"+usedtime+"ms");
			} catch (GdmException e) {
				//2010-12-22-YJM等待服务器重启
				boolean flag = false;
				if(e.getMessage().equals("网络通信异常")) {
					//exlog.errorWithTime("\r\n--********************\r\n--执行测试用例："+curFilePath);
					System.out.println(sql+"网络通信异常");
					Test_Point[xx][1]="FAULT";
					while(!InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getPort())) {
						InetOperator.isObjectAlive(ConfigInfo.getServer(), ConfigInfo.getDmServerPluginStartPort());
						try {
							flag = true;
							Thread.sleep(400);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					if(flag) {
						try {
							myConn = ConnectionContext.getConnection("usertable");
							dbOperator.setConn(myConn);
						} catch (GdmException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				if(("2").equalsIgnoreCase(ConfigInfo.getInfolevel()))
				{
					System.out.println("执行SQL语句失败：\r\n\t"+e.getMessage());
					if(!isinsqlcase){
						Test_Point[xx][1]="FAULT";
					   }else{
						   if(caseExpResult)
						   {
							casesqlcase=false;
							}
						   }
				}
				return false;
//				
			}
			mapForChange.put("@USEDTIMES", String.valueOf(usedtime));
			
			return true;
		}
		//2011-3-29 添加新关键字支持CALC_TIMETICKS、TOTALTIME、MINTIME、MAXTIME、AVGTIME支持
		public boolean CALC_TIMETICKS(Element element) {
			String str = element.getText().trim();
			String relStr = "";
			str = replaceStr(str);
			if (str==null||str.equals("")) {
				mapForChange.put("@USEDTIMES", "0");
			} else if(str.endsWith(" CLEAR")||str.endsWith(" clear"))
			{
				mapForChange.put(str.substring(6),"0");
				
			}else{
				relStr=String.valueOf(usedtime);
				mapForChange.put("@"+str, relStr);
			}
			return true;
		}
		public boolean TOTALTIME(Element element) {
			String str = element.getText().trim();
			String relStr = "";
			str = replaceStr(str);
			if (str.equals("clear")||str.equals("CLEAR")) {
				mapForChange.put("@TOTALTIME", "0");
			} else {
				relStr = mapForChange.get("@TOTALTIME");
				if(relStr==null||relStr.equals("")){
					relStr = "0";
				}
				relStr = Double.parseDouble(relStr.trim()) +usedtime+"";
				mapForChange.put("@TOTALTIME", relStr);
			}
			return true;
		}
		public boolean MAXTIME(Element element) {
			String str = element.getText().trim();
			String relStr = "";
			str = replaceStr(str);
			if (str.equals("clear")||str.equals("CLEAR")) {
				mapForChange.put("@MAXTIME", "0");
			} else {
				relStr = mapForChange.get("@MAXTIME");
				if(relStr==null||relStr.equals("")){
					relStr = "0";
				}
				if(Double.parseDouble(relStr.trim())<usedtime){
					relStr=String.valueOf(usedtime);
					mapForChange.put("@MAXTIME", relStr);
					}
			}
			return true;
		}
		public boolean MINTIME(Element element) {
			String str = element.getText().trim();
			String relStr = "";
			str = replaceStr(str);
			if (str.equals("clear")||str.equals("CLEAR")) {
				//这里认定最小时间不会大于999999999ms
				mapForChange.put("@MINTIME", "999999999");
			} else {
				relStr = mapForChange.get("@MINTIME");
				if(relStr==null||relStr.equals("")){
					relStr = "999999999";
				}
				if(Double.parseDouble(relStr.trim())>usedtime){
					relStr=String.valueOf(usedtime);
					mapForChange.put("@MINTIME", relStr);
					}
			}
			return true;
		}
		public boolean AVGTIME(Element element) {
			String str = element.getText().trim();
			String relStr = "";
			str = replaceStr(str);
			if (str.equals("clear")||str.equals("CLEAR")) {
				mapForChange.put("@AVGTIME", "0");
			} else {
				relStr = mapForChange.get("@AVGTIME");
				if(relStr==null||relStr.equals("")){
					relStr = "0";
				}
				//此处限定LOOP为正序循环，且从1开始
				if(loopCount==0||timesIndex[loopCount]==0){
					relStr=String.valueOf(usedtime);
				}else
					relStr=String.valueOf((usedtime+Double.parseDouble(relStr)*(timesIndex[loopCount]-1))/timesIndex[loopCount]);
				mapForChange.put("@AVGTIME", relStr);
			}
			return true;
		}
		public String getdmini(String filePath, String valName) throws IOException{
				FileReader fr = new FileReader(filePath);
				BufferedReader br = new BufferedReader(fr);
				String strLine = "";
				while(br.ready()){
					strLine = br.readLine();
					if(strLine.indexOf(valName)>-1){
						String temp = strLine.substring(strLine.indexOf('=')+1);
						if(temp.indexOf('#')>-1){
							temp = temp.substring(0, temp.indexOf('#'));
						}
						return temp.trim();
					}
				}
			return null;
		}
		
		public String replaceStr(String str){
			Iterator<String> iterator = mapForChange.keySet().iterator();
			ArrayList<String> tmpList = new ArrayList<String>();			// 用来存放HashMap里的Key，最后排序一下，方便替换
			String temp = "";
			while(iterator.hasNext()){
				temp = iterator.next();
				tmpList.add(temp);
			}
			Collections.sort(tmpList);
			
			// 先处理，避免@SERVER替换了@SERVERPATH
			temp = mapForChange.get("@SERVERPATH");
//			System.out.println(temp);
//			str = str.replaceAll("@SERVERPATH", mapForChange.get("@SERVERPATH"));
			if(str.indexOf("@SERVERPATH")>-1){
				str = mapForChange.get("@SERVERPATH")+str.substring(str.indexOf(filePathSepartor)); 
			}
			if(str.indexOf("@PATH")>-1){
				str = mapForChange.get("@PATH")+str.substring(str.indexOf(filePathSepartor));
			}
//			str = str.replaceAll("@SQLSTR1", mapForChange.get("@SQLSTR1"));
//			str = str.replaceAll("@SQLSTR2", mapForChange.get("@SQLSTR2"));
			int len = tmpList.size();
			int i = len-1;
			for(i=len-1;i>=0;i--){
				str = str.replace(tmpList.get(i), mapForChange.get(tmpList.get(i)));
			}
//			while(iterator.hasNext()){
//				temp = iterator.next();
//				str = str.replaceAll(temp, mapForChange.get(temp));
//			}
			//2010-3-2 处理@TIMES @_TIMES
			int indexT = str.indexOf("@");
			while (indexT != -1) {
				String tempHead = str.substring(0, indexT);
				int tempTime = str.indexOf("TIMES");
				if(tempTime<0){
					return str;
				}
				int loopIndex = loopCount - (tempTime - indexT) + 1; // 取mapForChange里的
				// @1TIMES 和
				// @2TIMES
				String tempTimeStr = mapForChange.get("@" + loopIndex + "TIMES");
				String tempLast = str.substring(tempTime + 5); // TIMES.length = 5
				str = tempHead + tempTimeStr + tempLast;
				indexT = str.indexOf("@");
			}
			
			return str;
		}
		//根据<TYPE>内字符串得到TYPE值
		public int getType(String type) {
			if (type.equals("DIRECT_EXECUTE_IGNORE"))
				return DIRECT_EXECUTE_IGNORE;
			else if (type.equals("DIRECT_EXECUTE_SUCCESS"))
				return DIRECT_EXECUTE_SUCCESS;
			else if (type.equals("DIRECT_EXECUTE_FAIL"))
				return DIRECT_EXECUTE_FAIL;
			else if(type.equals("DIRECT_EXECUTE_SELECT_WITH_RESULT"))
				return DIRECT_EXECUTE_SELECT_WITH_RESULT;
			else if(type.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT"))
				return DIRECT_EXECUTE_SELECT_COMPARE_RESULT;
			else if(type.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL"))
				return DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL;
			else if(type.equals("LOGIN_SUCCESS"))
				return LOGIN_SUCCESS;
			else if(type.equals("LOGIN_FAIL"))
				return LOGIN_FAIL;
			else 
				return -1;
		}
	}


	@Override
	public void run() {
		//System.out.println("THIS IS THREADS "+running);
		//String curFilePath1=getcurFilePath();
		ExecuteXml(curFilePath);
		AutoTestConsole.threads_count[running][1]="done";
		
	}
}
