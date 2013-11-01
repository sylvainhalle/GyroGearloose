package ca.uqac.lif.qr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.*;

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
  }
  
  public void setReader(ZXingReadWrite reader)
  {
    m_codeReader = reader;
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
    if (contents != null)
    {
      super.setTitle(TITLE + " (good)");
      m_codeContents.setText(contents);
    }
    else
    {
      super.setTitle(TITLE + " (bad)");
      m_codeContents.setText(" ");
    }
    super.repaint();
  }
}
