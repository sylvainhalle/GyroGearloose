package ca.uqac.lif.qr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.protocol.Sender;
import ca.uqac.lif.media.GifAnimator;
import ca.uqac.lif.util.FileReadWrite;
import ca.uqac.lif.util.StoppableRunnable.LoopStatus;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class AnimateWorkflow extends FrontEnd
{
  protected static final Options s_options = getOptions();
  
  /**
   * Reads command line arguments and performs actions for a "write"
   * operation (i.e. producing frames from a source). 
   * @param args The command line arguments, as obtained from 
   * {@link #main(String[])}.
   */
  static int mainLoop(String[] args)
  {
    // Default values for parameters
    int frame_rate = 10;
    String output_filename = "", input_filename = "";

    // Instantiate animator, reader, etc.
    ZXingWriter reader_writer = new ZXingWriter();
    Sender sender = new Sender();
    FrameEncoder encoder = null;

    CommandLineParser parser = new PosixParser();
    CommandLine c_line = null;
    try
    {
      // parse the command line arguments
      c_line = parser.parse(s_options, args);
    }
    catch (ParseException exp)
    {
      // oops, something went wrong
      System.err.println("ERROR: " + exp.getMessage() + "\n");
      showUsage();
      return FrontEnd.ERR_ARGUMENTS;
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
          return FrontEnd.ERR_IO;
        }
        catch (ReadException e)
        {
          System.err.println("Error parsing schema file " + schema_filename);
          return FrontEnd.ERR_PARSE;
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
        return FrontEnd.ERR_IO;
      }
    }

    // Setup frame encoder
    encoder.setSender(sender);
    encoder.setInputStream(in);
    encoder.setFramerate(frame_rate);

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
    return FrontEnd.ERR_OK;
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
            .create("z");
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
    return options;
  }
  
  public static void showUsage()
  {
    HelpFormatter hf = new HelpFormatter();
    hf.printHelp("java -jar GyroGearloose.jar animate [options] [file [schema1 [schema2 ...]]]", s_options);
  }
}
