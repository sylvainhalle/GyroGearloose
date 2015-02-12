/*
        BeepBeep, an LTL-FO+ runtime monitor with XML events
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
package ca.uqac.lif.util;

/**
 * Token buffer specialized for XML documents. Whereas the "plain"
 * {@link TokenBuffer} looks literally for the start and end separators,
 * the XmlTokenBuffer takes as input an element name, and looks for
 * an opening tag that contains this element name, but may also
 * contain additional attributes. For example, the
 * XmlTokenBuffer will correctly locate the token:
 * <pre>
 * &lt;abc attrib="2"&gt;contents&lt;/abc&gt;
 * </pre>
 * when asked to look for "<tt>abc</tt>" (while the standard
 * TokenBuffer will not).
 * @author sylvain
 *
 */
public class XmlTokenBuffer extends TokenBuffer
{
  public XmlTokenBuffer(String separator_begin, String separator_end)
  {
    super(separator_begin, separator_end);
  }
  
  @Override
  public String nextToken()
  {
    int left_beg = m_bufferedContents.indexOf("<" + m_separatorBegin);
    if (left_beg < 0)
      return "";
    int left_end = m_bufferedContents.indexOf(">", left_beg);
    if (left_end < 0)
      return "";
    int right_beg = m_bufferedContents.indexOf("</" + m_separatorBegin + ">");
    if (right_beg < 0)
      return "";
    String out = m_bufferedContents.substring(left_end + 1, right_beg);
    m_bufferedContents.delete(left_beg, right_beg + m_separatorBegin.length() + 3);
    return out;
  }
}
