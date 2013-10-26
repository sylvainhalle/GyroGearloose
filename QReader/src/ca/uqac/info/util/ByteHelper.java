package ca.uqac.info.util;

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
