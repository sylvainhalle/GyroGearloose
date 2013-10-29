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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.*;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.message.SchemaElement;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.lif.media.FilenameListIterator;
import ca.uqac.lif.media.VideoFrameIterator;
import ca.uqac.lif.media.VideoFrameReader;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ReaderException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class BtQrReader
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

  public static void main(String[] args)
  {
    // Parse command line arguments
    Options options = setupOptions();
    CommandLine c_line = setupCommandLine(args, options);
    assert c_line != null;
    int verbosity = 0;

    if (verbosity > 0)
    {
      showHeader();
    }
    if (c_line.hasOption("version"))
    {
      System.err.println("(C) 2013 Sylvain Hallé et al., Université du Québec à Chicoutimi");
      System.err.println("This program comes with ABSOLUTELY NO WARRANTY.");
      System.err.println("This is a free software, and you are welcome to redistribute it");
      System.err.println("under certain conditions. See the file COPYING for details.\n");
      System.exit(ERR_OK);
    }

    // Get action
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();
    if (remaining_args.isEmpty())
    {
      System.err.println("ERROR: missing action");
      showUsage(options);
      System.exit(ERR_ARGUMENTS);
    }
    String action = remaining_args.get(0);
    if (action.compareToIgnoreCase("read") == 0)
    {
      read(c_line);
    }
    else if (action.compareToIgnoreCase("animate") == 0)
    {
      animate(c_line);
    }
    else if (action.compareToIgnoreCase("decode") == 0)
    {
      decode(c_line);
    }
    else
    {
      System.err.println("ERROR: invalid arguments");
      showUsage(options);
      System.exit(ERR_ARGUMENTS);
    }
  }

  /**
   * Sets up the options for the command line parser
   * @return The options
   */
  @SuppressWarnings("static-access")
  private static Options setupOptions()
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
            .create("r");
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("version")
        .withDescription(
            "Show version")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("stats")
        .withDescription(
            "Generate stats to stdout")
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
        .withLongOpt("threshold")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Set binarization threshold to x ('guess', or between 0 and 255, default 128)")
            .create();
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
        .withLongOpt("verbosity")
        .withArgName("x")
        .hasArg()
        .withDescription(
            "Verbose messages with level x")
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
        .withLongOpt("mute")
        .withDescription(
            "Don't output decoded contents to stdout, just print stats")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("nodisplay")
        .withDescription(
            "Don't output encoding/decoding stats in realtime (only at end)")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("purecode")
        .withDescription(
            "Tells reader that input is a pure binary image of a code")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("window")
        .withDescription(
            "Display codes onscreen in a window")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("stdin")
        .withDescription(
            "Read trace from stdin")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("noprocess")
        .withDescription(
            "Only attempt to decode codes, don't process their contents")
            .create();
    options.addOption(opt);
    opt = OptionBuilder
        .withLongOpt("pipe")
        .withArgName("file")
        .hasArg()
        .withDescription(
            "Read trace from named pipe file")
            .create("p");
    options.addOption(opt);
    return options;
  }

  /**
   * Show the benchmark's usage
   * @param options The options created for the command line parser
   */
  private static void showUsage(Options options)
  {
    HelpFormatter hf = new HelpFormatter();
    hf.printHelp("java -jar QRTranslator.jar [read|animate|decode] [options]", options);
  }

  /**
   * Sets up the command line parser
   * @param args The command line arguments passed to the class' {@link #main}
   * method
   * @param options The command line options to be used by the parser
   * @return The object that parsed the command line parameters
   */
  private static CommandLine setupCommandLine(String[] args, Options options)
  {
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
      //HelpFormatter hf = new HelpFormatter();
      //hf.printHelp(t_gen.getAppName() + " [options]", options);
      System.exit(ERR_ARGUMENTS);
    }
    return c_line;
  }

  protected static void read(CommandLine c_line)
  {
    int lost_frames = 0, total_frames = 0;
    int lost_segments = 0;
    int total_messages = 0, total_size = 0;
    int frame_size = 0;
    int binarization_threshold = 128;
    int verbosity = 0;
    int fps = 8;
    boolean first_file = true, dont_process = false;
    boolean guess_threshold = false, mute = false;
    boolean realtime_display = true;
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();

    if (c_line.hasOption("verbosity"))
    {
      verbosity = Integer.parseInt(c_line.getOptionValue("verbosity"));
    }
    if (c_line.hasOption("framesize"))
    {
      frame_size = Integer.parseInt(c_line.getOptionValue("framesize"));
    }
    else
    {
      System.err.println("ERROR: a maximum frame size must be specified in order to read");
      System.exit(ERR_ARGUMENTS);
    }
    if (c_line.hasOption("noprocess"))
    {
      dont_process = true;
    }
    if (c_line.hasOption("framerate"))
    {
      fps = Integer.parseInt(c_line.getOptionValue("framerate"));
    }
    if (c_line.hasOption("threshold"))
    {
      String th = c_line.getOptionValue("threshold");
      if (th.compareToIgnoreCase("guess") == 0)
      {
        guess_threshold = true;
      }
      else if (th.compareToIgnoreCase("histogram") == 0)
      {
        binarization_threshold = -1;
      }
      else
      {
        binarization_threshold = Integer.parseInt(c_line.getOptionValue("threshold"));
        if (binarization_threshold < 0 || binarization_threshold > 255)
        {
          System.err.println("ERROR: binarization threshold must be in the range 0-255.");
          System.exit(ERR_ARGUMENTS);
        }
      }
    }
    if (c_line.hasOption("mute"))
    {
      mute = true;
    }
    if (c_line.hasOption("nodisplay"))
    {
      realtime_display = false;
    }

    // Instantiate BufferTannen receiver
    Receiver recv = new Receiver();
    recv.setFrameMaxLength(frame_size);
    recv.setVerbosity(verbosity);
    recv.setConsole(System.err);

    long last_refresh = -1;
    int num_files = 1;
    int last_good_threshold = binarization_threshold;
    BarcodeFormat format = BarcodeFormat.QR_CODE;
    ZXingReadWrite reader_writer = new ZXingReadWrite();
    if (c_line.hasOption("format"))
    {
      String bc_format = c_line.getOptionValue("format");
      if (bc_format.compareToIgnoreCase("aztec") == 0)
      {
        format = BarcodeFormat.AZTEC;
      }
      else if (bc_format.compareToIgnoreCase("datamatrix") == 0)
      {
        format = BarcodeFormat.DATA_MATRIX;
      }
      else if (bc_format.compareToIgnoreCase("qr") == 0)
      {
        format = BarcodeFormat.QR_CODE;
      }
      else
      {
        System.err.println("Invalid barcode format");
      }
    }
    reader_writer.setBarcodeFormat(format);
    if (c_line.hasOption("purecode"))
    {
      reader_writer.setPureCode(true);
    }
    Iterator<BufferedImage> image_source = null;
    // Check extension of first filename
    LinkedList<String> filenames = new LinkedList<String>();
    filenames.addAll(remaining_args);
    filenames.removeFirst(); // Since arg 0 is "read"
    String first_filename = filenames.peek(); 
    if (isVideoFile(first_filename))
    {
      // File is a video: iterate over its frames
      VideoFrameReader vfr = new VideoFrameReader(first_filename);
      image_source = new VideoFrameIterator(vfr);
      num_files = vfr.getNumFrames(fps);
    }
    else
    {
      // File is an image: iterate over each filename passed as argument
      image_source = new FilenameListIterator(filenames);
      num_files = filenames.size();
    }
    long start_time = System.nanoTime();
    while (image_source.hasNext())
    {
      BufferedImage img = image_source.next();
      if (img == null)
      {
        // Another way of checking if frames remain
        break;
      }
      total_frames++;
      long current_time = System.nanoTime();
      if (realtime_display && (last_refresh < 0 || current_time - last_refresh > 500000000)) // Refresh display every second or so
      {
        last_refresh = current_time;
        if (!first_file)
        {
          // Move cursor up 14 lines (this is the number of lines written by
          // {@link #printReadStatistics}
          if (dont_process)
            System.err.print("\u001B[5A\r");
          else
            System.err.print("\u001B[14A\r");
        }
        else
        {
          first_file = false;
        }
        printReadStatistics(System.err, dont_process, recv, total_frames, total_size, lost_frames, total_messages, start_time, fps, num_files);
      }
      String data = null;
      try
      {
        // First try to read code with last good threshold value
        data = reader_writer.readCode(img, last_good_threshold);
      }
      catch (ReaderException e)
      {
        // Cannot read code: re-estimate the best threshold value
        if (guess_threshold)
        {
          ThresholdGuesser guess = new ThresholdGuesser();
          guess.addImage(img);
          int suggested_threshold = guess.guessThreshold(60, 220, 10, last_good_threshold);
          if (suggested_threshold > 0)
          {
            last_good_threshold = suggested_threshold;
          }
          if (verbosity >= 2)
            System.err.println("Re-estimating threshold at " + last_good_threshold);
          try
          {
            data = reader_writer.readCode(img, binarization_threshold);
          }
          catch (ReaderException e1)
          {
            // Still cannot read code: too bad
          }
        }        
      }
      if (data == null)
      {
        if (verbosity >= 2)
          System.err.println("Cannot decode frame " + (total_frames - 1));
        lost_frames++;
        continue;
      }
      BitSequence bs = new BitSequence();
      try
      {
        System.out.println("RECV:" + data);
        bs.fromBase64(data);
      } catch (BitFormatException e)
      {
        if (verbosity >= 2)
          System.err.println("Cannot decode frame " + (total_frames - 1));
        lost_frames++;
        continue;
      }
      //System.err.printf("%4d/%4d (%2d%%)     \r", total_frames, num_files, (total_frames - lost_frames) * 100 / total_frames);
      if (!dont_process)
      {
        recv.putBitSequence(bs);
        SchemaElement se = recv.pollMessage();
        int lost_now = recv.getMessageLostCount();
        while (se != null)
        {
          if (verbosity >= 3)
            System.err.println("Lost : " + lost_now);
          total_messages++;
          BitSequence t_bs = null;
          try
          {
            t_bs = se.toBitSequence();
          }
          catch (BitFormatException e)
          {
            // Do nothing
          }
          total_size += t_bs.size();
          for (int i = 0; i < lost_now - lost_segments; i++)
          {
            if (!mute)
              System.out.println("This message was lost");
            if (verbosity >= 2)
              System.err.println("Lost message " + total_messages);
          }
          lost_segments = lost_now;
          if (!mute)
            System.out.println(se.toString());
          se = recv.pollMessage();
          lost_now = recv.getMessageLostCount();
        }        
      }
    }
    if (!first_file && realtime_display)
    {
      // Move cursor up
      if (dont_process)
        System.err.print("\u001B[5A\r");
      else
        System.err.print("\u001B[14A\r");
    }
    else
    {
      first_file = false;
    }
    printReadStatistics(System.err, dont_process, recv, total_frames, total_size, lost_frames, total_messages, start_time, fps, num_files);
    System.exit(ERR_OK);    
  }

  protected static void printReadStatistics(PrintStream out, boolean dont_process, Receiver recv, int total_frames, int total_size, int lost_frames, int total_messages, long start_time, int fps, int num_files)
  {
    long end_time = System.nanoTime();
    int raw_bits = recv.getNumberOfRawBits();
    long processing_time_ms = (end_time - start_time) / 1000000;
    //out.println("Processing results               ");
    out.println("----------------------------------------------------");
    out.printf(" Progress:           %04d/%04d (%02.1f sec. @%d fps)     \n", total_frames, num_files, (float) total_frames / (float) fps, fps);
    out.println(" Link quality:       " + (total_frames - lost_frames) + "/" + total_frames + " (" + ((total_frames - lost_frames) * 100 / Math.max(1, total_frames)) + "%)     ");
    if (!dont_process)
    {
      out.print(" Messages received:  " + total_messages + "/" + (total_messages + recv.getMessageLostCount()));
      if (total_messages + recv.getMessageLostCount() > 0)
        out.println(" (" + (total_messages * 100 / (total_messages + recv.getMessageLostCount())) + "%)     ");
      else
        out.println("     ");
      out.println("   Message segments: " + recv.getNumberOfMessageSegments() + " (" + recv.getNumberOfMessageSegmentsBits() + " bits)     ");
      out.println("   Delta segments:   " + recv.getNumberOfDeltaSegments() + " (" + recv.getNumberOfDeltaSegmentsBits() + " bits)     ");
      out.println("   Schema segments:  " + recv.getNumberOfSchemaSegments() + " (" + recv.getNumberOfSchemaSegmentsBits() + " bits)     ");
      out.print(" Processing rate:    " + processing_time_ms / Math.max(1, total_frames) + " ms/frame ");
      if (processing_time_ms > 0)
        out.println("(" + (total_frames * 1000 / processing_time_ms) + " fps)     ");
      else
        out.println("     ");
      out.println(" Bandwidth:");
      out.println("   Raw:              " + raw_bits + " bits (" + raw_bits * fps / Math.max(1, total_frames) + " bits/sec.)     ");
      out.println("   Actual:           " + recv.getNumberOfDistinctBits() + " bits (" + recv.getNumberOfDistinctBits() * fps / Math.max(1, total_frames) + " bits/sec.)     ");
      out.println("   Effective:        " + total_size + " bits (" + total_size * fps / Math.max(1, total_frames) + " bits/sec.)     ");
    }
    out.println("----------------------------------------------------\n");    
  }

  protected static void animate(CommandLine c_line)
  {
    int image_size = 300, frame_size = 2000, fps = 8;
    String output_filename = "", pipe_filename = "";
    BarcodeFormat format = BarcodeFormat.QR_CODE;
    ErrorCorrectionLevel level = ErrorCorrectionLevel.L;
    boolean animate_live = false, display_stats = true;
    boolean from_stdin = false;

    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();

    if (c_line.hasOption("size"))
    {
      image_size = Integer.parseInt(c_line.getOptionValue("size"));
    }
    if (c_line.hasOption("stdin"))
    {
      from_stdin = true;
    }
    if (c_line.hasOption("framesize"))
    {
      frame_size = Integer.parseInt(c_line.getOptionValue("framesize"));
    }
    if (c_line.hasOption("framerate"))
    {
      fps = Integer.parseInt(c_line.getOptionValue("framerate"));
    }
    if (c_line.hasOption("output"))
    {
      output_filename = c_line.getOptionValue("output");
    }
    if (c_line.hasOption("pipe"))
    {
      pipe_filename = c_line.getOptionValue("pipe");
    }
    if (c_line.hasOption("window"))
    {
      animate_live = true;
    }
    if (c_line.hasOption("nodisplay"))
    {
      display_stats = false;
    }
    if (c_line.hasOption("level"))
    {
      String ec_level = c_line.getOptionValue("level");
      if (ec_level.compareToIgnoreCase("L") == 0)
      {
        // 7% correction
        level = ErrorCorrectionLevel.L;
      }
      else if (ec_level.compareToIgnoreCase("M") == 0)
      {
        // 15% correction
        level = ErrorCorrectionLevel.M;
      }
      else if (ec_level.compareToIgnoreCase("Q") == 0)
      {
        // 25% correction
        level = ErrorCorrectionLevel.Q;
      }
      else if (ec_level.compareToIgnoreCase("H") == 0)
      {
        // 30% correction
        level = ErrorCorrectionLevel.H;
      }
    }
    if (c_line.hasOption("format"))
    {
      String bc_format = c_line.getOptionValue("format");
      if (bc_format.compareToIgnoreCase("aztec") == 0)
      {
        format = BarcodeFormat.AZTEC;
      }
      else if (bc_format.compareToIgnoreCase("datamatrix") == 0)
      {
        format = BarcodeFormat.DATA_MATRIX;
      }
      else if (bc_format.compareToIgnoreCase("qr") == 0)
      {
        format = BarcodeFormat.QR_CODE;
      }
      else
      {
        System.err.println("Invalid barcode format");
      }
    }

    BtQrSender animator = new BtQrSender(frame_size, display_stats, image_size, 100/fps, format, level);
    String trace_filename = "";
    if (remaining_args.size() < 2 && !from_stdin && pipe_filename.isEmpty())
    {
      System.err.println("Trace filename must be provided, or options --stdin or --pipe");
      System.exit(ERR_ARGUMENTS);
    }
    // If we read from stdin or a pipe, all filenames are schemas
    // If we read from a file, the first filename contains the trace
    int start_index = 2;
    if (from_stdin || !pipe_filename.isEmpty())
    {
      start_index = 1;
    }
    if (remaining_args.size() > start_index)
    {
      // All remaining arguments are schema files
      for (int i = start_index; i < remaining_args.size(); i++)
      {
        String schema_filename = remaining_args.get(i);
        try
        {
          animator.setSchema(i - start_index, new File(schema_filename));
        } catch (IOException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (ReadException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    // --- Setup message source ---
    if (from_stdin)
    {
      // Input message source is stdin
      animator.readMessages(System.in, false);
    }
    else if (!pipe_filename.isEmpty())
    {
      // Input message source is a named pipe
      try
      {
        animator.readMessages(new FileInputStream(pipe_filename), false);

      }
      catch (FileNotFoundException e)
      {
        System.err.println("Error: pipe " + pipe_filename + " not found");
        System.exit(ERR_IO);
      }
    }
    else
    {
      // Input message source is a file
      trace_filename = remaining_args.get(1);
      animator.readMessages(new File(trace_filename));
    }
    // --- Setup output ---
    if (animate_live || !pipe_filename.isEmpty())
    {
      // Either we asked explicitly for live animation, or we use
      // a pipe (which implies it)
      WindowUpdater wu = new WindowUpdater(animator, 1000/fps);
      CodeDisplayFrame m_window = new CodeDisplayFrame(wu);
      wu.setWindow(m_window);
      m_window.setVisible(true);
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
    else
    {
      if (output_filename.isEmpty())
      {
        // Output image to stdout
        byte[] image = animator.animate(100/fps, image_size, format, level);
        try
        {
          System.out.write(image);
        }
        catch (IOException e)
        {
          System.err.println("ERROR writing image to stdout");
          System.exit(ERR_IO);
        }
      }
      else
      {
        // Output image to file
        animator.animate(output_filename, 100/fps, image_size, format, level);
      }
    }
    System.exit(ERR_OK);
  }

  protected static void decode(CommandLine c_line)
  {
    @SuppressWarnings("unchecked")
    List<String> remaining_args = c_line.getArgList();
    String image_filename = remaining_args.get(1);
    BarcodeFormat format = BarcodeFormat.QR_CODE;
    ZXingReadWrite reader_writer = new ZXingReadWrite();
    if (c_line.hasOption("format"))
    {
      String bc_format = c_line.getOptionValue("format");
      if (bc_format.compareToIgnoreCase("aztec") == 0)
      {
        format = BarcodeFormat.AZTEC;
      }
      else if (bc_format.compareToIgnoreCase("datamatrix") == 0)
      {
        format = BarcodeFormat.DATA_MATRIX;
      }
      else if (bc_format.compareToIgnoreCase("qr") == 0)
      {
        format = BarcodeFormat.QR_CODE;
      }
      else
      {
        System.err.println("Invalid barcode format");
      }
    }
    reader_writer.setBarcodeFormat(format);
    if (c_line.hasOption("purecode"))
    {
      reader_writer.setPureCode(true);
    }
    String result = null;
    try
    {
      result = reader_writer.readCode(new FileInputStream(new File(image_filename)));
    } catch (FileNotFoundException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ReaderException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (result == null)
    {
      System.out.println("Cannot decode");
      System.exit(ERR_CANNOT_DECODE);
    }
    System.out.println(result);
    System.exit(ERR_OK);
  }

  public static void showHeader()
  {
    System.err.println("QRTranslator");
  }

  public static String getFileExtension(String filename)
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
    String extension = getFileExtension(filename);
    return extension.compareToIgnoreCase("mp4") == 0 || extension.compareToIgnoreCase("avi") == 0 || extension.compareToIgnoreCase("mkv") == 0;
  }
}