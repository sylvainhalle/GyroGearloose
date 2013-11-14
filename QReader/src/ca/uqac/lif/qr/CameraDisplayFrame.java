package ca.uqac.lif.qr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import javax.swing.*;

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
   * The label that displays the code's contents, if any
   */
  protected JLabel m_codeContents;
  
  /**
   * The code updater that animates this window.
   */
  protected CameraWindowUpdater m_updater;
  
  /**
   * The window's title
   */
  protected static final String TITLE = "Camera capture";
  
  public CameraDisplayFrame(int width, int height)
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
    super.setLocationRelativeTo(null); 
    super.pack();
    super.addKeyListener(kl);    
  }
  
  public CameraDisplayFrame()
  {
    // Assume VGA as default resolution
    this(640, 480);
  }
  
  public void setUpdater(CameraWindowUpdater upd)
  {
    m_updater = upd;
  }
  
  public void setImage(BufferedImage img)
  {
    m_imagePanel.setImage(img);
  }
  
  public void setFrameContents(String contents)
  {
    if (contents != null)
    {
      super.setTitle(TITLE + " (good)");
    }
    else
    {
      super.setTitle(TITLE + " (bad)");
      m_codeContents.setText(" ");
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
        m_updater.setProcessEvents(m_processEvents);
        break;
      case 'r':
      case 'R':
        // Reset statistics
        break;
      case 'q':
      case 'Q':
        // Quit by closing containing class (the JFrame)
        //CodeDisplayFrame.this.dispose();
        m_updater.stop();
        break;
      default:
        // Do nothing
        break;
      }
    }
  }
}
