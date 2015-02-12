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
    int exit_code = ERR_OK;
    if (args.length < 1)
    {
      AnimateWorkflow.showUsage();
      ReadWorkflow.showUsage();
      System.exit(ERR_ARGUMENTS);
    }
    String action = args[0];
    if (action.compareToIgnoreCase("animate") == 0)
    {
      exit_code = AnimateWorkflow.mainLoop(args);
    }
    else if (action.compareToIgnoreCase("read") == 0)
    {
      exit_code = ReadWorkflow.mainLoop(args);
    }
    else if (action.compareToIgnoreCase("-h") == 0 || action.compareToIgnoreCase("--help") == 0)
    {
      AnimateWorkflow.showUsage();
      ReadWorkflow.showUsage();
    }
    else
    {
      System.err.println("Invalid action. Valid values are `animate', `read'.");
      System.exit(ERR_ARGUMENTS);      
    }
    System.exit(exit_code);
  }

}
