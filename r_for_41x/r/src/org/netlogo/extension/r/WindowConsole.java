/*
This file is part of NetLogo-R-Extension.

Contact: jthiele at gwdg.de
Copyright (C) 2009 Jan C. Thiele

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



package org.netlogo.extension.r;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.rosuda.JRI.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener; 
import javax.swing.*;

/**
 * Class WindowConsole, adapted/copied from the examples of rJava/JRI
 * Used to watch the reaction of R in an console.
 */
class WindowConsole implements org.rosuda.JRI.RMainLoopCallbacks, ActionListener 
{
    JFrame f;
    PrintStream ps; 
    PrintStream es; 
    public JTextArea textarea = new JTextArea();
    private JButton button = new JButton("Clear messages...");
    
    /**
     * Constructor
     */
    public WindowConsole() {
        f = new JFrame("R Messages");
        f.getContentPane().add(new JScrollPane(textarea));
        JScrollPane scrollingResult = new JScrollPane(textarea);
        f.add(scrollingResult,BorderLayout.CENTER); 
        button.addActionListener(this); 
        f.add(button,BorderLayout.SOUTH);  
        f.setSize(new Dimension(400,200));
        f.show();
        f.addWindowListener( new WindowAdapter() {
    		public void windowClosing( WindowEvent e ){
    			System.setOut(ps);
    			System.setErr(es);
        	}
    		} ); 
    }

    /**
     * Method to clear the text area.
     * @param event ActionEvent
     */
    public void actionPerformed(ActionEvent event)  
    {  
     //Action that will perform when someone click the button  
     if(event.getSource()==button)  
     {  
      //Clear text in JTextArea  
    	 textarea.setText("");  
     }  
    }
    
    /**
     * Method to redirect the output and error stream to the Window.
     * @param re Rengine
     */
	public void init_ROutputStream(Rengine re) {
		ps = System.out;
		es = System.err;
		System.setOut(new PrintStream(new RConsoleOutputStream(re, 0)));
		System.setErr(new PrintStream(new RConsoleOutputStream(re, 1)));
	}
	
	/**
	 * Method to reopen the Window and redirect the output and error stream to the Window.
	 * @param re Rengine
	 */
	public void reopen(Rengine re) {
		System.setOut(new PrintStream(new RConsoleOutputStream(re, 0)));
		System.setErr(new PrintStream(new RConsoleOutputStream(re, 1)));
		f.show();
	}
	
	/**
	 * Method to close the Window and reset the redirection of the output and error stream.
	 */
	public void close() {
		System.setOut(ps);
		System.setErr(es);
		f.dispose();
		f = null;
	}
	
    public void rWriteConsole(Rengine re, String text, int oType) {
        textarea.append(text);
    }
    
    public void rBusy(Rengine re, int which) {
    }
    
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
    	textarea.append(prompt);
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String s=br.readLine();
            return (s==null||s.length()==0)?s:s+"\n";
        } catch (Exception e) {
        	textarea.append("jriReadConsole exception: "+e.getMessage());
        }
        return null;
    }
    
    public void rShowMessage(Rengine re, String message) {
    	textarea.append("\""+message+"\"");
    }
    
    public String rChooseFile(Rengine re, int newFile) {
		FileDialog fd = new FileDialog(f, (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE);
		fd.show();
		String res=null;
		if (fd.getDirectory()!=null) res=fd.getDirectory();
		if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
		return res;
    }
    
    public void   rFlushConsole (Rengine re) {
	}
    
    public void   rLoadHistory  (Rengine re, String filename) {
    }			
    
    public void   rSaveHistory  (Rengine re, String filename) {
    }	
}