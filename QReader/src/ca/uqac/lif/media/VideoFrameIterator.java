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
package ca.uqac.lif.media;

import java.awt.image.BufferedImage;
import java.util.Iterator;

/**
 * Outputs images one by one, read from the frames of a video file.
 * @author sylvain
 *
 */
public class VideoFrameIterator implements Iterator<BufferedImage>
{
  protected boolean m_isNotOver;
  
  protected VideoFrameReader m_reader;
  
  public VideoFrameIterator(String video_filename)
  {
    this(new VideoFrameReader(video_filename));
  }
  
  public VideoFrameIterator(VideoFrameReader reader)
  {
    super();
    m_isNotOver = true;
    m_reader = reader;
  }
  
  @Override
  public boolean hasNext()
  {
    return m_isNotOver;
  }

  @Override
  public BufferedImage next()
  {
    BufferedImage out = null;
    try
    {
      out = m_reader.nextFrame();
    }
    catch (Exception e)
    {
      m_isNotOver = false;
    }
    if (out == null)
    {
      m_isNotOver = false;
    }
    return out;
  }

  @Override
  public void remove()
  {
    // Unsupported
  }

}
