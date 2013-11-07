package ca.uqac.lif.qr;

import java.io.PrintStream;
import java.util.LinkedList;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.buffertannen.protocol.Sender;

public class FrameDecoder
{
  /**
   * The verbosity level used to display messages
   */
  protected int verbosity = 0;
  
  /**
   * Whether to process the received frames in the Buffer Tannen
   * receiver
   */
  protected boolean m_processEvents = true;
  
  /**
   * System time when first good frame was processed
   */
  protected long m_timeAtFirstFrame = 0;
  
  /**
   * System time when last frame required for decoding was processed
   */
  protected long m_timeAtLastFrame = 0; 
  
  /**
   * Total number of frames processed
   */
  protected int total_frames = 0;
  
  /**
   * Total size of frames received
   */
  protected int total_size = 0;
  
  /**
   * Total number of lost frames
   */
  protected int lost_frames = 0;
  
  /**
   * Total number of lost segments
   */
  protected int lost_segments = 0;
  
  /**
   * Total number of messages in communication
   */
  protected int total_messages = 0;
  
  /**
   * Whether to send messages to the output
   */
  protected boolean mute = true;
  
  /**
   * Number of files processed (?)
   */
  protected int num_files = 0;
  
  /**
   * System start time (used to estimate fps)
   */
  protected long start_time = 0;

  /**
   * List of booleans indicating the decoding status of the last n frames
   */
  protected LinkedList<Boolean> m_lastFrames;
  
  /**
   * The Buffer Tannen receiver to send the frames to
   */
  protected Receiver m_receiver;
  
  /**
   * The PrintStream to send statistics to
   */
  protected PrintStream m_statStream = System.err;
  
  /**
   * Whether to rewind in the print stream after printing
   * stats
   */
  protected boolean m_rewindStats = true;
  
  /**
   * The number of past frames to use when computing the instantaneous
   * link quality
   */
  protected int m_decodingWidth = 30;
  
  /**
   * The number of good frames in the interval window
   */
  protected int m_goodFramesInInterval = 0;
  
  /**
   * The last system time when results were displayed
   */
  protected long m_lastDisplayTime = -1;
  
  /**
   * The number of <em>nano</em>seconds between refreshes of the
   * statistics display
   */
  protected long m_displayRefreshInterval = 500000000;
  
  public FrameDecoder()
  {
    super();
    m_lastFrames = new LinkedList<Boolean>();
    reset();
  }
  
  public void setReceiver(Receiver recv)
  {
    m_receiver = recv;
  }
  
  /**
   * Resets the status of the frame decoder
   */
  public void reset()
  {
    total_frames = 0;
    total_messages = 0;
    total_size = 0;
    lost_frames = 0;
    lost_segments = 0;
    num_files = 0;
    start_time = System.nanoTime();
    m_lastFrames.clear();
  }
  
  /**
   * Number of frames per second
   */
  protected int fps = 30;
  
  public void setFrameRate(int rate)
  {
    fps = rate;
  }
  
  public void setNewFrame(String s)
  {
    BitSequence bs = null;
    if (s == null)
    {
      setNewFrame(bs);
      return;
    }
    bs = new BitSequence();
    try
    {
      bs.fromBase64(s);
    }
    catch (BitFormatException e)
    {
      bs = null;
    }
    setNewFrame(bs);
  }
  
  public void setNewFrame(BitSequence bs)
  {
    if (bs == null)
    {
      handleLostFrame();
      if (!m_processEvents)
      {
        printStatsInterval();
        return;
      }
    }
    else
    {
      handleGoodFrame();
      if (!m_processEvents)
      {
        printStatsInterval();
        return;
      }
      m_receiver.putBitSequence(bs);
      SchemaElement se = m_receiver.pollMessage();
      int lost_now = m_receiver.getMessageLostCount();
      while (se != null)
      {
        if (verbosity >= 3)
          System.err.println("Lost : " + lost_now);
        total_messages++;
        BitSequence t_bs = null;
        try
        {
          t_bs = se.toBitSequence();
        }
        catch (BitFormatException e)
        {
          // Do nothing
        }
        total_size += t_bs.size();
        for (int i = 0; i < lost_now - lost_segments; i++)
        {
          if (!mute)
            System.out.println("This message was lost");
          if (verbosity >= 2)
            System.err.println("Lost message " + total_messages);
        }
        lost_segments = lost_now;
        if (!mute)
          System.out.println(se.toString());
        se = m_receiver.pollMessage();
        lost_now = m_receiver.getMessageLostCount();
      }      
    }
    printStatsInterval();
  }
  
  protected void printStatsInterval()
  {
    long current_time = System.nanoTime();
    if (current_time - m_lastDisplayTime > m_displayRefreshInterval)
    {
      m_lastDisplayTime = current_time;
      printReadStatistics(m_rewindStats);
    }    
  }
  
  public SchemaElement pollMessage()
  {
    return m_receiver.pollMessage();
  }
  
  public BitSequence pollBinaryBuffer(int length)
  {
    return m_receiver.pollBinaryBuffer(length);
  }
  
  public void setProcessEvents(boolean b)
  {
    m_processEvents = b;
  }
  
  public String getResourceIdentifier()
  {
    return m_receiver.getResourceIdentifier();
  }
  
  protected void handleLostFrame()
  {
    lost_frames++;
    total_frames++;
    if (m_lastFrames.size() > m_decodingWidth)
    {
      if (m_lastFrames.peekFirst())
      {
        m_goodFramesInInterval--;
      }
      m_lastFrames.removeFirst();
    }
    m_lastFrames.addLast(false);
  }
  
  protected void handleGoodFrame()
  {
    if (m_timeAtFirstFrame == 0)
    {
      // Only for statistics
      m_timeAtFirstFrame = System.nanoTime();
    }
    total_frames++;
    if (m_lastFrames.size() > m_decodingWidth)
    {
      if (m_lastFrames.peekFirst())
      {
        m_goodFramesInInterval--;
      }
      m_lastFrames.removeFirst();
    }
    m_lastFrames.addLast(true);    
    m_goodFramesInInterval++;
  }
  
  protected static String valueToMeter(int value, int min, int max, int width)
  {
    StringBuilder out = new StringBuilder();
    out.append("[");
    int range = max - min;
    float frac = (float) (value - min) / (float) range;
    int symbols = (int)(frac * width);
    for (int i = 0; i < symbols; i++)
    {
      out.append("*");
    }
    for (int i = symbols; i < width; i++)
    {
      out.append(" ");
    }
    out.append("]");
    return out.toString();
  }
  
  protected String bufferStatusToMeter(boolean[] status, int width, int cursor_position)
  {
    StringBuilder out = new StringBuilder();
    out.append("[");
    float slot_width = (float) status.length / (float) width;
    int grand_total = 0;
    for (int i = 0; i < width; i++)
    {
      int sum = 0;
      int total = 0;
      boolean in_slot = false;
      for (int j = (int) (((float) i) * slot_width); j < (int) ((((float) i) + 1) * slot_width); j++)
      {
        total++;
        if (status[j])
        {
          sum++;
          grand_total++;
        }
        if (cursor_position == j)
        {
          in_slot = true;
        }
      }
      if (in_slot)
      {
        out.append(">");
      }
      float fraction = (float) sum / (float) total;
      if (fraction == 0)
      {
        out.append(" ");
      }
      else if (fraction < 0.5)
      {
        out.append(".");
      }
      else if (fraction < 1)
      {
        out.append(":");
      }
      else // fraction == 1
      {
        out.append("|");
      }
    }
    if (m_timeAtLastFrame == 0 && grand_total >= status.length)
    {
      m_timeAtLastFrame = System.nanoTime();
    }
    out.append("] " + grand_total * 100 / status.length + "% (" + grand_total + "/" + status.length + ")");
    return out.toString();
  }
  
  /**
   * Checked whether the received data, in lake mode, has been completely
   * retrieved and is ready to be polled
   * @return true if data can be polled; false otherwise
   */
  public boolean dataIsReady()
  {
    boolean[] status = m_receiver.getBufferStatus();
    if (status.length == 0)
    {
      return false;
    }
    for (boolean b : status)
    {
      if (!b)
        return false;
    }
    return true;
  }
  
  public void printReadStatistics(boolean rewind)
  {
    if (m_statStream == null)
      return;
    long end_time = System.nanoTime();
    int distinct_bits = m_receiver.getNumberOfDistinctBits();
    int raw_bits = m_receiver.getNumberOfRawBits();
    int lost_count = m_receiver.getMessageLostCount();
    int good_frames_total = total_frames - lost_frames;
    long processing_time_ms = (end_time - start_time) / 1000000;
    Sender.SendingMode mode = m_receiver.getSendingMode();
    m_statStream.println("----------------------------------------------------");
    if (mode == Sender.SendingMode.LAKE)
    {
      m_statStream.println(" Sending mode:       lake          ");
      if (m_processEvents)
      {
        m_statStream.printf(" Buffer state:       %s\n", bufferStatusToMeter(m_receiver.getBufferStatus(), 20, m_receiver.getLastSegmentNumberSeen()));
      }
    }
    else
    {
      m_statStream.println(" Sending mode:       stream        ");
    }
    m_statStream.printf (" Progress:           %04d/%04d (%02.1f sec. @%d fps)     \n", total_frames, num_files, (float) total_frames / (float) fps, fps);
    m_statStream.printf(" Link quality:       %02d/%02d %s (%3d%%) Global: %4d/%4d (%3d%%)      \n", m_goodFramesInInterval, m_decodingWidth, valueToMeter(m_goodFramesInInterval * 100 / m_decodingWidth, 0, 100, 10), m_goodFramesInInterval * 100 / m_decodingWidth, good_frames_total, total_frames, good_frames_total * 100 / Math.max(1, total_frames));
    if (m_processEvents)
    {
      m_statStream.printf(" Data stream index:  %s   \n", m_receiver.getDataStreamIndex());
      m_statStream.printf(" Resource ident.:    %s   \n", m_receiver.getResourceIdentifier());
      m_statStream.print(" Messages received:  " + total_messages + "/" + (total_messages + lost_count));
      if (total_messages + m_receiver.getMessageLostCount() > 0)
        m_statStream.println(" (" + (total_messages * 100 / (total_messages + lost_count)) + "%)     ");
      else
        m_statStream.println("     ");
      m_statStream.printf("   Message segments: %d (%d bits)      \n", m_receiver.getNumberOfMessageSegments(), m_receiver.getNumberOfMessageSegmentsBits());
      m_statStream.printf("   Delta segments:   %d (%d bits)      \n", m_receiver.getNumberOfDeltaSegments(), m_receiver.getNumberOfDeltaSegmentsBits());
      m_statStream.printf("   Schema segments:  %d (%d bits)      \n", m_receiver.getNumberOfSchemaSegments(), m_receiver.getNumberOfSchemaSegmentsBits());
      m_statStream.printf(" Processing rate:    %d ms/frame ", processing_time_ms / Math.max(1, total_frames));
      if (processing_time_ms > 0)
        m_statStream.println("(" + (total_frames * 1000 / processing_time_ms) + " fps)     ");
      else
        m_statStream.println("     ");
      m_statStream.println(" Bandwidth:");
      m_statStream.println("   Raw:              " + raw_bits + " bits (" + raw_bits * fps / Math.max(1, total_frames) + " bits/sec.)     ");
      //m_statStream.println("   Actual:           " + distinct_bits + " bits (" + distinct_bits * fps / Math.max(1, total_frames) + " bits/sec.)     ");
      
      if (mode == Sender.SendingMode.LAKE)
      {
        float time = 0;
        if (m_timeAtLastFrame != 0)
        {
          time = ((float)(m_timeAtLastFrame - m_timeAtFirstFrame)) / 1000000000f;
        }
        else
        {
          time = ((float)(System.nanoTime() - m_timeAtFirstFrame)) / 1000000000f;
          
        }
        m_statStream.printf("   Effective:        %d bits in %d s (%d bps)    \n", distinct_bits, (int) time, (int)(((float)distinct_bits) / time));
      }
      else
      {
        m_statStream.println("   Effective:        " + total_size + " bits (" + total_size * fps / Math.max(1, total_frames) + " bits/sec.)     ");
      }
    }
    m_statStream.println("----------------------------------------------------\n");  
    // Move cursor up 15 lines (this is the number of lines written)
    if (rewind)
    {
      if (m_processEvents)
      {
        if (mode == Sender.SendingMode.LAKE)
        {
          m_statStream.print("\u001B[17A\r");
        }
        else
        {
          m_statStream.print("\u001B[16A\r");
        }
      }
      else
        m_statStream.print("\u001B[6A\r");
    }
  }
}
