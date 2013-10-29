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

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.IError;

/**
 * The instructions for reading video frames have been adapted from
 * a piece of code found on <a href="http://stackoverflow.com/questions/15735716/how-can-i-get-a-frame-sample-jpeg-from-a-video-mov">StackOverflow</a>.
 * @author aclarke
 * @author trebor
 * @author sylvain
 *
 */
public class VideoFrameReader extends MediaListenerAdapter
{
  /**
   * The video stream index currently being read by the reader
   */
  protected int m_videoStreamIndex = -1;
  
  /**
   * Boolean that is set to true by the {@link #onVideoPicture} callback
   * to indicate that a new frame was decoded
   */
  protected boolean m_frameRead = false;
  
  /**
   * Where the next decoded frame is copied by the
   * {@link #onVideoPicture} callback
   */
  protected BufferedImage m_frame = null;
  
  /**
   * The media reader that will decode the frames
   */
  protected IMediaReader m_reader = null;
  
  public VideoFrameReader(String videoFile)
  {
    this(videoFile, BufferedImage.TYPE_3BYTE_BGR);
  }
  
  public VideoFrameReader(String videoFile, int image_type)
  {
    super();
    // Create a media reader for processing video
    m_reader = ToolFactory.makeReader(videoFile);

    // Stipulate that we want BufferedImages created in BGR 24bit color space
    m_reader.setBufferedImageTypeToGenerate(image_type);

    // note that DecodeAndCaptureFrames is derived from
    // MediaReader.ListenerAdapter and thus may be added as a listener
    // to the MediaReader. DecodeAndCaptureFrames implements
    // onVideoPicture().
    m_reader.addListener(this);
  }

  /**
   * Reads the next frame of the video.
   * @return The next decoded frame in the video, or null if decoding
   *   failed (or file is at the end)
   * @throws Exception
   */
  public BufferedImage nextFrame() throws Exception
  {
    // read out the contents of the media file, note that nothing else
    // happens here.  action happens in the onVideoPicture() method
    // which is called when complete video pictures are extracted from
    // the media source
    IError ie = null;
    do
    {
      ie = m_reader.readPacket();
      // Read packets until m_frameRead switches to true, indicating
      // that a full frame has been decoded
    } while (ie == null && !m_frameRead);
    if (ie != null && ie.getType() != IError.Type.ERROR_EOF)
    {
      // We are done reading the file; however if the error type
      // is not the indication of EOF, then an actual error occured
      throw new Exception();
    }
    if (m_frameRead)
    {
      // A frame was read; it is placed in member field m_frame
      BufferedImage bi = m_frame;
      m_frame = null;
      m_frameRead = false;
      return bi;
    }
    // No frame was read: return null
    return null;
  }

  /** 
   * Called after a video frame has been decoded from a media stream.
   * Optionally a BufferedImage version of the frame may be passed
   * if the calling {@link IMediaReader} instance was configured to
   * create BufferedImages.
   * 
   * This method blocks, so return quickly.
   */
  @Override
  public void onVideoPicture(IVideoPictureEvent event)
  {
    try
    {
      // if the stream index does not match the selected stream index,
      // then have a closer look
      if (event.getStreamIndex() != m_videoStreamIndex)
      {
        // if the selected video stream id is not yet set, go ahead an
        // select this lucky video stream
        if (-1 == m_videoStreamIndex)
          m_videoStreamIndex = event.getStreamIndex();

        // otherwise return, no need to show frames from this video stream
        else
        {
          return;
        }
      }
      // Tell the main loop that we have read a frame
      m_frame = event.getImage();
      m_frameRead = true;
      //ImageIO.write(event.getImage(), "jpg", new File(saveFile));
    }
    catch (Exception e)
    {
      // Do nothing
    }
  }
}
