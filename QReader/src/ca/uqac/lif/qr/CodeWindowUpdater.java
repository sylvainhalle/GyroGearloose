package ca.uqac.lif.qr;

import java.awt.image.BufferedImage;

public class CodeWindowUpdater extends WindowUpdater
{
  
  protected BtQrSender m_sender;
  
  public CodeWindowUpdater(BtQrSender sender, int interval)
  {
    super(interval);
    m_sender = sender;
  }
  
  @Override
  public LoopStatus actionLoop()
  {
    long time_beg = System.nanoTime();
    // Poll sender for a new image
    BufferedImage img = m_sender.pollNextImage();
    if (img != null)
    {
      // A new image was sent: update the window
      m_window.setImage(img);
      m_window.repaint();
    }
    // Sleep a little while
    long time_now = System.nanoTime();
    int actual_refresh = Math.max(0, m_refreshInterval - ((int) ((time_now - time_beg) / 1000000f)));
    try
    {
      Thread.sleep(actual_refresh);
    }
    catch (InterruptedException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // We want to be called again
    return LoopStatus.ACTIVE;
  }
}
