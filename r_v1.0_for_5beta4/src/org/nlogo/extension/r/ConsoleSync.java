package org.nlogo.extension.r;

/*
This file is part of NetLogo-R-Extension.

It is adapted from the class "ConsoleSync" from JGR - Java Gui for R (see http://www.rosuda.org/JGR/) from 
Markus Helbig/ Simon Urbanek (RoSuDa 2003 - 2005).
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---

Contact: jthiele at gwdg.de
Copyright (C) 2009-2011 Jan C. Thiele

NetLogo-R-Extension is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with NetLogo-R-Extension.  If not, see <http://www.gnu.org/licenses/>.

Linking this library statically or dynamically with other modules is making a combined work based on this library.  
Thus, the terms and conditions of the GNU General Public License cover the whole combination.

As a special exception, the copyright holders of this library give you permission to link this library with independent modules to produce an executable, 
regardless of the license terms of these independent modules, and to copy and distribute the resulting executable under terms of your choice, 
provided that you also meet, for each linked independent module, the terms and conditions of the license of that module. 
An independent module is a module which is not derived from or based on this library. 
If you modify this library, you may extend this exception to your version of the library, but you are not obligated to do so. 
If you do not wish to do so, delete this exception statement from your version.
*/

import java.util.Vector;

/**
 * Class ConsoleSync
 * Notifys when commands are in the queue and send them.
 */
public class ConsoleSync {
	Vector<String> msgs;

	
	public ConsoleSync() {
		msgs = new Vector<String>();
	}

	private boolean notificationArrived = false;

	/**
	 * this internal method waits until {@link #triggerNotification} is called
	 * by another thread. It is implemented by using {@link wait()} and checking
	 * {@link notificationArrived}.
	 */
	public synchronized String waitForNotification() {
	while (!notificationArrived)		
			//try {
				while (!this.notificationArrived)
				      try
				      {
				        wait(100L);
				        if (Entry.rConn.rConnection != null)
							((org.rosuda.REngine.JRI.JRIEngine)Entry.rConn.rConnection).getRni().rniIdle();
				      } catch (InterruptedException localInterruptedException) {
				      }
				// wait();
				//wait(100);
				//if (this.R != null)
				//	this.R.getRni().rniIdle();
				//if (JGR.R != null)
				//	JGR.R.rniIdle();
			//} catch (InterruptedException e) {
			//}
			
		String s = null;
		if (msgs.size() > 0) {
			s = (String) msgs.elementAt(0);
			msgs.removeElementAt(0);
		}
		if (msgs.size() == 0)
			notificationArrived = false;
		return s;
	}

	/**
	 * this methods awakens {@link #waitForNotification}. It is implemented by
	 * setting {@link #notificationArrived} to <code>true</code>, setting
	 * {@link #lastNotificationMessage} to the passed message and finally
	 * calling {@link notifyAll()}.
	 */
	public synchronized void triggerNotification(String msg) {
		notificationArrived = true;
		msgs.addElement(msg);
		notifyAll();
	}
}
