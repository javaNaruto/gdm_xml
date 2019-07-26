/**
 * @author		��Ρ
 * @date		2010-3-10
 * TODO
 * @modify		@name 		@date
 * ����						2010-3-10
 * ��δʹ��	
 */
package com.dm.db;

import java.util.ArrayList;

/**
 * @author ��Ρ
 * ����ִ��<SQL>�ڵ���һЩ��Ϣ
 * ����������������롢ִ��ʱ��...
 */
public class SqlResult {
	
	private int effectRows;					// Ӱ�������
	private int totalTime;					// ִ��ʱ��
	
	private ArrayList<ArrayList<String>> fullResult; // ���������
	private ArrayList<String> columns;				// ����Ϣ
	private String sql;								// ��ǰִ�е�SQL
//	private String exeResult;				// ִ������"SELECT 1+2;"�����Ľ�������ڼ���ĳЩ���ʽ
//	��������
	
	private int sqlState;					// ִ�б����ص�״̬��
	private int nerror;						// ִ�б����صĴ�����
	
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
