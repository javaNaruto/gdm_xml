/*
 * @(#)ConnectionContext.java, 2018年10月15日 上午10:04:19
 *
 * Copyright (c) 2018-2018, 四川蜀天梦图数据科技有限公司.
 * All rights reserved.
 */
package com.dm.connect;

import com.gdm.driver.GdmConnection;
import com.gdm.driver.GdmConnectionManager;

//import com.gdm.driver.GdmException;

/**
 *
 * @author lvjiyun
 * @version 1.0, 2018年10月15日
 */
public final class ConnectionContext {
  private ConnectionContext() {
  }

  private static final ThreadLocal<String> GRAPH = new ThreadLocal<String>();

  public static void setGraph(String graph) {
    GRAPH.set(graph);
  }

  public static GdmConnection getConnection() {
    return getConnection(GRAPH.get());
  }

  public static GdmConnection getConnection(String graphName) {
//    ServletRequestAttributes requestAttributes =
//    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//    HttpServletRequest request = requestAttributes.getRequest();
//    HttpSession session = request.getSession();
//    SessionHolder holder = (SessionHolder) session.getAttribute("user");
//    if (graphName == null) {
//      graphName = holder.getDefaultGraph();
//    }

    GdmConnection connection = null;
//    if (graphName != null) {
//      connection = (GdmConnection) session.getAttribute("graph-" + graphName);
//    }
//    if (connection != null) {
//      try {
//        connection.execute("gdm.graph()");
//      } catch (GdmException e) {
//        if (e.getMessage().contains("当前连接已关闭") || e.getMessage().contains("The connection is closed")) {
//          connection = null;
//        }
//      }
//    }
    if (connection == null) {
//      String url = SystemConfig.getProperties().getProperty("gdm://localhost:8100?autoReconnect=true");
      String url = "gdm://localhost:8100?autoReconnect=true";
      if (graphName != null) {
        String params = null;

        int index = url.lastIndexOf("?");
        if (index != -1) {
          params = url.substring(index);
          url = url.substring(0, index);
        }
        index = url.indexOf('/', 7);
        if (index != -1) {
          url = url.substring(0, index);
        }
        url = url + "/" + graphName;
        if (params != null) {
          url = url + params;
        }
      }
      connection = GdmConnectionManager.getConnection(url, "SYSDBA", "SYSDBA");

//      session.setAttribute("graph-" + graphName, connection);
    }

    return connection;
  }
}
