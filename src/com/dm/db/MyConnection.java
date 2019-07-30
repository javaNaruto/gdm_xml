/**
 * @author ÍõÎ¡
 * @date 2010-3-12 TODO
 * @modify        @name @date
 */
package com.dm.db;

import com.gdm.driver.GdmConnection;
import com.gdm.driver.GdmException;
import com.gdm.driver.GdmResultSet;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLXML;
//import java.sql.NClob;
import java.sql.PreparedStatement;
//import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
//import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
//import java.util.Arrays;
//import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author ÍõÎ¡
 *
 */
public class MyConnection  {

    private GdmConnection conn;

    public MyConnection(GdmConnection conn) throws GdmException {
        this.conn = conn;

    }

//	public void clearWarnings() throws SQLException {
//		this.conn.clearWarnings();
//	}

    public void close() throws GdmException {
        this.conn.close();
    }

    public List<Map<String, Object>> executeCypher(String s) {
        return null;
    }

    public List<Map<String, Object>> executeCypher(String s, Map<String, Object> map) {
        return null;
    }

    public void commit() throws GdmException {
        this.conn.commit();
    }


    public void rollback() throws GdmException {
        this.conn.rollback();
    }

    public String getGraph() {
        return null;
    }

    public void setGraph(String s) {

    }

    public GdmResultSet execute(String s) {
        return null;
    }

    public GdmResultSet execute(String s, Map<String, Object> map) {
        return null;
    }


    public void setAutoCommit(boolean autoCommit) throws GdmException {
        this.conn.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() {
        return false;
    }


    public Array createArrayOf(String typeName, Object[] elements)
            throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public Blob createBlob() throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public Clob createClob() throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
        public NClob createNClob() throws GdmException {
            // TODO Auto-generated method stub
            return null;
        }

        public SQLXML createSQLXML() throws GdmException {
            // TODO Auto-generated method stub
            return null;
        }
    */
    public Struct createStruct(String typeName, Object[] attributes)
            throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public Properties getClientInfo() throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getClientInfo(String name) throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isValid(int timeout) throws GdmException {
        // TODO Auto-generated method stub
        return false;
    }

    /*
        public void setClientInfo(Properties properties)
                throws SQLClientInfoException {
            // TODO Auto-generated method stub

        }

        public void setClientInfo(String name, String value)
                throws SQLClientInfoException {
            // TODO Auto-generated method stub

        }
    */
    public boolean isWrapperFor(Class<?> iface) throws GdmException {
        // TODO Auto-generated method stub
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public NClob createNClob() throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public SQLXML createSQLXML() throws GdmException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setClientInfo(Properties arg0) throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    public void setClientInfo(String arg0, String arg1)
            throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    public void setTypeMap(Map<String, Class<?>> arg0) throws GdmException {
        // TODO Auto-generated method stub

    }


}
