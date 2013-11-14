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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ZXingWriter
{
  /**
   * The barcode format used to write the code
   */
  protected BarcodeFormat m_format = BarcodeFormat.QR_CODE;
  
  /**
   * The error correction level to use when writing a QR code
   */
  protected ErrorCorrectionLevel m_errorCorrectionLevel = ErrorCorrectionLevel.L;
  
  /**
   * The dimensions of the code to generate (in pixels)
   */
  protected int m_codeSize = 300;
  
  /**
   * A multi-format writer of the ZXing library, whose static method "write"
   * is called to encode QR codes. It is instantiated only once here to
   * improve performance. 
   */
  protected static final MultiFormatWriter s_writer = new MultiFormatWriter();
  
  /**
   * The character set used to encode character inside a QR code.
   */
  protected static final String s_charset = "UTF-8";
  
  /**
   * Set the size of the codes to generate
   * @param size The cdoe size (in pixels)
   */
  public void setCodeSize(int size)
  {
    m_codeSize = size;
  }
  
  /**
   * Write a barcode.
   * @param data The character data that the code will contain
   * @return An image containig the code
   */
  public BufferedImage getCode(String data)
  {
    BufferedImage img = null;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try
    {
      writeCode(bos, data, m_codeSize, m_codeSize);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      img = ImageIO.read(bis);
    }
    catch (WriterException e)
    {
      e.printStackTrace();
      return null;
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return null;
    }
    return img;
  }
  
  /**
   * Write a QR code.
   * @param out The output stream where the image data will be written
   * @param data The character data that the code will contain
   * @param width The width of the resulting image
   * @param height The width of the resulting image
   * @throws WriterException
   * @throws IOException
   */
  protected void writeCode(OutputStream out, String data, int width, int height) throws WriterException, IOException
  {
    // Depending on the encoding used, some encoders need to have "hints"
    Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
    switch (m_format)
    {
    case AZTEC:
      hints.put(EncodeHintType.CHARACTER_SET, s_charset);
      hints.put(EncodeHintType.ERROR_CORRECTION, 25);
      break;
    case QR_CODE:
      hints.put(EncodeHintType.ERROR_CORRECTION, m_errorCorrectionLevel);
      break;
    default:
      // Do nothing
      break;
    }
    //hints.put(EncodeHintType.MARGIN, 4);
    BitMatrix matrix = s_writer.encode(data, m_format, m_codeSize, m_codeSize, hints);
    MatrixToImageWriter.writeToStream(matrix, "png", out);
  }
  
  public void setBarcodeFormat(BarcodeFormat format)
  {
    m_format = format;
  }
  
  public void setErrorCorrectionLevel(ErrorCorrectionLevel level)
  {
    m_errorCorrectionLevel = level;
  }

}
