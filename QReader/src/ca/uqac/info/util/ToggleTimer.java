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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Extension to the Timer class with methods to start/stop/toggle.
 * @author sylvain
 *
 */
public class ToggleTimer
{
  /**
   * An internal timer
   */
  protected Timer m_timer = null;
  
  /**
   * Whether the timer is running
   */
  protected boolean m_isRunning;
  
  /**
   * The task that this timer should run at intervals
   */
  protected TimerTaskFactory m_taskFactory;
  
  /**
   * The interval the timer should repeat
   */
  protected long m_period = 1000;
  
  public ToggleTimer()
  {
    super();
    m_isRunning = false;
  }
  
  public ToggleTimer(TimerTaskFactory factory, long period)
  {
    this();
    setTaskFactory(factory);
    setInterval(period);
  }
  
  public boolean isRunning()
  {
    return m_isRunning;
  }
  
  public void setInterval(long period)
  {
    m_period = period;
  }
  
  public void setTaskFactory(TimerTaskFactory factory)
  {
    m_taskFactory = factory;
  }
  
  public void toggle()
  {
    if (m_isRunning)
    {
      stop();
    }
    else
    {
      start();
    }
  }
  
  public void start()
  {
    if (!m_isRunning)
    {
      m_isRunning = true;
      m_timer = new Timer();
      m_timer.scheduleAtFixedRate(m_taskFactory.getTask(), 0, m_period);
    }
  }
  
  public void stop()
  {
    if (m_isRunning)
    {
      m_isRunning = false;
      if (m_timer != null)
        m_timer.cancel();
    }
  }
  
  public static interface TimerTaskFactory
  {
    public TimerTask getTask();
  }
}
