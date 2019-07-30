/**
 * @author		ÍõÎ¡
 * @date		2010-3-12
 * TODO
 * @modify		@name 		@date
 */
package com.dm.db;

import com.gdm.driver.GdmException;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author ÍõÎ¡
 * 
 */
public class MyConnManager {

	private HashMap<String, MyConnectionPool> poolMap;

	public MyConnManager(String table)
			throws SQLException {
		poolMap = new HashMap<String, MyConnectionPool>();
		MyConnectionPool connPool = new MyConnectionPool(table);
		poolMap.put(table, connPool);
	}

	public synchronized MyConnection getConnection(String table)
			throws GdmException, InterruptedException {
		MyConnectionPool connPool = poolMap.get(table);
		if (connPool != null) {
			return connPool.getConnection();
		} else {
			MyConnectionPool pool = new MyConnectionPool(table);
			poolMap.put(table, pool);
			return pool.getConnection();
		}
	}
}
