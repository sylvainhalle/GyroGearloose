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

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.BlobSegment;
import ca.uqac.info.buffertannen.protocol.Sender;

public class FrameEncoderBinary extends FrameEncoder
{  
  /**
   * When the desired number of bytes cannot be read from the input,
   * whether to fill the remaining space by zeros
   */
  protected boolean m_padWithZeros = false;
  
  /**
   * The binary buffer
   */
  protected BitSequence m_binaryBuffer;
  
  public FrameEncoderBinary()
  {
    super();
    m_binaryBuffer = new BitSequence();
  }
  
  /**
   * When the desired number of bytes cannot be read from the input,
   * sets whether to fill the remaining space by zeros.
   * @param pad Set to true to pad, false otherwise
   */
  public void setPadWithZeros(boolean pad)
  {
    m_padWithZeros = pad;
  }
  
  @Override
  public void setSender(Sender sender)
  {
    super.setSender(sender);
    m_chunkByteSize = (m_sender.getMaxDataSize() - BlobSegment.getHeaderSize()) / 8;
  }
  
  protected synchronized BitSequence pollNextFrameLake()
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
          m_binaryBuffer.addAll(new BitSequence(bytes, bytes_read * 8));
        }
      }
      catch (IOException e)
      {
        e.printStackTrace();
        break;
      }
      catch (BitFormatException e)
      {
        e.printStackTrace();
        break;
      }
    } while (bytes_read > 0);
    // Now, send the entire source contents to the sender
    while (!m_binaryBuffer.isEmpty())
    {
      BitSequence to_blob = m_binaryBuffer.truncatePrefix(m_chunkByteSize * 8);
      m_sender.addBlob(to_blob);
    }
    // And now, ask for a frame from the sender
    return m_sender.pollBitSequence();
  }

  @Override
  protected BitSequence pollNextFrameStream()
  {
    // First, handle some real time stats
    long current_time = System.nanoTime();
    m_lastFrameInterval = current_time - m_timeLastFrame;
    m_timeLastFrame = current_time;
    // Before asking sender, read the input source to feed the sender
    while (m_binaryBuffer.size() < m_chunkByteSize * 8)
    {
      byte[] bytes = new byte[m_chunkByteSize];
      int bytes_read = 0;
      try
      {
        bytes_read = m_inStream.read(bytes);
        if (bytes_read <= 0)
        {
          // Nothing could be read from input source
          if (m_binaryBuffer.isEmpty())
          {
            //... and the binary buffer is empty: return null
            return null;
          }
          // Otherwise, make a blob segment out of whatever we have
          break;
        }
        m_binaryBuffer.addAll(new BitSequence(bytes, bytes_read * 8));
      }
      catch (IOException e)
      {
        e.printStackTrace();
        break;
      }
      catch (BitFormatException e)
      {
        e.printStackTrace();
        break;
      }
    }
    int buf_size = m_binaryBuffer.size();
    if (m_padWithZeros && buf_size < m_chunkByteSize * 8)
    {
      // Binary buffer does not contain enough data: we pad what remains
      // up to desired length with zeros
      for (int i = buf_size; i < m_chunkByteSize * 8; i++)
      {
        m_binaryBuffer.add(false);
      }
    }
    BitSequence to_blob = m_binaryBuffer.truncatePrefix(m_chunkByteSize * 8);
    m_sender.addBlob(to_blob);
    BitSequence out = m_sender.pollBitSequence();
    return out;
  }
}
