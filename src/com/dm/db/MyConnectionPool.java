/**
 * @author 李鹏
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
 * @author 李鹏
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

    // 此处有可能造成递归调用溢出
    //2011-3-15 修改MyConnection构造方法不会再造成递归调用
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
