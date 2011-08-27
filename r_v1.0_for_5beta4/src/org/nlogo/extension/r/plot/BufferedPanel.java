/*
This file is part of NetLogo-R-Extension.

It is copied from the class "JGRBufferedPanel" from JGR - Java Gui for R (see http://www.rosuda.org/JGR/) from 
Markus Helbig/ Simon Urbanek (RoSuDa 2003 - 2005).
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---

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


import java.awt.Dimension;
import java.awt.Graphics;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.javaGD.JGDBufferedPanel;

/**
 * Class BufferedPanel
 * Used as BufferedPanel for the JavaGD plot window.
 * @version 1.0beta
 */
class BufferedPanel extends JGDBufferedPanel
{

  private static final long serialVersionUID = 1L;
  Dimension lastSize;

  public BufferedPanel(double paramDouble1, double paramDouble2)
  {
    super(paramDouble1, paramDouble2);
    this.lastSize = getSize();
  }
  public BufferedPanel(int paramInt1, int paramInt2) {
    super(paramInt1, paramInt2);
    this.lastSize = getSize();
  }

  public void devOff() {
    try {
      JavaGDFrame.engine.parseAndEval("dev.off(" + (this.devNr + 1) + ")");
    } catch (REngineException localREngineException) {
      localREngineException.printStackTrace();
    } catch (REXPMismatchException localREXPMismatchException) {
      localREXPMismatchException.printStackTrace();
    }
  }

  public void initRefresh() {
    try {
    	JavaGDFrame.engine.parseAndEval("try(.C(\"javaGDresize\",as.integer(" + this.devNr + ")),silent=TRUE)");
    } catch (REngineException localREngineException) {
      localREngineException.printStackTrace();
    } catch (REXPMismatchException localREXPMismatchException) {
      localREXPMismatchException.printStackTrace();
    }
  }

  public synchronized void paintComponent(Graphics paramGraphics) {
    Dimension localDimension = getSize();
    if (!localDimension.equals(this.lastSize)) {
      REXP localREXP = null;
      try {
        localREXP = JavaGDFrame.engine.parseAndEval("try(.C(\"javaGDresize\",as.integer(" + this.devNr + ")),silent=TRUE)");
      } catch (REngineException localREngineException) {
        localREngineException.printStackTrace();
      } catch (REXPMismatchException localREXPMismatchException) {
        localREXPMismatchException.printStackTrace();
      }
      if (localREXP != null)
        this.lastSize = localDimension;
      return;
    }
    super.paintComponent(paramGraphics);
  }
}