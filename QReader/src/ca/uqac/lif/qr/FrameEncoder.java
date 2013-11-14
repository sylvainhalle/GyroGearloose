/*
  QR Code manipulation and event processing
  Copyright (C) 2008-2013 Sylvain Hallé

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uqac.lif.qr;

import java.io.InputStream;
import java.io.PrintStream;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.Sender;

public abstract class FrameEncoder
{
  /**
   * The stream to read from
   */
  protected InputStream m_inStream;
  
  /**
   * The BufferTannen sender
   */
  protected Sender m_sender;
  
  /**
   * The last system time when results were displayed
   */
  protected long m_lastDisplayTime = -1;
  
  /**
   * Whether to rewind in the print stream after printing
   * stats
   */
  protected boolean m_rewindStats = true;
  
  /**
   * The number of <em>nano</em>seconds between refreshes of the
   * statistics display
   */
  protected long m_displayRefreshInterval = 500000000;
  
  /**
   * The frame rate used to display the codes
   */
  protected int m_frameRate = 0;
  
  /**
   * Whether encountering the end-of-file signal indicates we should
   * stop processing
   */
  protected boolean m_endOnEof = true;
  
  /**
   * The size (in bytes) of the chunks to read from the input
   * source
   */
  protected int m_chunkByteSize = 16384;
  
  /**
   * Time interval between last two frames sent
   */
  protected long m_lastFrameInterval = 0;
  
  /**
   * System time when last frame was sent
   */
  protected long m_timeLastFrame = 0;
  
  /**
   * Set the BufferTannen sender to use in the exchange
   * @param sender The sender
   */
  public void setSender(Sender sender)
  {
    m_sender = sender;
  }
  
  public void setInputStream(InputStream is)
  {
    m_inStream = is;
  }
  
  public void setFramerate(int fps)
  {
    m_frameRate = fps;
  }
  
  public void setEndOnEof(boolean b)
  {
    m_endOnEof = false;
  }
  
  public BitSequence pollNextFrame()
  {
    if (m_sender.getSendingMode() == Sender.SendingMode.LAKE)
    {
      return pollNextFrameLake();
    }
    return pollNextFrameStream();
  }
  
  protected abstract BitSequence pollNextFrameLake();
  
  protected abstract BitSequence pollNextFrameStream();
  
  public void printStatsInterval()
  {
    long current_time = System.nanoTime();
    if (current_time - m_lastDisplayTime > m_displayRefreshInterval)
    {
      m_lastDisplayTime = current_time;
      printWriteStatistics(System.err, m_rewindStats);
    }    
  }
  
  public final void printWriteStatistics(PrintStream out)
  {
    printWriteStatistics(out, true);
  }
  
  public void printWriteStatistics(PrintStream out, boolean rewind)
  {
    int lines = 0;
    int raw_bits = m_sender.getNumberOfRawBits();
    int total_frames = m_sender.getNumberOfFrames();
    int total_size = m_sender.getNumberOfDeltaSegmentsBits() + m_sender.getNumberOfMessageSegmentsBits() + m_sender.getNumberOfBlobSegmentsBits();
    String sending_mode = "Stream";
    if (m_sender.getSendingMode() == Sender.SendingMode.LAKE)
      sending_mode = "Lake";
    out.printf("----------------------------------------------\n"); lines++;
    out.printf(" Sending mode:          %s\n", sending_mode); lines++;
    out.printf(" Frames sent:           %03d (%02.1f sec.)      \n", total_frames, (float) total_frames * (float) m_frameRate); lines++;
    out.printf(" Frame rate:            %02d (nominal) %02.1f fps (actual)      \n", m_frameRate, 1000000000f / (float) Math.max(1, m_lastFrameInterval)); lines++;
    out.printf(" Buffer state:          %03d bits (%d segments)      \n", m_sender.getBufferSizeBits(), m_sender.getBufferSizeSegments()); lines++;
    int message_segments = m_sender.getNumberOfMessageSegments();
    int message_segments_bits = m_sender.getNumberOfMessageSegmentsBits();
    int schema_segments = m_sender.getNumberOfSchemaSegments();
    int schema_segments_bits = m_sender.getNumberOfSchemaSegmentsBits();
    int delta_segments = m_sender.getNumberOfDeltaSegments();
    int delta_segments_bits = m_sender.getNumberOfDeltaSegmentsBits();
    int blob_segments = m_sender.getNumberOfBlobSegments();
    int blob_segments_bits = m_sender.getNumberOfBlobSegmentsBits();
    out.println(" Messages sent:         " + (message_segments + delta_segments + blob_segments) + " (" + total_size + " bits)     "); lines++;
    out.println("   Message segments:    " + message_segments + " (" + message_segments_bits + " bits, " + message_segments_bits / Math.max(1, message_segments) + " bits/seg.)     "); lines++;
    out.println("   Delta segments:      " + delta_segments + " (" + delta_segments_bits + " bits, " + delta_segments_bits / Math.max(1, delta_segments) + " bits/seg.)     "); lines++;
    out.println("   Schema segments:     " + schema_segments + " (" + schema_segments_bits + " bits, " + schema_segments_bits / Math.max(1, schema_segments) + " bits/seg.)     "); lines++;
    out.println("   Blob segments:       " + blob_segments + " (" + blob_segments_bits + " bits, " + blob_segments_bits / Math.max(1, blob_segments) + " bits/seg.)     "); lines++;
    out.println(" Bandwidth:"); lines++;
    out.println("   Raw (with retrans.): "+ raw_bits + " bits (" + (raw_bits * total_frames) / m_frameRate + " bits/sec.)     "); lines++;
    out.println("   Actual:              " + total_size + " bits (" + (total_size * total_frames) / m_frameRate + " bits/sec.)     "); lines++;
    out.printf("----------------------------------------------\n"); lines++;
    if (rewind)
    {
      // Rewind in display to overwrite next time
      out.printf("\u001B[%dA", lines);
    }
  }
}
