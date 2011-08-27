/*
This file is part of NetLogo-R-Extension.

It is copied from the class "ImageSelection" from JGR - Java Gui for R (see http://www.rosuda.org/JGR/) from 
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Class ImageSelection
 * Used for the clipboard copy event of the JavaGD plot window. 
 * @version 1.0beta
 */
public class ImageSelection
  implements Transferable
{
  private Image image;

  public ImageSelection(Image paramImage)
  {
    this.image = paramImage;
  }

  public DataFlavor[] getTransferDataFlavors() {
    return new DataFlavor[] { DataFlavor.imageFlavor };
  }

  public boolean isDataFlavorSupported(DataFlavor paramDataFlavor) {
    return DataFlavor.imageFlavor.equals(paramDataFlavor);
  }

  public Object getTransferData(DataFlavor paramDataFlavor) throws UnsupportedFlavorException, IOException {
     if (!(DataFlavor.imageFlavor.equals(paramDataFlavor))) {
       throw new UnsupportedFlavorException(paramDataFlavor);
    }
     return this.image;
  }

  public static ImageSelection setClipboard(Image paramImage)
  {
     ImageSelection localImageSelection = new ImageSelection(paramImage);
     Toolkit.getDefaultToolkit().getSystemClipboard().setContents(localImageSelection, null);
     return localImageSelection;
  }

  public static ImageSelection copyComponent(Component paramComponent, boolean paramBoolean1, boolean paramBoolean2) {
     Dimension localDimension = paramComponent.getSize();
     Image localImage = paramComponent.createImage(localDimension.width, localDimension.height);
     Graphics localGraphics = localImage.getGraphics();
     Graphics2D localGraphics2D = (Graphics2D)localGraphics;
     if (paramBoolean2)
       localGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
     if (paramBoolean1) {
       localGraphics2D.setColor(Color.white);
       localGraphics2D.fillRect(0, 0, localDimension.width, localDimension.height);
    }
     paramComponent.paint(localGraphics2D);
     return setClipboard(localImage);
  }
}

/* Location:           C:\Programme\R\R-2.13.0\library\iplots\java\iplots.jar
 * Qualified Name:     org.rosuda.util.ImageSelection
 * Java Class Version: 1.4 (48.0)
 * JD-Core Version:    0.5.3
 */