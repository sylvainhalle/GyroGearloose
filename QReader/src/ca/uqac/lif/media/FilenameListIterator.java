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
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;

/**
 * Outputs images one by one, read from a list of filenames.
 * @author sylvain
 *
 */
public class FilenameListIterator implements Iterator<BufferedImage>
{
  /**
   * Internal iterator over the list of filenames 
   */
  protected Iterator<String> m_filenames;
  
  public FilenameListIterator(Collection<String> files)
  {
    super();
    m_filenames = files.iterator();
  }

  @Override
  public boolean hasNext()
  {
    return m_filenames.hasNext();
  }

  @Override
  public BufferedImage next()
  {
    try
    {
      return ImageIO.read(new File(m_filenames.next()));
    }
    catch (IOException e)
    {
      return null;
    }
  }

  @Override
  public void remove()
  {
    m_filenames.remove();
  }

}
