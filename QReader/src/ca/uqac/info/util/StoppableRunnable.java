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
package ca.uqac.info.util;

/**
 * A simple extension to the Runnable interface that allows one
 * to stop and suspend a thread from the outside.
 * @author sylvain
 *
 */
public abstract class StoppableRunnable implements Runnable
{
  /**
   * Whether the thread is active
   */
  private boolean m_active = false;
  
  /**
   * Whether the thread is temporarily suspended (but still
   * active)
   */
  private boolean m_suspended = false;
  
  /**
   * All extensions to StoppableRunnable must implement this method.
   * It will be called in a loop until either 1) the thread is stopped
   * from the outside using {@link #stop()}, or 2) the method
   * {@link #actionLoop()} returns false (i.e. the thread can stop
   * itself in this way).
   */
  public abstract boolean actionLoop();

  @Override
  public final void run()
  {
    m_active = true;
    while (m_active)
    {
      // If thread is suspended, wait until someone starts it again
      while (m_suspended)
      {
        if (!m_active)
          break;
      }
      boolean run_again = actionLoop();
      if (!run_again)
      {
        m_active = false;
        break;
      }
    }
  }
  
  /**
   * Stops the loop for this thread
   */
  public final void stop()
  {
    m_active = false;
  }

}
