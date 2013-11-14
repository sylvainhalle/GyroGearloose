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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;

public class ZXingReader
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
   * The binarization threshold used to convert the images to
   * black and white
   */
  protected int m_binarizationThreshold = 128;
  
  /**
   * A multi-format reader of the ZXing library, whose static method "read"
   * is called to decode QR codes. It is instantiated only once here to
   * improve performance. 
   */
  protected static final MultiFormatReader s_reader = new MultiFormatReader();
  
  /**
   * The character set used to encode character inside a QR code.
   */
  protected static final String s_charset = "UTF-8";
  
  /**
   * If decoding fails using threshold binarizer, whether to retry with
   * other binarization thresholds
   */
  protected boolean m_guessThreshold = false;
  
  /**
   * Whether to use the threshold binarizer
   */
  protected boolean m_useThresholdBinarizer = false;
  
  /**
   * Minimum threshold value when guessing
   */
  protected static final int THRESHOLD_MIN = 60;
  
  /**
   * Maximum threshold value when guessing
   */
  protected static final int THRESHOLD_MAX = 220;
  
  /**
   * Increment steps when guessing threshold
   */
  protected static final int THRESHOLD_INCREMENT = 10;
  
  /**
   * Sets whether to use the threshold binarizer or the generic
   * histogram binarizer.
   * @param b Set to true to use the threshold binarizer
   */
  public void useThresholdBinarizer(boolean b)
  {
    m_useThresholdBinarizer = b;
  }
  
  /**
   * Sets whether to try multiple binarization thresholds when decoding
   * fails. 
   * @param b Set to true to enable multiple tries
   */
  public void setGuessThreshold(boolean b)
  {
    m_guessThreshold = b;
  }
  
  /**
   * Set the threshold to use in the threshold binarizer.
   * @param threshold A threshold value between 0 and 255.
   */
  public void setBinarizationThreshold(int threshold)
  {
    m_binarizationThreshold = threshold;
  }
  
  /**
   * Reads a QR code, using the default image binarizer
   * @param in The input stream for the contents of the image to read
   * @return The (character) contents of the decoded QR code; null if no
   *   code could be read for some reason
   */
  public String readCode(BufferedImage img)
  {
    Result result = null;
    if (m_useThresholdBinarizer)
    {
      result = readCode(img, m_binarizationThreshold);
      if (result == null && m_guessThreshold)
      {
        // No success in decoding: try with other threshold values
        ThresholdGuesser guess = new ThresholdGuesser(this);
        guess.addImage(img);
        int suggested_threshold = guess.guessThreshold(THRESHOLD_MIN, THRESHOLD_MAX, THRESHOLD_INCREMENT, m_binarizationThreshold);
        if (suggested_threshold > 0)
        {
          // The guesser suggests a new value: try to decode again
          m_binarizationThreshold = suggested_threshold;
          result = readCode(img, m_binarizationThreshold);
        }
      }
    }
    else
    {
      GlobalHistogramBinarizer bin = new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(img));
      BinaryBitmap binaryBitmap = new BinaryBitmap(bin);
      Map<DecodeHintType, Object> hints = setupHints();
      try
      {
        result = s_reader.decode(binaryBitmap, hints);
      } 
      catch (NotFoundException e)
      {
        result = null;
      }
    }
    if (result != null)
    {
      return result.getText();
    }
    return null;
  }
  
  /*package*/ Result readCode(BufferedImage img, int threshold)
  {
    Result result = null;
    ThresholdBinarizer bin = new ThresholdBinarizer(new BufferedImageLuminanceSource(img), m_binarizationThreshold);
    BinaryBitmap binaryBitmap = new BinaryBitmap(bin);
    Map<DecodeHintType, Object> hints = setupHints();
    try
    {
      result = s_reader.decode(binaryBitmap, hints);
    }
    catch (NotFoundException e)
    {
      result = null;
    }
    return result;
  }

  /**
   * Setup the array of "hints" (i.e. parameters) used by the ZXing
   * decoder to decode images. This is only used internally and does not
   * need to be called directly from outside the class.
   * @return The map of hints
   */
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
