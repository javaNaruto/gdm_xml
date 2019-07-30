/**
 * @author		��Ρ
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.db;

import com.gdm.driver.GdmConnection;
import com.gdm.driver.GdmException;
import com.gdm.driver.GdmResultSet;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

/**
 * @author ��Ρ
 *
 */
public class DbOperator {
	private SqlResult result;
	private GdmConnection conn;
	
//	private Statement stmt;
//	private PreparedStatement pstmt;
	private GdmResultSet rs;
	private ResultSetMetaData metaData;
	
	public DbOperator(GdmConnection conn){
		this.conn = conn;
		result = new SqlResult();
	}
	
	// ֱ��ִ��SQL��䣬���ִ��SQL���Ӱ�������
	public int DirectExecute(String gql) throws GdmException{
//		stmt = conn.();
		GdmResultSet rs;
		int effectRows = 0;
		// ����-1����ʾsql���Ϊselect���ͣ��������Ϊ�շ���1
			rs = conn.execute(gql);
			if(rs.next()){
				effectRows = 1;
			}
//		conn.close();
		return effectRows;
	}
	
	public int GetResultRows(String gql) throws GdmException{
//		stmt = conn.createStatement();
		rs = conn.execute(gql);
		int counts = 0;
		while(rs.next())
			counts++;
		return counts;
	}
	
//	public SqlResult ExecuteQuery(String gql) throws GdmException{
//		pstmt = conn.prepareStatement(sql);
//		rs = conn.execute(gql);
//		metaData = rs.getMetaData();
//		result.setSql(sql);
//		int columnLen = metaData.getColumnCount();
//		ArrayList<String> columns = new ArrayList<String>();
//		int i=0;
//		for(i=0;i<columnLen;i++){
//			columns.add(metaData.getColumnName(i+1));
//		}
//		result.setColumns(columns);
//
//		ArrayList<ArrayList<String>> fullResult = new ArrayList<ArrayList<String>>();
//		while(rs.next()){
//			ArrayList<String> columnResult = new ArrayList<String>();
//			for(i=0;i<columnLen;i++){
//				String temp = rs.getString(i+1);
//				// 2010-3-18��� ���NULL����
//				if(temp==null){
//					columnResult.add("NULL");
//				}else{
//					columnResult.add(temp);
//				}
//			}
//			fullResult.add(columnResult);
//		}
//		if(fullResult.size()>0)
//			result.setEffectRows(1);
//		result.setFullResult(fullResult);
//		pstmt.close();
//		return result;
//	}
	// ��sql�������ʽֵ
//	public String getExp(String gql) throws GdmException{
//		String result = "";
//		stmt = conn.createStatement();
//		rs = conn.execute(gql);
//		rs.next();
//		result = rs.getString(1);
//		return result;
//	}
	
	public GdmConnection getConn(){
		return this.conn;
	}
	public void setConn(GdmConnection conn){
		this.conn = conn;
	}
	
	public void close()throws GdmException{
		this.conn.close();
	}
	
	public void openAutoCommit() throws GdmException{
		this.conn.setAutoCommit(true);
//		this.stmt = conn.createStatement();
//		this.conn.setTransactionIsolation(level);
	}
	
	public void closeAutoCommit() throws GdmException{
		this.conn.setAutoCommit(false);
//		this.stmt = conn.createStatement();
	}
	
	public void commit() throws GdmException {
		this.conn.commit();
	}
	
	public void rollback() throws GdmException{
		this.conn.rollback();
	}
}
