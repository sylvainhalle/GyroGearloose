/*
  QR Code manipulation and event processing
  Copyright (C) 2013 Sylvain Hallé
  
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;

/**
 * Provides methods to encode and decode QR codes. This class is
 * mostly an easy front-end to methods from the
 * <a href="code.google.com/p/zxing/">ZXing</a> library. QR codes
 * can be written directly to an image from a string by calling
 * static method {@link #writeQrCode}, and read in the same way by calling
 * {@link #readQrCode}. 
 * @author sylvain
 *
 */
public class ZXingReadWrite extends CodeReaderWriter
{
  /**
   * When set to true, will output to a temp directory the
   * image that is being decoded for analysis and debugging.
   * This severely impairs performance, and should be set to
   * false for normal use.
   */
  private static final boolean DEBUG_MODE = false;
  
  /**
   * A multi-format reader of the ZXing library, whose static method "read"
   * is called to decode QR codes. It is instantiated only once here to
   * improve performance. 
   */
  protected static final MultiFormatReader s_reader = new MultiFormatReader();
  
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
   * The error correction level to use when writing a QR code
   */
  protected ErrorCorrectionLevel m_errorCorrectionLevel = ErrorCorrectionLevel.L;
  
  /**
   * Write a QR code.
   * @param out The output stream where the image data will be written
   * @param data The character data that the code will contain
   * @param width The width of the resulting image
   * @param height The width of the resulting image
   * @throws WriterException
   * @throws IOException
   */
  @Override
  public void writeCode(OutputStream out, String data, int width, int height) throws WriterException, IOException
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
    BitMatrix matrix = s_writer.encode(data, m_format, width, height, hints);
    MatrixToImageWriter.writeToStream(matrix, "png", out);
  }

  /**
   * Generates an image from a byte matrix. This method is only used by
   * the debug mode.
   * @param matrix The matrix to take its data from
   * @param height The height of the resulting image
   * @param width The width of the resulting image
   * @return An image of type RGB
   */
  protected static BufferedImage matrixToImage(ByteMatrix matrix, int height, int width)
  {
    //generate an image from the byte matrix
    int matrix_height = matrix.getHeight();
    int matrix_width = matrix.getWidth();
    int width_step = width / matrix_width; 
    int height_step = height / matrix_height;
    height = matrix.getHeight() * height_step;
    width = matrix.getWidth() * width_step;

    byte[][] array = matrix.getArray();

    //create buffered image to draw to
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    //iterate through the matrix and draw the pixels to the image
    for (int y = 0; y < matrix_height; y++)
    {
      for (int y_i = 0; y_i < height_step; y_i++)
      {
        for (int x = 0; x < matrix_width; x++)
        { 
          for (int x_i = 0; x_i < width_step; x_i++)
          {
            int grayValue = array[y][x] & 0xff; 
            image.setRGB(x * width_step + x_i, y * height_step + y_i, (grayValue == 0 ? 0 : 0xFFFFFF));            
          }
        }
      }
    }	
    return image;
  }
  
  public void setErrorCorrectionLevel(ErrorCorrectionLevel level)
  {
    m_errorCorrectionLevel = level;
  }
  
  /**
   * Generates an image from a bit matrix. This method is only used by
   * the debug mode.
   * @param matrix The matrix to take its data from
   * @param height The height of the resulting image
   * @param width The width of the resulting image
   * @return An image of type RGB
   */
  protected static BufferedImage matrixToImage(BitMatrix matrix, int height, int width)
  {
    //generate an image from the bit matrix
    int matrix_height = matrix.getHeight();
    int matrix_width = matrix.getWidth();
    int width_step = width / matrix_width; 
    int height_step = height / matrix_height; 

    //create buffered image to draw to
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    //iterate through the matrix and draw the pixels to the image
    for (int y = 0; y < matrix_height; y++)
    {
      for (int y_i = 0; y_i < height_step; y_i++)
      {
        for (int x = 0; x < matrix_width; x++)
        { 
          for (int x_i = 0; x_i < width_step; x_i++)
          {
            boolean bit = matrix.get(x, y);
            int grayValue = 0;
            if (bit)
              grayValue = 255; 
            image.setRGB(x * width_step + x_i, y * height_step + y_i, (grayValue == 0 ? 0 : 0xFFFFFF));            
          }
        }
      }
    }   
    return image;    
  }
  
  /**
   * Reads a QR code, using the default image binarizer
   * @param in The input stream for the contents of the image to read
   * @return The (character) contents of the decoded QR code
   * @throws IOException Indicates a problem with reading the input stream
   * @throws ReaderException Indicates that no code could be
   *   read from the input image
   */
  public String readCode(BufferedImage bi) throws IOException, ReaderException
  { 
    GlobalHistogramBinarizer bin = new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(bi));
    BinaryBitmap binaryBitmap = new BinaryBitmap(bin);
    
    if (DEBUG_MODE)
    {
        ImageIO.write(matrixToImage(bin.getBlackMatrix(), bin.getHeight(), bin.getWidth()), "png", new java.io.File("/tmp/out.png"));
    }
    // Depending on the encoding used, some encoders need to have "hints"
    Map<DecodeHintType, Object> hints = setupHints();
    Result result = s_reader.decode(binaryBitmap, hints);
    return result.getText();  
  }
  
  @Override
  public String readCode(InputStream in) throws IOException, ReaderException
  {
    BufferedImage bi = ImageIO.read(in);
    return readCode(bi);
  }
  
  protected Map<DecodeHintType, Object> setupHints()
  {
    // Depending on the encoding used, some encoders need to have "hints"
    Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
    LinkedList<BarcodeFormat> formats = new LinkedList<BarcodeFormat>();
    formats.add(m_format);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    if (m_pureCode)
    {
      hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }
    switch (m_format)
    {
    case AZTEC:
      break;
    case QR_CODE:
      break;
    default:
      // Do nothing
      break;
    }
    return hints;
  }

  /**
   * Reads a QR code, using a simple threshold image binarizer.
   * Each pixel of the image is given a brightness value between 0 (black)
   * and 255 (white). A pixel will be converted to full black if below
   * the threshold value, and to full white if above the threshold value.
   * <p>
   * Guessing the threshold that maximizes the odds of properly decoding
   * an image is a hard task.
   * @param in The input stream for the contents of the image to read
   * @param threshold The threshold to be used for the binarizer (between 1
   *   and 255)
   * @return The (character) contents of the decoded QR code
   * @throws IOException Indicates a problem with reading the input stream
   * @throws ReaderException Indicates that no code could be
   *   read from the input image
   */
  public String readCode(InputStream in, int threshold) throws IOException, ReaderException
  {
    return readCode(ImageIO.read(in), threshold);      
  }
  
  /**
   * Reads a QR code, using a simple threshold image binarizer.
   * Each pixel of the image is given a brightness value between 0 (black)
   * and 255 (white). A pixel will be converted to full black if below
   * the threshold value, and to full white if above the threshold value.
   * <p>
   * Guessing the threshold that maximizes the odds of properly decoding
   * an image is a hard task. The {@link ThresholdGuesser} can provide
   * some help at the expense of much computation.
   * @param bi A buffered image to read from
   * @param threshold The threshold to be used for the binarizer (between 1
   *   and 255)
   * @return The (character) contents of the decoded QR code
   * @throws IOException Indicates a problem with reading the input stream
   * @throws ReaderException Indicates that no code could be
   *   read from the input image
   */
  public String readCode(BufferedImage bi, int threshold) throws ReaderException
  {
    ThresholdBinarizer bin = new ThresholdBinarizer(new BufferedImageLuminanceSource(bi), threshold);
    BinaryBitmap binaryBitmap = new BinaryBitmap(bin);
    Map<DecodeHintType, Object> hints = setupHints();
    Result result = s_reader.decode(binaryBitmap, hints);
    return result.getText();         
  }
}
