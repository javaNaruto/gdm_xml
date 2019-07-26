/**
 * @author ����
 * @date 2010-3-15 TODO
 * @modify @name @date
 */
package com.dm.db;

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

    public MyConnectionPool(String url, String uid, String pwd)
            throws SQLException {
        //maxSize = Integer.parseInt(AutoTestConsole.config.getMaxConnect());
        this.url = url;
        this.uid = uid;
        this.pwd = pwd;
        queue = new LinkedList<>();
        System.out.println("url:" + url);
        Connection conn = DriverManager.getConnection(url, uid, pwd);
        MyConnection myConn = new MyConnection(conn);
        queue.offer(myConn);
    }

    // �˴��п�����ɵݹ�������
    //2011-3-15 �޸�MyConnection���췽����������ɵݹ����
    public synchronized MyConnection getConnection() throws SQLException,
            InterruptedException {
        if (!queue.isEmpty()) {
            return queue.poll();
        } else {
            Connection conn = DriverManager.getConnection(url, uid, pwd);
            MyConnection myConn = new MyConnection(conn);
            return myConn;
        }
    }

    public synchronized void close(MyConnection conn) {
        queue.offer(conn);
    }


}  
