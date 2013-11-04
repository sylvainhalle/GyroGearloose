package ca.uqac.lif.qr;
import org.opencv.core.Core;
import org.opencv.highgui.VideoCapture;

import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.util.StoppableRunnable;



public class WebcamTest
{
  public static void main(String[] args)
  {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    int fps = 12;
    Receiver m_btReader = new Receiver();
    m_btReader.setFrameMaxLength(2500);
    ZXingReadWrite reader = new ZXingReadWrite();
    VideoCapture camera = new VideoCapture(0);
    camera.open(0); //Useless
    if(!camera.isOpened())
    {
      System.out.println("Camera Error");
    }
    CameraWindowUpdater wu = new CameraWindowUpdater(camera, 1000/fps);
    CameraDisplayFrame window = new CameraDisplayFrame(wu);
    wu.setWindow(window);
    wu.setStartState(StoppableRunnable.LoopStatus.ACTIVE);
    window.setReader(reader);
    window.setReceiver(m_btReader);
    window.setVisible(true);
    Thread th = new Thread(wu);
    th.start();
    while (th.isAlive())
    {
      try
      {
        Thread.sleep(1000);
      } catch (InterruptedException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

}
