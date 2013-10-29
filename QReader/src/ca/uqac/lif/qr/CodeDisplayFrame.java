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

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import javax.swing.*;


/**
 * Window used to display codes on the screen. When the windows has the focus,
 * some keys can be used to control it:
 * <table>
 * <tr><td>q</td><td>Quits the program</td></tr>
 * <tr><td>Space</td><td>Queries the buffer for the next frame, if any is available</td></tr>
 * <tr><td>s</td><td>Starts/stop the repeated querying of the buffer, at the framerate
 *   specified in the command-line parameters</td></tr>
 * </ul>
 * @author sylvain
 *
 */
public class CodeDisplayFrame extends JFrame
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
   * The thread responsible for updating the window
   */
  protected WindowUpdater m_updater;
  
  /**
   * The window's title
   */
  protected static final String TITLE = "Code display";
  
  /**
   * The cumulative number of frames displayed by the window
   */
  protected int m_frameCount = 0;
  
  public CodeDisplayFrame(WindowUpdater wu)
  {
    super(TITLE + " (0)");
    super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    m_imagePanel = new ImagePanel();
    m_imagePanel.setPreferredSize(new Dimension(300, 300));
    KeyListener kl = new KeyTrap();
    m_imagePanel.addKeyListener(kl);
    super.getContentPane().setBackground(Color.WHITE);
    Box box = new Box(BoxLayout.Y_AXIS);
    box.add(Box.createVerticalGlue());
    box.add(m_imagePanel);
    box.add(Box.createVerticalGlue());
    super.getContentPane().add(box, BorderLayout.CENTER);
    super.addKeyListener(kl);
    super.setLocationRelativeTo(null); 
    super.pack();
    m_updater = wu;
  }
  
  public void setImage(BufferedImage img)
  {
    m_imagePanel.setImage(img);
    super.repaint();
  }
  
  /**
   * Simple panel used to display an image
   * @author sylvain
   *
   */
  protected class ImagePanel extends JPanel
  {
    /**
     * Dummy UID
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * The image being displayed by the panel
     */
    protected BufferedImage m_image;
    
    public ImagePanel()
    {
      super();
    }
    
    public void setImage(BufferedImage img)
    {
      m_image = img;
      int height = img.getHeight();
      int width = img.getWidth();
      Dimension dim = new Dimension(width, height);
      super.setPreferredSize(dim);
      super.setMaximumSize(dim);
      super.setMinimumSize(dim);
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.drawImage(m_image, 0, 0, null); // see javadoc for more info on the parameters            
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
  
  protected class WindowEventTrap implements ComponentListener
  {
    @Override
    public void componentHidden(ComponentEvent arg0) {  }

    @Override
    public void componentMoved(ComponentEvent arg0) {  }

    @Override
    public void componentResized(ComponentEvent arg0)
    {
      
    }

    @Override
    public void componentShown(ComponentEvent arg0) {  }
    
  }
}
