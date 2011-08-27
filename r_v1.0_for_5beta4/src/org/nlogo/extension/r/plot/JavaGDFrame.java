/*
This file is part of NetLogo-R-Extension.

It is inspired by the class "JavaGD" from JGR - Java Gui for R (see http://www.rosuda.org/JGR/) from 
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

package org.nlogo.extension.r.plot;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import org.rosuda.javaGD.GDInterface;
import org.rosuda.javaGD.JGDPanel;

/**
 * Class JavaGDFrame
 * Used as custom window for JavaGD plot device.
 * @version 1.0beta
 */
public class JavaGDFrame extends GDInterface
  implements WindowListener
{
  javax.swing.JFrame jfr;
  static int count = 0;
  public static org.rosuda.REngine.REngine engine = null; 

  public void gdOpen(double paramDouble1, double paramDouble2)
  {
    this.open = true;
    if (this.jfr != null) {
      gdClose();
    }
    this.jfr = new javax.swing.JFrame("JavaGD from R-Extension")
    {
      private static final long serialVersionUID = 8263748858987338205L;

      public void dispose()
      {
        if (JavaGDFrame.this.c != null)
          JavaGDFrame.this.executeDevOff();
        super.dispose();
      }
    };
    this.jfr.addWindowListener(this);
    this.jfr.setJMenuBar(new MenuBar(this));
    this.jfr.setDefaultCloseOperation(2);
    this.c = new BufferedPanel(paramDouble1, paramDouble2);
    this.jfr.getContentPane().add((JGDPanel)this.c);
    this.jfr.setSize((int)paramDouble1, (int)paramDouble2);
    this.jfr.pack();
    this.jfr.setVisible(true);
  }

  public void gdNewPage(int paramInt)
  {
    super.gdNewPage(paramInt);
    this.jfr.setTitle("JavaGD for R-Extension (" + (getDeviceNumber() + 1) + ")" + (this.active ? " *active*" : ""));
  }

  public void gdActivate()
  {
    super.gdActivate();
    this.jfr.toFront();
    this.jfr.setTitle("JavaGD for R-Extension " + (getDeviceNumber() > 0 ? "(" + (getDeviceNumber() + 1) + ")" : "") + " *active*");
  }

  public void gdDeactivate()
  {
    super.gdDeactivate();
    this.jfr.setTitle("JavaGD for R-Extension (" + (getDeviceNumber() + 1) + ")");
  }

  public void gdClose()
  {
    super.gdClose();
    if (this.jfr != null) {
      this.c = null;
      this.jfr.getContentPane().removeAll();
      this.jfr.dispose();
      this.jfr = null;
    }
  }

  /**
   * Method executes window closing
   */
  public void executeDevOff()
  {
    if ((this.c == null) || (this.c.getDeviceNumber() < 0)) return; try
    {
    	engine.parseAndEval("try({ dev.set(" + (this.c.getDeviceNumber() + 1) + "); dev.off()},silent=TRUE)"); 
    	
    } catch (Exception localException) {
      localException.printStackTrace();
    }
  }
  
  /**
   * Method called when closing plot window
   */
  public void windowClosing(WindowEvent paramWindowEvent) {
    if (this.c != null)
      executeDevOff();
  }

  /**
   * currently unused
   */
  public void windowClosed(WindowEvent paramWindowEvent)
  {
  }

  /**
   * currently unused
   */
  public void windowOpened(WindowEvent paramWindowEvent)
  {
  }

  /**
   * currently unused
   */
  public void windowIconified(WindowEvent paramWindowEvent)
  {
  }

  /**
   * currently unused
   */
  public void windowDeiconified(WindowEvent paramWindowEvent)
  {
  }

  /**
   * currently unused
   */
  public void windowActivated(WindowEvent paramWindowEvent)
  {
  }

  /**
   * currently unused
   */
  public void windowDeactivated(WindowEvent paramWindowEvent)
  {
  }
}