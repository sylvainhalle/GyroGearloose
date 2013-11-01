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

import ca.uqac.info.util.StoppableRunnable;

public class WindowUpdater extends StoppableRunnable
{
  
  protected BtQrSender m_sender;
  
  protected int m_refreshInterval;
  
  protected CodeDisplayFrame m_window;
  
  public WindowUpdater(BtQrSender sender, int interval)
  {
    super();
    m_sender = sender;
    m_refreshInterval = interval;
  }
  
  public void setWindow(CodeDisplayFrame w)
  {
    m_window = w;
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
