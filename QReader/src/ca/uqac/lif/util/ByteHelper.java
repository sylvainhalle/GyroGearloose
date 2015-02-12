/******************************************************************************
Runtime monitor for pipe-based events
Copyright (C) 2013 Sylvain Halle et al.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/
package ca.uqac.lif.util;

public class ByteHelper
{
  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
  
  /**
   * Stolen from here
   * http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
   * @param bytes
   * @return
   */
  public static String bytesToHex(byte[] bytes) {
      char[] hexChars = new char[bytes.length * 2];
      int v;
      for ( int j = 0; j < bytes.length; j++ ) {
          v = bytes[j] & 0xFF;
          hexChars[j * 2] = hexArray[v >>> 4];
          hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
  }
}
