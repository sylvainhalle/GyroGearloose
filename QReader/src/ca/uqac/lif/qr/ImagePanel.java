package ca.uqac.lif.qr;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Simple panel used to display an image
 * @author sylvain
 *
 */
public class ImagePanel extends JPanel
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