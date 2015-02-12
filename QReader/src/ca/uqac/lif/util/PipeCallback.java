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

/**
 * The PipeCallback interface defines a method, called notify, that an object
 * expects to be called when the PipeReader has read a complete fragment of
 * its input (the token).
 * @author sylvain
 *
 */
public interface PipeCallback<T>
{  
  public void notify(T token, long buffer_size) throws CallbackException;

  public class CallbackException extends EmptyException
  {
    /**
     * Dummy UID
     */
    private static final long serialVersionUID = 1L;

    public CallbackException(String message)
    {
      super(message);
    }
  }
}
