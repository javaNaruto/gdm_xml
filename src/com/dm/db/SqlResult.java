/**
 * @author		王巍
 * @date		2010-3-10
 * TODO
 * @modify		@name 		@date
 * 新增						2010-3-10
 * 暂未使用	
 */
package com.dm.db;

import java.util.ArrayList;

/**
 * @author 王巍
 * 保存执行<SQL>节点后的一些信息
 * 包括结果集、错误码、执行时间...
 */
public class SqlResult {
	
	private int effectRows;					// 影响的行数
	private int totalTime;					// 执行时间
	
	private ArrayList<ArrayList<String>> fullResult; // 完整结果集
	private ArrayList<String> columns;				// 列信息
	private String sql;								// 当前执行的SQL
//	private String exeResult;				// 执行类似"SELECT 1+2;"的语句的结果，用于计算某些表达式
//	单独计算
	
	private int sqlState;					// 执行报错返回的状态码
	private int nerror;						// 执行报错返回的错误码
	
	public SqlResult(){
		fullResult = new ArrayList<ArrayList<String>>();
		columns = new ArrayList<String>();
	}

	public void setSql(String sql){
		this.sql = sql;
	}
	public String getSql(){
		return sql;
	}
	public int getEffectRows() {
		return effectRows;
	}

	public void setEffectRows(int effectRows) {
		this.effectRows = effectRows;
	}


	public int getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}

	public ArrayList<ArrayList<String>> getFullResult() {
		return fullResult;
	}

	public void setFullResult(ArrayList<ArrayList<String>> fullResult) {
		this.fullResult = fullResult;
	}

	public ArrayList<String> getColumns() {
		return columns;
	}

	public void setColumns(ArrayList<String> columns) {
		this.columns = columns;
	}

	public int getSqlState() {
		return sqlState;
	}

	public void setSqlState(int sqlState) {
		this.sqlState = sqlState;
	}

	public int getNerror() {
		return nerror;
	}

	public void setNerror(int nerror) {
		this.nerror = nerror;
	}
		
}
