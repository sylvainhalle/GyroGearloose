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

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

/**
 * Simple class that converts grayscale images into
 * black-and-white.  Each pixel of the image is given a brightness value
 * between 0 (black) and 255 (white). A pixel will be converted to full
 * black if below the threshold value, and to full white if above the
 * threshold value.
 * @author sylvain
 *
 */
public class ThresholdBinarizer extends Binarizer
{
  protected byte[] m_bytes = null;
  protected int m_height = 0;
  protected int m_width = 0;
  protected BitMatrix m_matrix;
  
  /**
   * Instantiates a binarizer from a luminance source (i.e.
   * a grayscale image), using the default threshold value of
   * 128.
   * @param source The image to read from
   */
  protected ThresholdBinarizer(LuminanceSource source)
  {
    this(source, 128);
  }
  
  /**
   * Instantiates a binarizer from a luminance source (i.e.
   * a grayscale image).
   * @param source The image to read from
   * @param threshold The threshold to use for the binarization
   *  (between 1 and 255)
   */
  protected ThresholdBinarizer(LuminanceSource source, int threshold)
  {
    super(source);
    m_height = source.getHeight();
    m_width = source.getWidth();
    m_bytes = source.getMatrix();
    m_matrix = new BitMatrix(m_width, m_height);
    for (int x = 0; x < m_width; x++)
    {
      for (int y = 0; y < m_height; y++)
      {
        int luminance = m_bytes[y * m_width + x] & 0xff;
        //if (luminance > threshold)
        if (256 - luminance > threshold)
        {
          m_matrix.set(x, y);
        }
      }
    }

  }

  @Override
  public BitArray getBlackRow(int y, BitArray row) throws NotFoundException
  {
    if (m_matrix == null || y > m_height)
    {
      throw NotFoundException.getNotFoundInstance();
    }
    m_matrix.getRow(y, row);
    return row;
  }

  @Override
  public BitMatrix getBlackMatrix() throws NotFoundException
  {
    return m_matrix;
  }

  @Override
  public Binarizer createBinarizer(LuminanceSource source)
  {
    return new ThresholdBinarizer(source);
  }

}
