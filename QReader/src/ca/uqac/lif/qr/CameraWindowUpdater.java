package ca.uqac.lif.qr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class CameraWindowUpdater extends WindowUpdater
{
  protected VideoCapture m_camera;
  
  protected CameraDisplayFrame m_window;
  
  public CameraWindowUpdater(VideoCapture camera, int interval)
  {
    super(interval);
    m_camera = camera;
  }
  
  public void setWindow(CameraDisplayFrame w)
  {
    m_window = w;
  }

  @Override
  public LoopStatus actionLoop()
  {
    long time_beg = System.nanoTime();
    // Poll sender for a new image
    Mat frame = new Mat();
    m_camera.read(frame);
    MatOfByte buf = new MatOfByte();
    //Highgui.imencode(".png", frame, buf);
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
    if (img != null)
    {
      // A new image was sent: update the window
      m_window.setImage(img);
      m_window.repaint();
    }
    // Sleep a little while
    long time_now = System.nanoTime();
    int actual_refresh = Math.max(0, m_refreshInterval - ((int) ((time_now - time_beg) / 1000000f)));
    try
    {
      Thread.sleep(actual_refresh);
    }
    catch (InterruptedException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // We want to be called again
    return LoopStatus.ACTIVE;
  }

}
