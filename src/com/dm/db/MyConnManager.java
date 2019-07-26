/**
 * @author		ÍõÎ¡
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.db;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author ÍõÎ¡
 * 
 */
public class MyConnManager {

	private HashMap<String, MyConnectionPool> poolMap;

	public MyConnManager(String dbUrl, String uid, String pwd)
			throws SQLException {
		poolMap = new HashMap<String, MyConnectionPool>();
		MyConnectionPool connPool = new MyConnectionPool(dbUrl, uid, pwd);
		poolMap.put(dbUrl + uid + pwd, connPool);
	}

	public synchronized MyConnection getConnection(String dbUrl, String uid, String pwd)
			throws SQLException, InterruptedException {
		MyConnectionPool connPool = poolMap.get(dbUrl + uid + pwd);
		if (connPool != null) {
			return connPool.getConnection();
		} else {
			MyConnectionPool pool = new MyConnectionPool(dbUrl, uid, pwd);
			poolMap.put(dbUrl + uid + pwd, pool);
			return pool.getConnection();
		}
	}
}
