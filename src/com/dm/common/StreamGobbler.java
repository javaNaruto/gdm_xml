/**
 * 为了解决Java中调用Window下exe的问题
 * 调用waitFor()等待返回结果的时候，要处理错误信息和输出信息
 */
package com.dm.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread
{
    InputStream is;
    public StreamGobbler(InputStream is)
    {
        this.is = is;
    }
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ( br.readLine() != null);
            } catch (IOException ioe)
              {
                ioe.printStackTrace();  
              }
    }
}
