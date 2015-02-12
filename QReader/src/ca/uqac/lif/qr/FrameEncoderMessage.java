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

import java.io.IOException;

import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.message.ReadException;
import ca.uqac.info.buffertannen.protocol.UnknownSchemaException;
import ca.uqac.lif.util.TokenBuffer;

public class FrameEncoderMessage extends FrameEncoder
{
  /**
   * The token buffer used to break the input into individual
   * messages
   */
  protected TokenBuffer m_tokBuf;
  
  public FrameEncoderMessage()
  {
    super();
    m_tokBuf = new TokenBuffer();
    m_tokBuf.setSeparators("", "---");
  }
  
  @Override
  protected BitSequence pollNextFrameLake()
  {
    // First, handle some real time stats
    long current_time = System.nanoTime();
    m_lastFrameInterval = current_time - m_timeLastFrame;
    m_timeLastFrame = current_time;
    BitSequence out = m_sender.pollBitSequence();
    if (out != null)
    {
      return out;
    }
    // out == null => we haven't populated the lake frames yet; do it now by
    // consuming the entire source
    int bytes_read = 0;
    do
    {
      byte[] bytes = new byte[m_chunkByteSize];
      try
      {
        bytes_read = m_inStream.read(bytes);
        if (bytes_read >= 0)
        {
          String contents = new String(bytes);
          m_tokBuf.append(contents);
        }
      }
      catch (IOException e)
      {
        e.printStackTrace();
        break;
      }
    } while (bytes_read > 0);
    // Now, send the entire source contents to the sender
    String message = "";
    do
    {
      message = m_tokBuf.nextToken();
      readMessage(message);
    } while (message != null && !message.isEmpty());
    // And now, ask for a frame from the sender
    out = m_sender.pollBitSequence();
    return out;
  }

  @Override
  protected BitSequence pollNextFrameStream()
  {
    // First, handle some real time stats
    long current_time = System.nanoTime();
    m_lastFrameInterval = current_time - m_timeLastFrame;
    m_timeLastFrame = current_time;
    // Before even polling the sender, read some of the input source to feed the sender
    int chars_read = 0, total_chars_read = 0;
    do
    {
      byte[] chars = new byte[m_chunkByteSize];
      try
      {
        chars_read = m_inStream.read(chars);
        if (chars_read > 0)
        {
          total_chars_read += chars_read;
          String contents = new String(chars);
          m_tokBuf.append(contents);
        }
      }
      catch (IOException e)
      {
        e.printStackTrace();
        break;
      }
    } while (total_chars_read < m_chunkByteSize && chars_read > 0);
    //...then feed the sender from the contents of the token buffer
    String message = "";
    do
    {
      message = m_tokBuf.nextToken();
      readMessage(message);
    } while (message != null && !message.isEmpty());
    //...then ask the sender for a new frame
    BitSequence out = m_sender.pollBitSequence();
    return out;
  }
  
  /**
   * Processes a single message
   * @param part
   */
  protected void readMessage(String part)
  {
    if (part == null)
      return;
    part = part.trim();
    int first_space = part.indexOf(" ");
    if (first_space < 0)
      return;
    String left = part.substring(0, first_space);
    part = part.substring(first_space + 1);
    int schema_number = Integer.parseInt(left);
    try
    {
      m_sender.addMessage(schema_number, part);
    } catch (ReadException e)
    {
      // Ignore if cannot read
      System.err.println("Could not add message");
      return;
    }
    catch (UnknownSchemaException e)
    {
      System.err.println("Unknown schema");
      return;
    }    
  }

}
