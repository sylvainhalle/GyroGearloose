package ca.uqac.lif.qr;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.util.StoppableRunnable;



public class WebcamTest
{
  public static void main(String[] args)
  {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    int fps = 30;
    Receiver m_btReader = new Receiver();
    FrameDecoder fd = new FrameDecoder();
    fd.setReceiver(m_btReader);
    
    ZXingReadWrite reader = new ZXingReadWrite();
    VideoCapture camera = new VideoCapture(0);
    camera.open(0); //Useless
    //camera.set(3,800);
    int width = 640;
    int height = 480;
    camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, width);
    camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, height);
    if(!camera.isOpened())
    {
      System.out.println("Camera Error");
    }
    //System.out.println(camera.get(Highgui.CV_CAP_PROP_FRAME_WIDTH));
    //System.out.println(camera.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT));
    //camera.set(Highgui.CV_CAP_PROP_FOCUS, 2);
    CameraWindowUpdater wu = new CameraWindowUpdater(camera, 1000/fps);
    CameraDisplayFrame window = new CameraDisplayFrame(wu, width, height);
    wu.setWindow(window);
    wu.setStartState(StoppableRunnable.LoopStatus.ACTIVE);
    window.setReader(reader);
    window.setDecoder(fd);
    window.setVisible(true);
    Thread th = new Thread(wu);
    th.start();
  }

}
