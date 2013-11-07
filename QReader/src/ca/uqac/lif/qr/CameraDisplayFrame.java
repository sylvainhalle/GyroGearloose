package ca.uqac.lif.qr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.*;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.lif.qr.CodeDisplayFrame.KeyTrap;

import com.google.zxing.ReaderException;

public class CameraDisplayFrame extends JFrame
{
  /**
   * Dummy UID
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Whether to send the frames to the BufferTannen reader
   */
  protected boolean m_processEvents = false;
  
  /**
   * The panel that will actually contain the image to display
   */
  protected ImagePanel m_imagePanel;
  
  /**
   * Whether a binary file has been written in lake mode
   */
  protected boolean file_written = false;
  
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
  protected FrameDecoder m_decoder;
  
  /**
   * The window's title
   */
  protected static final String TITLE = "Camera capture";
  
  public CameraDisplayFrame(WindowUpdater u, int width, int height)
  {
    super(TITLE);
    //super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    m_imagePanel = new ImagePanel();
    m_imagePanel.setPreferredSize(new Dimension(width, height));
    super.getContentPane().setBackground(Color.BLACK);
    super.getContentPane().add(m_imagePanel, BorderLayout.CENTER);
    KeyListener kl = new KeyTrap();
    m_imagePanel.addKeyListener(kl);
    m_codeContents = new JLabel();
    m_codeContents.setForeground(Color.WHITE);
    m_codeContents.setFont(m_codeContents.getFont().deriveFont(16f));
    super.getContentPane().add(m_codeContents, BorderLayout.SOUTH);
    m_updater = u;
    super.setLocationRelativeTo(null); 
    super.pack();
    super.addKeyListener(kl);    
  }
  
  public CameraDisplayFrame(WindowUpdater u)
  {
    this(u, 640, 480);
  }
  
  public void setReader(ZXingReadWrite reader)
  {
    m_codeReader = reader;
  }
  
  public void setDecoder(FrameDecoder dec)
  {
    m_decoder = dec;
  }
  
  public void setImage(BufferedImage img)
  {
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
    m_decoder.setNewFrame(contents);
    if (contents != null)
    {
      super.setTitle(TITLE + " (good)");
    }
    else
    {
      super.setTitle(TITLE + " (bad)");
      m_codeContents.setText(" ");
    }
    super.repaint();
    if (!file_written && m_decoder.dataIsReady())
    {
      // Save binary data to a file
      BitSequence bs = m_decoder.pollBinaryBuffer(-1);
      byte[] byte_contents = bs.toByteArray();
      try
      {
        String filename = m_decoder.getResourceIdentifier();
        FileOutputStream fos = new FileOutputStream(new File("/tmp/" + filename));
        fos.write(byte_contents);
        fos.close();
      } catch (FileNotFoundException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      file_written = true; // So that we don't save the file every time
    }
  }

  protected class KeyTrap implements KeyListener
  {
    @Override
    public void keyPressed(KeyEvent arg0) {  }

    @Override
    public void keyReleased(KeyEvent arg0) {  }

    @Override
    public void keyTyped(KeyEvent arg0)
    {
      char key = arg0.getKeyChar();
      switch (key)
      {
      case 's':
      case 'S':
        // Start/stop the automatic refresh
        m_updater.toggle();
        break;
      case 'p':
      case 'P':
        // Toggle processing of events
        m_processEvents = !m_processEvents;
        m_decoder.setProcessEvents(m_processEvents);
        break;
      case 'r':
      case 'R':
        // Reset statistics
        m_decoder.reset();
        break;
      case 'q':
      case 'Q':
        // Quit by closing containing class (the JFrame)
        //CodeDisplayFrame.this.dispose();
        System.exit(0);
        break;
      case ' ':
        m_updater.actionLoop();
        break;
      default:
        // Do nothing
        break;
      }
    }
  }
}
