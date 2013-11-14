package ca.uqac.lif.qr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class CameraWindowUpdater extends WindowUpdater
{
  protected VideoCapture m_camera;
  
  protected CameraDisplayFrame m_window;
  
  protected ZXingReader m_reader;
  
  protected FrameDecoder m_decoder;
  
  public CameraWindowUpdater(CameraDisplayFrame window, ZXingReader reader, FrameDecoder decoder, int interval)
  {
    super(interval);
    m_window = window;
    m_reader = reader;
    m_decoder = decoder;
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    m_camera = new VideoCapture(0);
    m_camera.open(0);
    if(!m_camera.isOpened())
    {
      System.err.println("Camera Error");
      System.exit(FrontEnd.ERR_IO);
    }
  }
  
  public void setProcessEvents(boolean b)
  {
    m_decoder.setProcessEvents(b);
  }
  
  /**
   * Attempt to read a frame from the camera
   * @return
   */
  protected BufferedImage getCameraFrame()
  {
    Mat frame = new Mat();
    m_camera.read(frame);
    MatOfByte buf = new MatOfByte();
    Highgui.imencode(".bmp", frame, buf);
    byte[] bytes = buf.toArray();
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    BufferedImage img = null;
    try
    {
      img = ImageIO.read(in);
    }
    catch (IOException e1)
    {
      // Could not read image
    }
    return img;
  }

  @Override
  public LoopStatus actionLoop()
  {
    long time_beg = System.nanoTime();
    // Poll sender for a new image
    BufferedImage img = getCameraFrame();
    if (img != null)
    {
      // A new image was sent: update the window
      m_window.setImage(img);
      String contents = m_reader.readCode(img);
      m_window.setFrameContents(contents);
      m_window.repaint();
      m_decoder.setNewFrame(contents); 
    }
    // Sleep a little while
    long time_now = System.nanoTime();
    int actual_refresh = Math.max(0, m_refreshInterval - ((int) ((time_now - time_beg) / 1000000f)));
    safeSleep(actual_refresh);
    // We want to be called again
    return LoopStatus.ACTIVE;
  }
  
  protected static void safeSleep(int duration)
  {
    try
    {
      Thread.sleep(duration);
    }
    catch (InterruptedException e)
    {
      // Do nothing
      e.printStackTrace();
    }
  }

}
