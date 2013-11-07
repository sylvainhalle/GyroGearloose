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

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import ca.uqac.info.buffertannen.message.*;
import ca.uqac.info.buffertannen.protocol.*;
import ca.uqac.info.util.FileReadWrite;
import ca.uqac.info.util.PipeCallback;
import ca.uqac.info.util.PipeReader;
import ca.uqac.lif.media.GifAnimator;

public class BtQrSender
{
  /**
   * The BufferTannen sender used to build the frames
   */
  protected Sender m_sender;
  
  /**
   * Whether to display stats live
   */
  protected boolean m_displayStats = true;
  
  /**
   * A code writer
   */
  protected ZXingReadWrite m_readerWriter;
  
  /**
   * Whether this is the first frame displayed
   */
  protected boolean m_firstFrame;
  
  /**
   * The size of the images to draw
   */
  protected int m_imageSize = 300;
  
  /**
   * The system time when the last frame was produced
   */
  protected long m_time_last_frame = 0;
  
  /**
   * The error correction level to use when producing codes
   */
  ErrorCorrectionLevel m_level = ErrorCorrectionLevel.L;
  
  /**
   * The type of bar code to generate
   */
  BarcodeFormat m_format = BarcodeFormat.QR_CODE;
  
  /**
   * The delay (in 1/100s of a second) between each frame
   */
  protected int m_frameDelay = 13; // = 8 fps
  
  public BtQrSender(int frame_length, boolean display_stats, int image_size, int frame_delay, BarcodeFormat format, ErrorCorrectionLevel level, Sender.SendingMode sending_mode, boolean lake_loop)
  {
    super();
    m_sender = new Sender();
    m_sender.setFrameMaxLength(frame_length);
    m_sender.setSendingMode(sending_mode);
    m_sender.setLakeLoop(lake_loop);
    m_displayStats = display_stats;
    m_imageSize = image_size;
    m_frameDelay = frame_delay;
    m_format = format;
    m_level = level;
    m_readerWriter = new ZXingReadWrite();
    m_readerWriter.setBarcodeFormat(format);
    m_readerWriter.setErrorCorrectionLevel(level);
    m_firstFrame = true;
  }
  
  public void setResourceIdentifier(String s)
  {
    m_sender.setResourceIdentifier(s);
  }
  
  public void setDataStreamIndex(int index)
  {
    m_sender.setDataStreamIndex(index);
  }
  
  public BufferedImage pollNextImage()
  {
    BufferedImage image_out = null;
    BitSequence bs = m_sender.pollBitSequence();
    if (bs == null)
    {
      // Nothing to read from sender at this moment
      return image_out;
    }
    try
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      String base64_contents = bs.toBase64();
      m_readerWriter.writeCode(out, base64_contents, m_imageSize, m_imageSize);
      ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
      image_out = ImageIO.read(in);
    }
    catch (WriterException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (m_displayStats)
    {
      if (!m_firstFrame)
      {
        // Move cursor up 14 lines
        System.err.print("\u001B[14A\r");
      }
      else
      {
        m_firstFrame = false;
      }
      long current_time = System.nanoTime();
      printWriteStatistics(System.err, m_frameDelay, current_time - m_time_last_frame, m_sender.getSendingMode());
      m_time_last_frame = current_time;
    }
    return image_out;
  }
  
  protected void animate(String out_filename, int frame_rate, int image_size, BarcodeFormat format, ErrorCorrectionLevel level, Sender.SendingMode mode)
  {
    GifAnimator animator = createAnimation(frame_rate, image_size, format, level, mode);
    animator.getAnimation(frame_rate, out_filename);
  }
  
  protected byte[] animate(int frame_delay, int image_size, BarcodeFormat format, ErrorCorrectionLevel level, Sender.SendingMode mode)
  {
    GifAnimator animator = createAnimation(frame_delay, image_size, format, level, mode);
    return animator.getAnimation(frame_delay);
  }
  
  protected GifAnimator createAnimation(int frame_delay, int image_size, BarcodeFormat format, ErrorCorrectionLevel level, Sender.SendingMode mode)
  {
    GifAnimator animator = new GifAnimator();
    BitSequence bs = m_sender.pollBitSequence();
    ZXingReadWrite reader_writer = new ZXingReadWrite();
    reader_writer.setBarcodeFormat(format);
    reader_writer.setErrorCorrectionLevel(level);
    boolean first_file = true;
    long current_time = 0;
    while (bs != null)
    {
      current_time = System.nanoTime();
      try
      {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //System.out.println("Written: " + bs.toString());
        reader_writer.writeCode(out, bs.toBase64(), image_size, image_size);
        animator.addImage(out.toByteArray());
      } catch (WriterException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (m_displayStats)
      {
        if (!first_file)
        {
          // Move cursor up 12 lines
          System.err.print("\u001B[12A\r");
        }
        else
        {
          first_file = false;
        }
        printWriteStatistics(System.err, frame_delay, current_time - m_time_last_frame, mode);
      }
      m_time_last_frame = current_time;
      bs = m_sender.pollBitSequence();
    }
    printWriteStatistics(System.err, frame_delay, current_time - m_time_last_frame, mode);
    return animator;
  }
  
  protected void setSchema(int number, String file_contents) throws ReadException
  {
    m_sender.setSchema(number, file_contents);
  }
  
  protected void setSchema(int number, File f) throws IOException, ReadException
  {
    String contents = FileReadWrite.readFile(f);
    setSchema(number, contents);
  }
  
  protected void readMessages(File f)
  {
    try
    {
      String contents = FileReadWrite.readFile(f);
      readMessages(contents);
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * Instructs the sender to fetch its messages from some input stream
   * @param in The input stream to read from (can also be {@link System.in}
   */
  protected void readMessages(InputStream in, boolean is_file)
  {
    PipeReader pr = new PipeReader(new BufferedInputStream(in), new QrCallback(), is_file);
    pr.setSeparator("", "---");
    if (!is_file)
    {
      // When the stream is momentarily empty, take the opportunity to broadcast schema segments
      m_sender.setEmptyBufferIsEof(false);
    }
    Thread th = new Thread(pr);
    th.start();
  }
  
  protected void readBlob(File f)
  {
    try
    {
      FileInputStream fis = new FileInputStream(f);
      BufferedInputStream bis = new BufferedInputStream(fis);
      int totalBytesRead = 0;
      byte[] result = new byte[(int)f.length()];
      while(totalBytesRead < result.length)
      {
        int bytesRemaining = result.length - totalBytesRead;
        //input.read() returns -1, 0, or more :
        int bytesRead = bis.read(result, totalBytesRead, bytesRemaining); 
        if (bytesRead > 0)
        {
          totalBytesRead = totalBytesRead + bytesRead;
        }
      }
      // Split data into binary fragments
      int max_length = m_sender.getMaxBlobSize() - 100; // For safety, we trim 100 bits off advertised max length
      BitSequence seq = new BitSequence(result, result.length * 8);
      while (!seq.isEmpty())
      {
        BitSequence part = seq.truncatePrefix(max_length);
        m_sender.addBlob(part);
      }
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (BitFormatException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
  }
  
  /**
   * Simple callback that is called whenever the pipe reader has
   * processed a full message.
   * @author sylvain
   *
   */
  protected class QrCallback implements PipeCallback<String>
  { 
    @Override
    public void notify(String token, long buffer_size)
        throws ca.uqac.info.util.PipeCallback.CallbackException
    {
      BtQrSender.this.readMessage(token);
    }
  }
  
  /**
   * Read messages from a file
   * @param file_contents
   */
  protected void readMessages(String file_contents)
  {
    String[] parts = file_contents.split("\n---\n");
    for (String part : parts)
    {
      readMessage(part);
    }
  }
  
  /**
   * Processes a single message
   * @param part
   */
  protected void readMessage(String part)
  {
    part = part.trim();
    int first_space = part.indexOf(" ");
    if (first_space < 0)
      return;
    String left = part.substring(0, first_space);
    part = part.substring(first_space + 1);
    int schema_number = Integer.parseInt(left);
    try
    {
      m_sender.addMessage(schema_number, part);
    } catch (ReadException e)
    {
      // Ignore if cannot read
      System.err.println("Could not add message");
      return;
    }
    catch (UnknownSchemaException e)
    {
      System.err.println("Unknown schema");
      return;
    }    
  }
  
  protected void printWriteStatistics(PrintStream out, int frame_delay, long time_since_last_frame_ms, Sender.SendingMode mode)
  {
    int raw_bits = m_sender.getNumberOfRawBits();
    int total_frames = m_sender.getNumberOfFrames();
    int total_size = m_sender.getNumberOfDeltaSegmentsBits() + m_sender.getNumberOfMessageSegmentsBits();
    // In the following, we use Math.max to avoid divisions by zero when no segment is sent yet
    //out.printf("Written to %s                  \n", output_filename);
    out.println("----------------------------------------------------");
    out.print(" Sending mode:          ");
    if (mode == Sender.SendingMode.LAKE)
    {
      out.println("lake     ");
    }
    else
    {
      out.println("stream   ");
    }
    out.printf(" Frames sent:           %03d (%02.1f sec.)      \n", total_frames, (float) total_frames * (float) frame_delay / 100f);
    out.printf(" Frame rate:            %02d (nominal) %02.1f fps (actual)      \n", 100 / frame_delay, 1000000000f / (float) Math.max(1, time_since_last_frame_ms));
    out.printf(" Buffer state:          %03d bits (%d segments)      \n", m_sender.getBufferSizeBits(), m_sender.getBufferSizeSegments());
    out.println(" Messages sent:         " + (m_sender.getNumberOfMessageSegments() + m_sender.getNumberOfDeltaSegments()) + " (" + total_size + " bits)     ");
    out.println("   Message segments:    " + m_sender.getNumberOfMessageSegments() + " (" + m_sender.getNumberOfMessageSegmentsBits() + " bits, " + m_sender.getNumberOfMessageSegmentsBits() / Math.max(1, m_sender.getNumberOfMessageSegments()) + " bits/seg.)     ");
    out.println("   Delta segments:      " + m_sender.getNumberOfDeltaSegments() + " (" + m_sender.getNumberOfDeltaSegmentsBits() + " bits, " + m_sender.getNumberOfDeltaSegmentsBits() / Math.max(1, m_sender.getNumberOfDeltaSegments()) + " bits/seg.)     ");
    out.println("   Schema segments:     " + m_sender.getNumberOfSchemaSegments() + " (" + m_sender.getNumberOfSchemaSegmentsBits() + " bits, " + m_sender.getNumberOfSchemaSegmentsBits() / Math.max(1, m_sender.getNumberOfSchemaSegments()) + " bits/seg.)     ");
    out.println(" Bandwidth:");
    out.println("   Raw (with retrans.): "+ raw_bits + " bits (" + (raw_bits * frame_delay * total_frames) / 100 + " bits/sec.)     ");
    out.println("   Actual:              " + total_size + " bits (" + (total_size * frame_delay * total_frames) / 100 + " bits/sec.)     ");
    out.println("----------------------------------------------------\n");       
  }

}
