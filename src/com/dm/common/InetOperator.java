package com.dm.common;
/**
 * @auth ychao
 * @desc 网络操作工具类
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class InetOperator {
	private Socket			csocket		 = null;
	private Socket			ssocket		 = null;
	private ServerSocket 	serverSocket = null;
	
	/*------------------------client------------------------*/
	public boolean initCSocket(String host, int port) {
		boolean ret = false;
		
		try {
			csocket = new Socket(host, port);
			ret = true;
		} catch (Exception e) {
			System.out.println("csocket init fail...");
		}
		
		return ret;
	}
	
	public boolean destroyCSocket() {
		boolean ret = false;
		
		try {
			if(csocket != null) {
				csocket.close();
			}
			ret = true;
		} catch(Exception e) {
			System.out.println("csocket destroy fail...");
		}
		
		return ret;
	}
	
	public PrintWriter getClientSocketWriter() {
		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter(csocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("get csocket writer fail...");
		}
		
		return writer;
	}
	
	public BufferedReader getClientSocketReader() {
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
		} catch (IOException e) {
			System.out.println("get csocket reader fail...");
		}
		
		return reader;
	}
	
	/*------------------------server------------------------*/
	
	public boolean initServerSocket(int port) {
		boolean ret = false;
		
		try {
			serverSocket = new ServerSocket(port);
			ret = true;
		} catch(Exception e) {
			System.out.println("server socket init fail...");
		}
		return ret;
	}
	
	public boolean destroyServerSocket() {
		boolean ret = false;
		
		try {
			if(serverSocket != null) {
				serverSocket.close();
			}
			ret = true;
		} catch(Exception e) {
			System.out.println("server socket destroy fail...");
		}
		
		return ret;
	}
	
	public boolean initSSocket() {
		boolean ret = false;
		
		try {
			ssocket = serverSocket.accept();
			ret = true;
		} catch (IOException e) {
			System.out.println("ssocket init fail...");
		}
		
		return ret;
	}

	public boolean destroySSocket() {
		boolean ret = false;
		
		try {
			if(ssocket != null) {
				ssocket.close();
			}
			ret = true;
		} catch(Exception e) {
			System.out.println("ssocket destroy fail...");
		}
		
		return ret;
	}
	
	public PrintWriter getServerSocketWriter() {
		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter(ssocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("get ssocket writer fail...");
		}
		
		return writer;
	}
	
	public BufferedReader getServerSocketReader() {
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(ssocket.getInputStream()));
		} catch (IOException e) {
			System.out.println("get ssocket reader fail...");
		}
		
		return reader;
	}
	
	/*----------------------utils--------------------------*/
	
	public static boolean isObjectAlive(String host, String port1) {
		boolean ret 	= false;
		Socket  tsocket = null;
		int 	port = Integer.valueOf(port1);
		
		try {
			tsocket = new Socket(host, port);
			ret = true;
		} catch (Exception e) {
		} finally {
			try { 
				if(tsocket != null) tsocket.close();
			} catch(Exception e) {}
		}
		
		return ret;
	}
}
