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

import magick.*;

import java.util.*;

/**
 * Creates an animated Gif from a set of images.
 * Instructions taken from <a href="http://sourceforge.net/apps/mediawiki/jmagick/index.php?title=How_to_make_an_Animated_GIF_with_JMagick">SourceForge</a>.
 * Normal use is as follows:
 * <ol>
 * <li>Images are added to the animation using {@link #addImage}</li>
 * <li>Once images have been added, {@link #getAnimation} is called
 *   to write the resulting Gif file to disk, or to return its contents
 *   as an array of bytes</li>
 * </ol>
 * @author sylvain
 *
 */
public class GifAnimator
{
  protected Vector<MagickImage> m_images;
  
  public GifAnimator()
  {
    super();
    m_images = new Vector<MagickImage>();
  }
  
  /**
   * Adds an image to the animation
   * @param ba The image's contents
   * @return false if image could not be added
   */
  public boolean addImage(byte[] ba)
  {
    try
    {
      ImageInfo ii = new ImageInfo();
      MagickImage mi = new MagickImage(ii, ba);
      return addImage(mi);
    }
    catch (MagickException me)
    {
      return false;
    }
  }
  
  /**
   * Adds an image to the animation
   * @param mi The image's contents
   * @return Always true
   */
  public boolean addImage(MagickImage mi)
  {
    return m_images.add(mi);
  }
  
  /**
   * Produces an animated Gif file from a set of images
   * given beforehand.
   * @param frame_delay The delay (in 1/100ths of a second) between
   *   each of the frames of the resulting animation
   * @param filename The filename to save the image to
   */
  public void getAnimation(int frame_delay, String filename)
  {
    try
    {
      MagickImage out = getAnimationMagick(frame_delay);
      out.setFileName(filename);
      ImageInfo to_image = new ImageInfo();
      out.writeImage(to_image);
    }
    catch (MagickException e)
    {
      e.printStackTrace();
    }
  }
  
  /**
   * Produces an animated Gif file from a set of images
   * given beforehand.
   * <p><strong>Note:</strong> this method does not
   * work properly, as JMagick's <tt>imageToBlob</tt> method only
   * outputs the contents of the <em>first</em> frame in the animation.
   * A workaround was provided in later versions of JMagick, but
   * Ubuntu ships an old version of the library that does not have it
   * (alas). At the moment, the only solution under Ubuntu is to
   * write the image directly to a file (which works correctly),
   * using {@link #getAnimation(int, String)}.
   * @param frame_delay The delay (in 1/100ths of a second) between
   *   each of the frames of the resulting animation
   * @return An array of bytes containing the resulting Gif file
   */
  public byte[] getAnimation(int frame_delay)
  {
    byte[] out_array = null;
    try
    {
      MagickImage out = getAnimationMagick(frame_delay);
      ImageInfo to_image = new ImageInfo();
      out_array = out.imageToBlob(to_image);
    }
    catch (MagickException e)
    {
      e.printStackTrace();
    }  
    return out_array;
  }
  
  /**
   * Produces an animated Gif file from a set of images
   * given beforehand.
   * @param frame_delay The delay (in 1/100ths of a second) between
   *   each of the frames of the resulting animation
   * @return The MagickImage object for the resulting Gif file
   */
  protected MagickImage getAnimationMagick(int frame_delay) throws MagickException
  {
    MagickImage out = new MagickImage(getImageArray(frame_delay));
    out.setImageAttribute("Dispose", "1");
    out.setImageAttribute("Delay", new Integer(frame_delay).toString());
    out.setMagick("GIF");
    return out;
  }
  
  /**
   * Creates an array of magick image objects out of a list of images
   * given beforehand. This method is only used internally by the class.
   * @param frame_delay The delay (in 1/100ths of a second) between
   *   each of the frames of the resulting animation
   * @return The array of images
   */
  protected MagickImage[] getImageArray(int frame_delay)
  {
    MagickImage[] images = new MagickImage[m_images.size()];
    int i = 0;
    for (MagickImage ii : m_images)
    {
      try
      {
        ii.setImageAttribute("Dispose", "1");
        ii.setImageAttribute("Delay", new Integer(frame_delay).toString());
      } catch (MagickException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      images[i] = ii;
      i++;
    }
    return images;
  }
}
