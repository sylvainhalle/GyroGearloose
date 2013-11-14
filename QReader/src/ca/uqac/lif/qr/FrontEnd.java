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
import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import ca.uqac.info.buffertannen.message.*;
import ca.uqac.info.buffertannen.protocol.*;
import ca.uqac.info.util.FileReadWrite;
import ca.uqac.info.util.StoppableRunnable;
import ca.uqac.info.util.StoppableRunnable.LoopStatus;
import ca.uqac.lif.media.FilenameListIterator;
import ca.uqac.lif.media.GifAnimator;
import ca.uqac.lif.media.VideoFrameIterator;
import ca.uqac.lif.media.VideoFrameReader;

public class FrontEnd
{
  /**
   * Return codes
   */
  public static final int ERR_OK = 0;
  public static final int ERR_FILE_NOT_FOUND = 0;
  public static final int ERR_PARSE = 2;
  public static final int ERR_IO = 3;
  public static final int ERR_ARGUMENTS = 4;
  public static final int ERR_RUNTIME = 6;
  public static final int ERR_WRITER = 7;
  public static final int ERR_CANNOT_DECODE = 8;
  
  /**
   * Main entrance point for the decoder.
   * @param args Command line arguments
   */
  public static void main(String[] args)
  {
    if (args.length < 1)
    {
      System.err.println("Missing action. Valid values are `animate', `read'.");
      System.exit(ERR_ARGUMENTS);
    }
    String action = args[0];
    if (action.compareToIgnoreCase("animate") == 0)
    {
      setupAnimation(args);
    }
    else if (action.compareToIgnoreCase("read") == 0)
    {
      setupRead(args);
    }
    else
    {
      System.err.println("Invalid action. Valid values are `animate', `read'.");
      System.exit(ERR_ARGUMENTS);      
    }
    System.exit(ERR_OK);
  }
  
  /**
   * Reads command line arguments and performs actions for a "read"
   * operation (i.e. decoding frames from a source). 
   * @param args The command line arguments, as obtained from 
   * {@link #main(String[])}.
   */
  @SuppressWarnings("static-access")
  protected static void setupRead(String[] args)
  {
    // Setup default values
    int fps = 30;
    boolean in_binary = false;
    // Setup and parse command line options
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
      HelpFormatter hf = new HelpFormatter();
      hf.printHelp("java -jar GyroGearloose.jar read [options]", options);
      System.exit(ERR_ARGUMENTS);
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
          System.exit(ERR_ARGUMENTS);
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
  }
   
  /**
   * Reads command line arguments and performs actions for a "write"
   * operation (i.e. producing frames from a source). 
   * @param args The command line arguments, as obtained from 
   * {@link #main(String[])}.
   */
  @SuppressWarnings("static-access")
  protected static void setupAnimation(String[] args)
  {
    // Default values for parameters
    int frame_rate = 10;
    String output_filename = "", input_filename = "";

    // Setup and parse command line options
    Options options = new Options();
    Option opt;
    opt = OptionBuilder
        .withLongOpt("help")
        .withDescription(
            "Display command line usage")
            .create("h");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("size")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set output image size to x (default: 300)")
            .create("s");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("framerate")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set animation speed to x fps (default: 8)")
            .create("r");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("framesize")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set maximum frame size to x bits (default: 2000)")
            .create("r");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("noloop")
        .withDescription(
            "Don't loop through frames when sending in lake mode")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("level")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set error correction level to x (either L (7%), M (15%), Q (25%), or H (30%), default L)")
            .create("l");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("pipe")
        .withDescription(
            "Input file is a pipe (not a regular file)")
            .create("p");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("output")
        .withArgName("file")
        .hasArg()
        .withDescription(
            "Output GIF animation to file")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("format")
        .withArgName("f")
        .hasArg()
        .withDescription(
            "Write codes using format f (qr, aztec, datamatrix)")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("stdin")
        .withDescription(
            "Read trace from stdin")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("binary")
        .withDescription(
            "Encode input file as blob segments")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("lake")
        .withDescription(
            "Send in lake mode")
            .create();
    options.addOption(opt);
    
    // Instantiate animator, reader, etc.
    ZXingWriter reader_writer = new ZXingWriter();
    Sender sender = new Sender();
    FrameEncoder encoder = null;
    
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
      HelpFormatter hf = new HelpFormatter();
      hf.printHelp("java -jar GyroGearloose.jar animate [options] [file [schema1 [schema2 ...]]]", options);
      System.exit(ERR_ARGUMENTS);
    }
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();

    if (c_line.hasOption("size"))
    {
      int image_size = Integer.parseInt(c_line.getOptionValue("size"));
      reader_writer.setCodeSize(image_size);
    }
    if (c_line.hasOption("output"))
    {
      output_filename = c_line.getOptionValue("output");
    }
    if (c_line.hasOption("framesize"))
    {
      int frame_size = Integer.parseInt(c_line.getOptionValue("framesize"));
      sender.setFrameMaxLength(frame_size);
    }
    if (c_line.hasOption("binary"))
    {
      encoder = new FrameEncoderBinary();
    }
    else
    {
      encoder = new FrameEncoderMessage();
    }
    if (c_line.hasOption("pipe"))
    {
      sender.setEmptyBufferIsEof(false);
    }
    if (c_line.hasOption("lake"))
    {
      // Beware: lake mode overrides settings for pipe
      Sender.SendingMode sending_mode = Sender.SendingMode.LAKE;
      sender.setSendingMode(sending_mode);
      sender.setEmptyBufferIsEof(true);
    }
    if (c_line.hasOption("noloop"))
    {
      sender.setLakeLoop(false);
    }
    if (c_line.hasOption("framerate"))
    {
      frame_rate = Integer.parseInt(c_line.getOptionValue("framerate"));
      encoder.setFramerate(frame_rate);
    }
    if (c_line.hasOption("resourceid"))
    {
      String resource_identifier = c_line.getOptionValue("resourceid");
      sender.setResourceIdentifier(resource_identifier);
    }
    if (c_line.hasOption("streamindex"))
    {
      int data_stream_index = Integer.parseInt(c_line.getOptionValue("streamindex"));
      sender.setDataStreamIndex(data_stream_index);
    }
    if (c_line.hasOption("level"))
    {
      String ec_level = c_line.getOptionValue("level");
      if (ec_level.compareToIgnoreCase("L") == 0)
      {
        // 7% correction
        reader_writer.setErrorCorrectionLevel(ErrorCorrectionLevel.L);
      }
      else if (ec_level.compareToIgnoreCase("M") == 0)
      {
        // 15% correction
        reader_writer.setErrorCorrectionLevel(ErrorCorrectionLevel.M);
      }
      else if (ec_level.compareToIgnoreCase("Q") == 0)
      {
        // 25% correction
        reader_writer.setErrorCorrectionLevel(ErrorCorrectionLevel.Q);
      }
      else if (ec_level.compareToIgnoreCase("H") == 0)
      {
        // 30% correction
        reader_writer.setErrorCorrectionLevel(ErrorCorrectionLevel.H);
      }
    }
    if (c_line.hasOption("format"))
    {
      String bc_format = c_line.getOptionValue("format");
      if (bc_format.compareToIgnoreCase("aztec") == 0)
      {
        reader_writer.setBarcodeFormat(BarcodeFormat.AZTEC);
      }
      else if (bc_format.compareToIgnoreCase("datamatrix") == 0)
      {
        reader_writer.setBarcodeFormat(BarcodeFormat.DATA_MATRIX);
      }
      else if (bc_format.compareToIgnoreCase("qr") == 0)
      {
        reader_writer.setBarcodeFormat(BarcodeFormat.QR_CODE);
      }
      else
      {
        System.err.println("Invalid barcode format");
      }
    }
    if (remaining_args.size() >= 2)
    {
      // We read from a file
      input_filename = remaining_args.get(1);
      int schema_nb = 0;
      for (int i = 2; i < remaining_args.size(); i++)
      {
        String schema_filename = remaining_args.get(i);
        String schema_contents;
        try
        {
          schema_contents = FileReadWrite.readFile(schema_filename);
          sender.setSchema(schema_nb++, schema_contents);
        }
        catch (IOException e)
        {
          System.err.println("Error reading schema file " + schema_filename);
          System.exit(ERR_IO);
        }
        catch (ReadException e)
        {
          System.err.println("Error parsing schema file " + schema_filename);
          System.exit(ERR_PARSE);
        }
      }
    }
    
    // Setup input stream
    InputStream in = null;
    if (input_filename.isEmpty())
    {
      in = System.in;
    }
    else
    {
      try
      {
        in = new FileInputStream(new File(input_filename));
      }
      catch (FileNotFoundException e)
      {
        System.err.println("File not found: " + input_filename);
        System.exit(ERR_IO);
      }
    }
    
    // Setup frame encoder
    encoder.setSender(sender);
    encoder.setInputStream(in);
    
    if (output_filename.isEmpty())
    {
      // We animate the codes live in a window
      CodeWindowUpdater wu = new CodeWindowUpdater(encoder, reader_writer, 1000/frame_rate);
      CodeDisplayFrame window = new CodeDisplayFrame(wu);
      wu.setWindow(window);
      wu.setStartState(LoopStatus.SUSPENDED);
      window.setVisible(true);
      Thread th = new Thread(wu);
      th.start();
      while (th.isAlive())
      {
        CameraWindowUpdater.safeSleep(1000);
      }
    }
    else
    {
      // We output the codes into a GIF file
      GifAnimator animator = new GifAnimator();
      BitSequence bs = null;
      do
      {
        bs = encoder.pollNextFrame();
        BufferedImage img = reader_writer.getCode(bs.toBase64());
        animator.addImage(img);
        encoder.printStatsInterval();
      } while (bs != null);
      animator.getAnimation(100 / frame_rate, output_filename);
    }
    // Done!
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

}
