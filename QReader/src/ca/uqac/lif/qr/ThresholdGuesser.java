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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import com.google.zxing.ReaderException;

/**
 * Provides functionalitities to find the best binarization threshold
 * to decode a set of images. Normal use is as follows:
 * <ol>
 * <li>One or more images are given the the ThresholdGuesser using
 *  {@link #addImage}</li>
 * <li>The guesser tries to find the threshold that maximizes the
 *   number of images decoded by calling one of the {@link #guessThreshold}
 *   methods</li>
 * </ol>
 * @author sylvain
 *
 */
public class ThresholdGuesser
{
  /**
   * A list of images used as a decoding sample
   */
  protected List<BufferedImage> m_sources;
  
  /**
   * An instance of QR code reader to use for the decoding
   */
  protected static final ZXingReadWrite s_decoder = new ZXingReadWrite();
  
  public ThresholdGuesser()
  {
    super();
    m_sources = new LinkedList<BufferedImage>();
  }
  
  /**
   * Add an image to the sample
   * @param f The image file to add
   */
  public void addImage(File f)
  {
    FileInputStream fis;
    try
    {
      fis = new FileInputStream(f);
      BufferedImage bi = ImageIO.read(fis);
      addImage(bi);
    }
    catch (IOException e)
    {
      // Do nothing
    }
  }
  
  /**
   * Add an image to the sample
   * @param img The image to add
   */
  public void addImage(BufferedImage img)
  {
    m_sources.add(img);
  }
  
  /**
   * Tries to guess an appropriate binarization threshold to decode QR codes
   * on the sample of images given beforehand.
   * The method iterates through various threshold values in an interval, and
   * each time tries to decode all images from the sample.
   * 
   * @param start Starting threshold. Should be between 1 and 255.
   * @param end Ending threshold. Should be between 1 and 255.
   * @param step Threshold increment on each iteration
   * @return The threshold value that maximizes the number of decoded images
   */
  public int guessThreshold(int start, int end, int step)
  {
    return guessThreshold(m_sources, start, end, step);
  }
  
  /**
   * Tries to guess an appropriate binarization threshold to decode QR codes
   * on the sample of images given beforehand.
   * The method iterates through various threshold values in an interval, and
   * each time tries to decode all images from the sample.
   * 
   * @param start Starting threshold. Should be between 1 and 255.
   * @param end Ending threshold. Should be between 1 and 255.
   * @param step Threshold increment on each iteration
   * @param starting_point The starting point to search from
   * @return The threshold value that maximizes the number of decoded images
   */
  public int guessThreshold(int start, int end, int step, int starting_point)
  {
    return guessThreshold(m_sources, start, end, step, starting_point);
  }
  
  /**
   * Tries to guess an appropriate binarization threshold to decode QR codes.
   * The method iterates through various threshold values in an interval, and
   * each time tries to decode all images from a list passed as an argument.
   * 
   * @param images The list of images to guess the threshold on
   * @param start Starting threshold. Should be between 1 and 255.
   * @param end Ending threshold. Should be between 1 and 255.
   * @param step Threshold increment on each iteration
   * @return The threshold value that maximizes the number of decoded images
   */
  public static int guessThreshold(List<BufferedImage> images, int start, int end, int step)
  {
    return guessThreshold(images, start, end, step, start);
  }
  
  /**
   * Tries to guess an appropriate binarization threshold to decode QR codes.
   * The method iterates through various threshold values in an interval, and
   * each time tries to decode all images from a list passed as an argument.
   * The method starts from a starting point x, and successively tries values
   * x+delta and x-delta within the interval. Hence if one suspects the proper
   * value to be near x, the function will find it faster than with a simple
   * linear scan of the interval.
   * 
   * @param images The list of images to guess the threshold on
   * @param start Starting threshold. Should be between 1 and 255.
   * @param end Ending threshold. Should be between 1 and 255.
   * @param step Threshold increment on each iteration
   * @param starting_point The starting point to search from
   * @return The threshold value that maximizes the number of decoded images
   */
  public static int guessThreshold(List<BufferedImage> images, int start, int end, int step, int starting_point)
  {
    int best_threshold = 0;
    int best_decoded = 0;
    for (int delta = step; starting_point - delta >= start || starting_point + delta <= end; delta += step)
    {
      // Try starting_point + delta
      int threshold = starting_point + delta;
      if (threshold <= end)
      {
        int num_decoded = countDecoded(images, threshold);
        if (num_decoded > best_decoded)
        {
          best_decoded = num_decoded;
          best_threshold = threshold;
        }
        if (num_decoded == images.size()) // All images were decoded; stop
        {
          break;
        }
      }
      
      // Try starting_point - delta
      threshold = starting_point - delta;
      if (threshold >= start)
      {
        int num_decoded = countDecoded(images, threshold);
        if (num_decoded > best_decoded)
        {
          best_decoded = num_decoded;
          best_threshold = threshold;
        }
        if (num_decoded == images.size()) // All images were decoded; stop
        {
          break;
        }        
      }
    }
    return best_threshold;
  }
  
  protected static int countDecoded(List<BufferedImage> images, int threshold)
  {
    int num_decoded = 0;
    String value = null;
    for (BufferedImage bi : images)
    {
      try
      {
        value = s_decoder.readCode(bi, threshold);
      }
      catch (ReaderException e)
      {
        // Nothing to do; this only means we could not decode
      }
      if (value != null)
      {
        num_decoded++;
      }
    }
    return num_decoded;
  }
}
