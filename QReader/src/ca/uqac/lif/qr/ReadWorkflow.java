package ca.uqac.lif.qr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.lif.media.FilenameListIterator;
import ca.uqac.lif.media.VideoFrameIterator;
import ca.uqac.lif.media.VideoFrameReader;
import ca.uqac.lif.util.StoppableRunnable;

import com.google.zxing.BarcodeFormat;

public class ReadWorkflow extends FrontEnd
{
  protected static final Options s_options = getOptions();
  
  /**
   * Reads command line arguments and performs actions for a "read"
   * operation (i.e. decoding frames from a source). 
   * @param args The command line arguments, as obtained from 
   * {@link #main(String[])}.
   */
  static int mainLoop(String[] args)
  {
    // Setup default values
    int fps = 30;
    boolean in_binary = false;
    // Setup and parse command line options
    Options options = getOptions();

    // Setup receiver, decoder, etc.
    ZXingReader reader = new ZXingReader();
    Receiver recv = new Receiver();
    recv.setConsole(System.err);
    FrameDecoder fd = new FrameDecoder();
    fd.setReceiver(recv);

    CommandLineParser parser = new PosixParser();
    CommandLine c_line = null;
    try
    {
      // parse the command line arguments
      c_line = parser.parse(options, args);
    }
    catch (ParseException exp)
    {
      // oops, something went wrong
      System.err.println("ERROR: " + exp.getMessage() + "\n");
      showUsage();
      return ERR_ARGUMENTS;
    }
    if (c_line.hasOption("h"))
    {
      showUsage();
      return ERR_OK;
    }
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();
    if (c_line.hasOption("verbosity"))
    {
      int verbosity = Integer.parseInt(c_line.getOptionValue("verbosity"));
      recv.setVerbosity(verbosity);
    }
    if (c_line.hasOption("binary"))
    {
      in_binary = true;
    }
    if (c_line.hasOption("threshold"))
    {
      String th = c_line.getOptionValue("threshold");
      if (th.compareToIgnoreCase("guess") == 0)
      {
        reader.useThresholdBinarizer(true);
        reader.setGuessThreshold(true);
      }
      else if (th.compareToIgnoreCase("histogram") == 0)
      {
        reader.setGuessThreshold(false);
      }
      else
      {
        int binarization_threshold = Integer.parseInt(c_line.getOptionValue("threshold"));
        if (binarization_threshold < 0 || binarization_threshold > 255)
        {
          System.err.println("ERROR: binarization threshold must be in the range 0-255.");
          return ERR_ARGUMENTS;
        }
        reader.useThresholdBinarizer(true);
        reader.setGuessThreshold(false);
        reader.setBinarizationThreshold(binarization_threshold);
      }
    }
    if (c_line.hasOption("format"))
    {
      String bc_format = c_line.getOptionValue("format");
      if (bc_format.compareToIgnoreCase("aztec") == 0)
      {
        reader.setBarcodeFormat(BarcodeFormat.AZTEC);
      }
      else if (bc_format.compareToIgnoreCase("datamatrix") == 0)
      {
        reader.setBarcodeFormat(BarcodeFormat.DATA_MATRIX);
      }
      else if (bc_format.compareToIgnoreCase("qr") == 0)
      {
        reader.setBarcodeFormat(BarcodeFormat.QR_CODE);
      }
      else
      {
        System.err.println("Invalid barcode format");
      }
    }
    if (c_line.hasOption("purecode"))
    {
      reader.setPureCode(true);
    }
    //fd.setProcessEvents(!dont_process);

    Iterator<BufferedImage> image_source = null;
    // Check extension of first filename
    LinkedList<String> filenames = new LinkedList<String>();
    filenames.addAll(remaining_args);
    filenames.removeFirst(); // Since arg 0 is "read"
    String first_filename = filenames.peek(); 
    if (first_filename != null)
    {
      // We read from some file
      if (isVideoFile(first_filename))
      {
        // File is a video: iterate over its frames
        VideoFrameReader vfr = new VideoFrameReader(first_filename);
        image_source = new VideoFrameIterator(vfr);
        //num_files = vfr.getNumFrames(fps);
      }
      else
      {
        // File is an image: iterate over each filename passed as argument
        image_source = new FilenameListIterator(filenames);
        //num_files = filenames.size();
      }
      while (image_source.hasNext())
      {
        BufferedImage img = image_source.next();
        if (img == null)
        {
          // Another way of checking if frames remain
          break;
        }
        fd.printReadStatistics(true);
        String data = reader.readCode(img);
        fd.setNewFrame(data);
        if (in_binary)
        {
          // Poll receiver's binary buffer and write whatever bytes that
          // can be written
          BitSequence recv_bs = fd.pollBinaryBuffer(-1);
          byte[] bytes = recv_bs.toByteArray();
          try
          {
            System.out.write(bytes);
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
        }
        else
        {
          // Poll receiver's message buffer and write whatever messages
          // that can be written
          SchemaElement se = null;
          do
          {
            se = fd.pollMessage();
            System.out.println(se.toString());
          } while (se != null);
        }
      }
    }
    else
    {
      // Read from camera
      CameraDisplayFrame window = new CameraDisplayFrame();
      CameraWindowUpdater wu = new CameraWindowUpdater(window, reader, fd, 1000/fps);
      wu.setStartState(StoppableRunnable.LoopStatus.ACTIVE);
      window.setVisible(true);
      Thread th = new Thread(wu);
      th.start();
      while (th.isAlive())
      {
        CameraWindowUpdater.safeSleep(1000);
      }
    }
    // Done!
    return ERR_OK;
  }
  
  @SuppressWarnings("static-access")
  static Options getOptions()
  {
    Options options = new Options();
    Option opt;
    opt = OptionBuilder
        .withLongOpt("help")
        .withDescription(
            "Display command line usage")
            .create("h");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("mute")
        .withDescription(
            "Don't output decoded contents to stdout, just print stats")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("purecode")
        .withDescription(
            "Tells reader that input is a pure binary image of a code")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("threshold")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set binarization threshold to x ('guess', or between 0 and 255, default 128)")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("framerate")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Process images at x fps (default: 8)")
            .create("r");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("binary")
        .withDescription(
            "Frames encode blob segments")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("verbosity")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Verbose messages with level x")
            .create();
    options.addOption(opt);
    return options;
  }
  
  protected static String getFileExtension(String filename)
  {
    if (filename == null) {
      return null;
    }
    int lastUnixPos = filename.lastIndexOf('/');
    int lastWindowsPos = filename.lastIndexOf('\\');
    int indexOfLastSeparator = Math.max(lastUnixPos, lastWindowsPos);
    int extensionPos = filename.lastIndexOf('.');
    int lastSeparator = indexOfLastSeparator;
    int indexOfExtension = lastSeparator > extensionPos ? -1 : extensionPos;
    int index = indexOfExtension;
    if (index == -1) {
      return "";
    } else {
      return filename.substring(index + 1);
    }
  }

  /**
   * Checks if a given file is a video, by looking at its extension
   * @param filename The filename
   * @return true if file is deemed to be a video
   */
  protected static boolean isVideoFile(String filename)
  {
    if (filename == null)
    {
      return false;
    }
    String extension = getFileExtension(filename);
    return extension.compareToIgnoreCase("mp4") == 0 || extension.compareToIgnoreCase("avi") == 0 || extension.compareToIgnoreCase("mkv") == 0;
  }
  
  public static void showUsage()
  {
    HelpFormatter hf = new HelpFormatter();
    hf.printHelp("java -jar GyroGearloose.jar read [options]", s_options);
  }
}
