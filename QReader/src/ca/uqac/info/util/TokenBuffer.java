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
package ca.uqac.info.util;

public class TokenBuffer
{
  protected StringBuilder m_bufferedContents;
  protected String m_separatorBegin;
  protected String m_separatorEnd;
  
  public TokenBuffer()
  {
    super();
    m_bufferedContents = new StringBuilder();
  }
  
  public TokenBuffer(String separator_begin, String separator_end)
  {
    this();
    m_separatorBegin = separator_begin;
    m_separatorEnd = separator_end;
  }
  
  public void append(String s)
  {
    if (!s.isEmpty())
    {
      m_bufferedContents.append(s);
    }
  }
  
  public void append(char[] cbuf)
  {
    m_bufferedContents.append(cbuf);
  }

  /**
   * Analyzes the current contents of the buffer; extracts a complete token
   * from it, if any is present 
   * @return The next token, or an empty string if none could be formed
   */
  public String nextToken()
  {
    String s = m_bufferedContents.toString();
    int index = s.indexOf(m_separatorEnd);
    if (index < 0)
    {
      return "";
    }
    int index2 = s.indexOf(m_separatorBegin);
    if (index2 > index)
      index2 = 0;
    String out = s.substring(index2, index + m_separatorEnd.length());
    m_bufferedContents.delete(index2, index + m_separatorEnd.length());
    return out;
  }
  
  public void setSeparators(String begin, String end)
  {
    m_separatorBegin = begin;
    m_separatorEnd = end;
  }
}
