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

import ca.uqac.info.buffertannen.message.BitSequence;

public class CodeWindowUpdater extends WindowUpdater
{
  /**
   * The encoder we shall periodically poll for new frames to send 
   */
  protected FrameEncoder m_encoder;
  
  /**
   * The ZXing writer used to produce the codes
   */
  protected ZXingWriter m_writer;
  
  public CodeWindowUpdater(FrameEncoder sender, ZXingWriter rw, int interval)
  {
    super(interval);
    m_encoder = sender;
    m_writer = rw;
  }
  
  @Override
  public synchronized LoopStatus actionLoop()
  {
    long time_beg = System.nanoTime();
    // Poll sender for a new image
    BitSequence bs = m_encoder.pollNextFrame();
    BufferedImage img = null;
    if (bs != null)
    {
      img = m_writer.getCode(bs.toBase64());
    }
    if (img != null)
    {
      // A new image was sent: update the window
      m_encoder.printStatsInterval();
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
      e.printStackTrace();
    }
    // We want to be called again
    return LoopStatus.ACTIVE;
  }
}
