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

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

/**
 * Class MenuBar
 * Used to add a menubar to the JavaGD plot window with copy and save events.
 * @version 1.0beta
 */
public class MenuBar extends JMenuBar 
implements ActionListener, ItemListener 
{
	private static final long serialVersionUID = 1L;
	private JMenu menu;
	private JMenuItem menuItem;
	private JavaGDFrame jgd;
	
	/**
	 * Constructor
	 * @param jgd a JavaGDFrame instance
	 */
	public MenuBar(JavaGDFrame jgd) 
	{
		super();
		this.jgd = jgd;
		
		//Build first menu (Copy to Clipboard) in the menu bar
		menu = new JMenu("File...");
		menuItem = new JMenuItem("Copy to Clipboard",KeyEvent.VK_C);
		menuItem.setMnemonic(KeyEvent.VK_C);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("copyImg");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		this.add(menu);
			
		//Build the second menu (Save as...)
		menu = new JMenu("Save As...");
		this.add(menu);
	
		//a group of JMenuItems for different file types
		menuItem = new JMenuItem("PDF",KeyEvent.VK_P);
		menuItem.setMnemonic(KeyEvent.VK_P);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_P, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("save_pdf");
		menuItem.addActionListener(this);
		menu.add(menuItem);
	
		menuItem = new JMenuItem("EPS",KeyEvent.VK_E);
		menuItem.setMnemonic(KeyEvent.VK_E);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("save_eps");
		menuItem.addActionListener(this);
		menu.add(menuItem);
	
		menuItem = new JMenuItem("PNG",KeyEvent.VK_G);
		menuItem.setMnemonic(KeyEvent.VK_G);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_G, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("save_png");
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menuItem = new JMenuItem("JPEG",KeyEvent.VK_J);
		menuItem.setMnemonic(KeyEvent.VK_J);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_J, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("save_jpeg");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		menuItem = new JMenuItem("BMP",KeyEvent.VK_B);
		menuItem.setMnemonic(KeyEvent.VK_B);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_B, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("save_bmp");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		menuItem = new JMenuItem("TIFF",KeyEvent.VK_T);
		menuItem.setMnemonic(KeyEvent.VK_T);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_T, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("save_tiff");
		menuItem.addActionListener(this);
		menu.add(menuItem);
	}

	/**
	 * Method to open a save dialog
	 * @param paramString File type
	 * @return selected file
	 */
	String getFileDlg(String paramString) {
		String str = null;
		javax.swing.JFileChooser localFileSelector = new javax.swing.JFileChooser();
		localFileSelector.setDialogTitle("Select a "+paramString+" file");
		int returnVal = localFileSelector.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = localFileSelector.getSelectedFile();
			str = file.getAbsolutePath();
			if (!str.endsWith(paramString))
				str = str + "." + paramString;              
		}     	
		return str;
	}
	  
	/**
	 * Method to replace slashes
	 * @param paramString
	 * @return
	 */
	String escapeStr(String paramString) {
		int i = 0;
	    StringBuffer localStringBuffer = new StringBuffer(paramString.length() + 16);
	    while (i < paramString.length()) {
	      char c = paramString.charAt(i);
	      if ((c == '"') || (c == '\\'))
	    	  localStringBuffer.append("\\");
	      localStringBuffer.append(c);
	      i++;
	    }
	    return localStringBuffer.toString();
	}

	/**
	 * Method to execute the save or copy events
	 * @param paramActionEvent The action event
	 */
	@Override
	public void actionPerformed(ActionEvent paramActionEvent) {
	    String str1 = paramActionEvent.getActionCommand();
	    if (str1.equals("copyImg")) {
	      ImageSelection.copyComponent((java.awt.Component)jgd.c, false, true);
	    }
	    if (str1.startsWith("save")) {
	      String str2 = str1.split("_")[1];
	      String str3 = getFileDlg(str2);
	      if (str3 != null)
	      {
	    	try {
	          str3 = escapeStr(str3);
	          if (str2.equals("pdf")) {
	        	jgd.engine.parseAndEval(".javaGD.copy.device(devNr=" + (jgd.getDeviceNumber() + 1) + ", device=pdf, file=\"" + str3 + "\",onefile=TRUE, paper=\"special\")");
	          }
	          else if (str2.equals("eps")) {
	        	jgd.engine.parseAndEval(".javaGD.copy.device(devNr=" + (jgd.getDeviceNumber() + 1) + ", device=postscript, file=\"" + str3 + "\",onefile=FALSE, paper=\"special\")");
	          }
	          else if (str2.equals("png")) {
    	          jgd.engine.parseAndEval(".javaGD.copy.device(devNr=" + (jgd.getDeviceNumber() + 1) + ", device=png, file=\"" + str3 + "\",units=\"in\",res=244)");
	          }
	          else if (str2.equals("jpeg")) {
		        jgd.engine.parseAndEval(".javaGD.copy.device(devNr=" + (jgd.getDeviceNumber() + 1) + ", device=jpeg, file=\"" + str3 + "\",units=\"in\",res=72)");
	          }
	          else if (str2.equals("bmp")) {
	        	jgd.engine.parseAndEval(".javaGD.copy.device(devNr=" + (jgd.getDeviceNumber() + 1) + ", device=bmp, file=\"" + str3 + "\",units=\"in\",res=244)");
	          }
	          else if (str2.equals("tiff")) {
	        	jgd.engine.parseAndEval(".javaGD.copy.device(devNr=" + (jgd.getDeviceNumber() + 1) + ", device=tiff, file=\"" + str3 + "\",units=\"in\",res=244)");
	          }
	    	}
	        catch (Exception localException)
	        {
	        	JOptionPane.showMessageDialog(null, localException, "Error in R-Extension", JOptionPane.INFORMATION_MESSAGE);
	        }
	      }
	    }
	}

	/**
	 * currently unused
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
	}
}
