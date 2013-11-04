package ca.uqac.lif.qr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.*;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.protocol.Receiver;

import com.google.zxing.ReaderException;

public class CameraDisplayFrame extends JFrame
{

  /**
   * Dummy UID
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The panel that will actually contain the image to display
   */
  protected ImagePanel m_imagePanel;
  
  /**
   * The label that displays the code's contents, if any
   */
  protected JLabel m_codeContents;
  
  /**
   * The thread responsible for updating the window
   */
  protected WindowUpdater m_updater;
  
  /**
   * The code reader to be called to process the frames
   */
  protected ZXingReadWrite m_codeReader;
  
  /**
   * The BufferTannen receiver connected to the camera's frames
   */
  protected Receiver m_btReceiver;
  
  /**
   * The verbosity level used to display messages
   */
  protected int verbosity = 0;
  
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
   * Number of frames per second
   */
  protected int fps = 12;
  
  /**
   * The window's title
   */
  protected static final String TITLE = "Camera capture";
  
  public CameraDisplayFrame(WindowUpdater u)
  {
    super(TITLE);
    //super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    m_imagePanel = new ImagePanel();
    m_imagePanel.setPreferredSize(new Dimension(640, 480));
    super.getContentPane().setBackground(Color.BLACK);
    super.getContentPane().add(m_imagePanel, BorderLayout.CENTER);
    //JPanel text_panel = new JPanel();
    m_codeContents = new JLabel();
    m_codeContents.setForeground(Color.WHITE);
    m_codeContents.setFont(m_codeContents.getFont().deriveFont(16f));
    //text_panel.add(m_codeContents);
    super.getContentPane().add(m_codeContents, BorderLayout.SOUTH);
    m_updater = u;
    //super.getContentPane().add(box, BorderLayout.CENTER);
    super.setLocationRelativeTo(null); 
    super.pack();
    start_time = System.nanoTime();
  }
  
  public void setReader(ZXingReadWrite reader)
  {
    m_codeReader = reader;
  }
  
  public void setReceiver(Receiver receiver)
  {
    m_btReceiver = receiver;
  }
  
  public void setImage(BufferedImage img)
  {
    total_frames++;
    m_imagePanel.setImage(img);
    // Try to decode the image
    String contents = null;
    try
    {
      contents = m_codeReader.readCode(img);
    } catch (IOException e)
    {
      // Cannot read code
    }
    catch (ReaderException e)
    {
      // Cannot read code
    }
    if (contents != null)
    {
      super.setTitle(TITLE + " (good)");
      BitSequence bs = new BitSequence();
      try
      {
        bs.fromBase64(contents);
      } catch (BitFormatException e)
      {
        if (verbosity >= 2)
          System.err.println("Cannot decode frame " + (total_frames - 1));
        lost_frames++;
        return;
      }
      //System.err.printf("%4d/%4d (%2d%%)     \r", total_frames, num_files, (total_frames - lost_frames) * 100 / total_frames);
      m_btReceiver.putBitSequence(bs);
      SchemaElement se = m_btReceiver.pollMessage();
      int lost_now = m_btReceiver.getMessageLostCount();
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
        se = m_btReceiver.pollMessage();
        lost_now = m_btReceiver.getMessageLostCount();
      }
      BtQrReader.printReadStatistics(System.err, false, m_btReceiver, total_frames, total_size, lost_frames, total_messages, start_time, fps, num_files, true);
    }
    else
    {
      super.setTitle(TITLE + " (bad)");
      m_codeContents.setText(" ");
      lost_frames++;
      BtQrReader.printReadStatistics(System.err, false, m_btReceiver, total_frames, total_size, lost_frames, total_messages, start_time, fps, num_files, true);
    }
    super.repaint();
  }
}
