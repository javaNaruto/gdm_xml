/**
 * @author ����
 * @date 2010-3-15 TODO
 * @modify @name @date
 */
package com.dm.db;

import com.dm.connect.ConnectionContext;
import com.gdm.driver.GdmConnection;
import com.gdm.driver.GdmException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author ����
 *
 */
public class MyConnectionPool {
    //private int maxSize;
    private Queue<MyConnection> queue;
    private String url, uid, pwd;

    public MyConnectionPool(String table)
            throws GdmException {
        //maxSize = Integer.parseInt(AutoTestConsole.config.getMaxConnect());
        this.url = url;
        this.uid = uid;
        this.pwd = pwd;
        queue = new LinkedList<>();
        System.out.println("url:" + url);
        GdmConnection conn = ConnectionContext.getConnection(table);
        MyConnection myConn = new MyConnection(conn);
        queue.offer(myConn);
    }

    // �˴��п�����ɵݹ�������
    //2011-3-15 �޸�MyConnection���췽����������ɵݹ����
    public synchronized MyConnection getConnection() throws GdmException,
            InterruptedException {
        if (!queue.isEmpty()) {
            return queue.poll();
        } else {
            GdmConnection conn = ConnectionContext.getConnection("usertable");
            return new MyConnection(conn);
        }
    }

    public synchronized void close(MyConnection conn) {
        queue.offer(conn);
    }


}  
