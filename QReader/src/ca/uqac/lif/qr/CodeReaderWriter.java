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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ReaderException;
import com.google.zxing.WriterException;

/**
 * Provides methods to encode and decode QR codes. QR codes
 * can be written directly to an image from a string by calling
 * static method {@link #writeCode}, and read in the same way by calling
 * {@link #readCode}. 
 * @author sylvain
 *
 */
public abstract class CodeReaderWriter
{
  
  /**
   * The barcode format used to write the code
   */
  protected BarcodeFormat m_format = BarcodeFormat.QR_CODE;
  
  /**
   * Tells the reader whether the image to process is a pure
   * binary image of a code (as opposed to a <em>picture</em> of a code taken
   * with e.g. a camera)
   */
  protected boolean m_pureCode = false;
  
  /**
   * Write a QR code.
   * @param out The output stream where the image data will be written
   * @param data The character data that the code will contain
   * @param width The width of the resulting image
   * @param height The width of the resulting image
   * @throws WriterException
   * @throws IOException
   */
  public abstract void writeCode(OutputStream out, String data, int width, int height) throws WriterException, IOException;
  
  /**
   * Reads a QR code, using the default image binarizer
   * @param in The input stream for the contents of the image to read
   * @return The (character) contents of the decoded QR code
   * @throws IOException Indicates a problem with reading the input stream
   * @throws ReaderException Indicates that no code could be
   *   read from the input image
   */
  public abstract String readCode(InputStream in) throws IOException, ReaderException;
  
  /**
   * Tells the reader whether the image to process is a pure
   * binary image of a code (as opposed to a <em>picture</em> of a code taken
   * with e.g. a camera)
   * @param b True if image is a pure code, false otherwise
   */
  public void setPureCode(boolean b)
  {
    m_pureCode = b;
  }
  
  public void setBarcodeFormat(BarcodeFormat format)
  {
    m_format = format;
  }
}
