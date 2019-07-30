/**
 * @author 李鹏
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
 * @author 李鹏
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

    // 此处有可能造成递归调用溢出
    //2011-3-15 修改MyConnection构造方法不会再造成递归调用
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
