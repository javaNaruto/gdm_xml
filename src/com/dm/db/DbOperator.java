/**
 * @author		��Ρ
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * @author ��Ρ
 *
 */
public class DbOperator {
	private SqlResult result;
	private Connection conn;
	
	private Statement stmt;
	private PreparedStatement pstmt;
	private ResultSet rs;
	private ResultSetMetaData metaData;
	
	public DbOperator(Connection conn){
		this.conn = conn;
		result = new SqlResult();
	}
	
	// ֱ��ִ��SQL��䣬���ִ��SQL���Ӱ�������
	public int DirectExecute(String sql) throws SQLException{
		stmt = conn.createStatement();
		stmt.execute(sql);
		int effectRows = stmt.getUpdateCount();
		// ����-1����ʾsql���Ϊselect���ͣ��������Ϊ�շ���1
		if(effectRows==-1){
			rs = stmt.getResultSet();
			if(rs.next()){
				effectRows = 1;
			}else{
				effectRows = 0;
			}
		}
		stmt.close();
		return effectRows;
	}
	
	public int GetResultRows(String sql) throws SQLException{
		stmt = conn.createStatement();
		rs = stmt.executeQuery(sql);
		int counts = 0;
		while(rs.next())
			counts++;
		return counts;
	}
	
	public SqlResult ExecuteQuery(String sql) throws SQLException{
		pstmt = conn.prepareStatement(sql);
		rs = pstmt.executeQuery();
		metaData = rs.getMetaData();
		result.setSql(sql);
		int columnLen = metaData.getColumnCount();
		ArrayList<String> columns = new ArrayList<String>();
		int i=0;
		for(i=0;i<columnLen;i++){
			columns.add(metaData.getColumnName(i+1));
		}
		result.setColumns(columns);
		
		ArrayList<ArrayList<String>> fullResult = new ArrayList<ArrayList<String>>();
		while(rs.next()){
			ArrayList<String> columnResult = new ArrayList<String>();
			for(i=0;i<columnLen;i++){
				String temp = rs.getString(i+1);			
				// 2010-3-18��� ���NULL���� 
				if(temp==null){
					columnResult.add("NULL");
				}else{
					columnResult.add(temp);
				}
			}
			fullResult.add(columnResult);
		}
		if(fullResult.size()>0)
			result.setEffectRows(1);
		result.setFullResult(fullResult);
		pstmt.close();
		return result;
	}
	// ��sql�������ʽֵ
	public String getExp(String sql) throws SQLException{
		String result = "";
		stmt = conn.createStatement();
		rs = stmt.executeQuery(sql);
		rs.next();
		result = rs.getString(1);
		return result;
	}
	
	public Connection getConn(){
		return this.conn;
	}
	public void setConn(Connection conn){
		this.conn = conn;
	}
	
	public void close()throws SQLException{
		this.conn.close();
	}
	
	public void openAutoCommit() throws SQLException{
		this.conn.setAutoCommit(true);
		this.stmt = conn.createStatement();
//		this.conn.setTransactionIsolation(level);
	}
	
	public void closeAutoCommit() throws SQLException{
		this.conn.setAutoCommit(false);
		this.stmt = conn.createStatement();
	}
	
	public void commit() throws SQLException{
		this.conn.commit();
	}
	
	public void rollback() throws SQLException{
		this.conn.rollback();
	}
}
