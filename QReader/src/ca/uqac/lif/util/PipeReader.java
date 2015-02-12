/******************************************************************************
Runtime monitor for pipe-based events
Copyright (C) 2013 Sylvain Halle et al.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/
package ca.uqac.lif.util;
import java.io.*;
import java.util.Scanner;

import ca.uqac.lif.util.PipeCallback.CallbackException;

/**
 * Class that reads a named pipe and notifies some other class whenever
 * complete tokens are ready to be processed. A token is delimited by
 * the "#" character, but this could be replaced by any other string.
 * The named pipe in question can be either a normal file or a true
 * named pipe.
 */
public class PipeReader implements Runnable
{
  protected TokenBuffer m_tokenBuffer;
  
  protected InputStream m_fis = null;
  
  /**
   * The size of chunks. The PipeReader will try to read this number
   * of bytes every time it queries the underlying input source.
   * Setting it to a too small value will cause the reader to loop
   * uselessly to process tiny bits of the string. Setting it to a
   * too large value (i.e. 1 MB) has an equally adverse effect.
   * Experimentally, a sweet spot seems to be 16 kB.
   */
  protected static final int m_chunkSize = 16384;
  
  /**
   * The interval that the reader should sleep
   * (i.e. wait) before polling the pipe again in the loop.
   * This interval is broken down in milliseconds + nanoseconds;
   * nano should not be over 999,999 (otherwise add 1 to milli).
   * You should tweak these values to avoid clogging your CPU
   * (setting them to 0 will hike it to 100%) while not lagging
   * on the input trace. See {@link setSleepInterval} for further
   * discussion.
   */
  protected int m_sleepIntervalMs = 0;
  protected int m_sleepIntervalNs = 100000;
  
  /**
   * Character indicating the closing of a pipe.
   * By default, we use ASCII 4, which is traditionally interpreted
   * as the <a href="http://en.wikipedia.org/wiki/End-of-transmission_character">end
   * of transmission character (EOT)</a>. This has no effect when the
   * underlying input is not a pipe. 
   */
  public static final String END_CHARACTER = String.valueOf((char) 4);

  /**
   * The object on which to call the {@link PipeCallback.notify}
   * method whenever a complete token has been received from
   * the underlying input stream
   */
  protected volatile PipeCallback<String> m_callback;

  /**
   * Remembers whether the underlying input stream is a file or
   * a pipe. This changes the condition to test to determine
   * if there is more data to read.
   */
  protected boolean m_isFile;
  
  /**
   * The pipe reader carries a "return code" that indicates
   * under which conditions the thread has stopped (normal
   * end or error of some kind)
   */
  protected int m_returnCode;
  public static final int ERR_OK = 0;
  public static final int ERR_THREAD = 1;
  public static final int ERR_EOF = 2;  // Encountered EOF (for a file)
  public static final int ERR_EOT = 3;  // Encountered EOT (for a pipe)

  /**
   * Simple main loop, used for testing that displays tokens to the screen
   * @param args
   */
  public static void main(String[] args)
  {
    String pipe_filename = "/tmp/mapipe"; // This must be created first with mkfifo
    PipeReader pr = new PipeReader(pipe_filename, new PipeReader.SimpleCallback());
    Thread th = new Thread(pr);
    System.out.println("Starting reader, type quit to quit...");
    th.start();
    Scanner sc = new Scanner(System.in, "UTF-8");
    sc.next();
    System.out.println("Terminated");
    th.interrupt();
    sc.close();
    System.exit(0);
  }

  public PipeReader()
  {
    super();
    m_tokenBuffer = new TokenBuffer();
    m_callback = null;
    m_returnCode = ERR_OK;
    m_isFile = true;
  }

  public PipeReader(String pipeName, PipeCallback<String> t, boolean isFile)
  {
    this();
    m_callback = t;
    m_isFile = isFile;
  }

  public PipeReader(String pipeName, PipeCallback<String> t)
  {
    this(pipeName, t, true);
  }
  
  public PipeReader(InputStream is, PipeCallback<String> t, boolean isFile)
  {
    this();
    m_callback = t;
    m_isFile = isFile;
    m_fis = is;
  }
  
  /**
   * Sets the sleep interval for this reader. The reader repeatedly
   * polls the underlying pipe for new characters in an infinite loop.
   * Setting a non-null sleep interval will make the reader take a
   * small pause between each iteration of the loop in order to decrease
   * CPU usage. A sleep interval of 0 will consume all the available
   * CPU by polling the pipe as fast as possible.
   * <p>The amount of data that can be processed by the reader per
   * unit of time can be estimated by:
   * <blockquote>
   * chunk size &div; sleep interval
   * </blockquote>
   * The default sleep interval (100µs) and chunk size (16 kB) 
   * allows for a throughput
   * of about 1.5 TB per second: this should be OK for most uses. ;-)
   * @param nanoseconds The number of nanoseconds to wait
   */
  public void setSleepInterval(long nanoseconds)
  {
    m_sleepIntervalMs = (int) (nanoseconds / 1000000f);
    m_sleepIntervalNs = (int) (nanoseconds % 1000000);
  }

  /**
   * Set the separator string that will indicate the end of a
   * token. By default the separator is the "#" symbol, but this
   * can be replaced by any string.
   * @param s The separator
   */
  public void setSeparator(String begin, String end)
  {
    m_tokenBuffer.setSeparators(begin, end);
  }

  @Override
  public void run()
  {
    InputStreamReader isr = null;
    BufferedReader br = null;
    try
    {
      isr = new InputStreamReader(m_fis, "UTF8");
      br = new BufferedReader(isr);
    }
    catch (UnsupportedEncodingException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    try
    {
      while (true)
      {
        try
        {
          Thread.sleep(m_sleepIntervalMs, m_sleepIntervalNs);
        }
        catch (InterruptedException e)
        {
          m_returnCode = ERR_THREAD;
          break;
        }
        if (br.ready())
        {
          char[] char_array = new char[m_chunkSize];
          @SuppressWarnings("unused")
          int chars_read = br.read(char_array, 0, m_chunkSize);
          // When the input is a pipe and we read the special character,
          // this indicates the end of transmission
          if (!m_isFile)
          {
            String st = new String(char_array);
            if (st.contains(END_CHARACTER))
            {
              m_returnCode = ERR_EOT;
              break;
            }
          }
          m_tokenBuffer.append(char_array);
          
          //m_bufferedContents.append((char)c);
          String tok = m_tokenBuffer.nextToken();
          while (!tok.isEmpty())
          {
            if (m_callback != null)
            {
              m_callback.notify(tok, m_fis.available());
            }
            tok = m_tokenBuffer.nextToken();
          } 
        }
        else
        {
          // If the underlying input source is not a pipe, the
          // fact that the input stream is not ready means there
          // is no more data to read.
          if (m_isFile)
          {
            m_returnCode = ERR_EOF;
            break;
          }
        }
      }
    }
    catch (IOException e)
    {
      // This will occur if the input stream is closed
      // Not an error in itself, but will cause the thread in which PipeReader
      // runs to end (gracefully)
    }
    catch (CallbackException e)
    {
      // A callback exception is thrown whenever some subprocess
      // of the current thread wants to abort processing. This generally
      // indicates a true error of some sort, so we display it.
      System.err.println(e);
      m_returnCode = ERR_THREAD;
    }
  }

  public int getReturnCode()
  {
    return m_returnCode;
  }

  /**
   * Simple TokenCallback: method notify simply prints the received token
   * to the screen.
   * @author sylvain
   *
   */
  protected static class SimpleCallback implements PipeCallback<String>
  {
    public void notify(String token)
    {
      System.out.println("Called> " + token);
    }
    
    public void notify(String token, long buffer_size)
    {
      notify(token);
    }
  }
}
