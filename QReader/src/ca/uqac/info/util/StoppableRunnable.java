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
   * When the thread is suspended, amount of time (in ms) to sleep
   * before checking again
   */
  private static final int s_idleSleepInterval = 100;
  
  /**
   * An enum representing the current status of the action loop
   * @author sylvain
   */
  public static enum LoopStatus {ACTIVE, FINISHED, SUSPENDED};
  
  /**
   * All extensions to StoppableRunnable must implement this method.
   * It will be called in a loop until either 1) the thread is stopped
   * from the outside using {@link #stop()}, or 2) the method
   * {@link #actionLoop()} returns SUSPEND or FINISHED
   * (i.e. the thread can stop itself in this way).
   */
  public abstract LoopStatus actionLoop();

  @Override
  public final void run()
  {
    m_active = true;
    while (m_active)
    {
      // If thread is suspended, wait until someone starts it again
      while (m_suspended)
      {
        //...unless someone squarely stops it
        if (!m_active)
          break;
        try
        {
          // Sleep for some time
          Thread.sleep(s_idleSleepInterval);
        } catch (InterruptedException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      // We are here: the thread is not suspended anymore
      LoopStatus run_again = actionLoop();
      if (run_again == LoopStatus.FINISHED)
      {
        // Actionloop itself elected to stop the loop: break
        m_active = false;
      }
      else if (run_again == LoopStatus.SUSPENDED)
      {
        // Actionloop itself elected to suspend the loop
        m_suspended = true;
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
  
  /**
   * Temporarily suspends the thread. The loop can be
   * restarted again by calling {@link #resume()}.
   */
  public final void suspend()
  {
    if (!m_suspended)
    {
      m_suspended = true;
    }
  }
  
  /**
   * Resumes a previously suspended loop. This has no effect
   * if the loop is already running.
   */
  public final void resume()
  {
    if (m_suspended)
    {
      m_suspended = false;
    }
  }
  
  /**
   * Toggles the execution of the action loop (either suspends
   * or resumes, depending on current state).
   */
  public final void toggle()
  {
    if (m_suspended)
    {
      resume();
    }
    else
    {
      suspend();
    }
  }

}
