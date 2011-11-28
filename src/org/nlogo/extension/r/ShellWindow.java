package org.nlogo.extension.r;

/*
This file is part of NetLogo-R-Extension.

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


import java.io.BufferedReader;
import java.io.InputStreamReader;	
import org.nlogo.api.CompilerException;
import org.nlogo.api.ErrorSource;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.ExtensionObject;
import org.nlogo.api.Primitive;
import org.rosuda.REngine.REngine;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;
import javax.swing.*;

import java.awt.event.KeyListener;

/**
 * Class InteractiveShell
 * Used to have an interactive R shell.
 * Because NetLogo can't cast an Object stored in the ExtensionManager via ex.storeObject/ex.retrieveObject 
 * to classes and interfaces other than java natives and NetLogo ones, using NetLogo interfaces is one
 * (dirty) way, to store the ShellWindow object.
 * @version 1.0beta
 */
class ShellWindow 
extends javax.swing.JFrame 
implements 
KeyListener,
ActionListener,
org.rosuda.REngine.REngineOutputInterface,
org.rosuda.REngine.REngineInputInterface,
org.rosuda.REngine.REngineCallbacks,
org.nlogo.api.ExtensionManager 
{
	private static final long serialVersionUID = 1L;	
	final JFileChooser fc = new JFileChooser();
	javax.swing.filechooser.FileFilter ff = new javax.swing.filechooser.FileFilter() {
        public boolean accept(File f) {
            return f.getName().toLowerCase().endsWith(".rhistory") || f.isDirectory();
        }
        public String getDescription() {
            return "R History File (*.RHistory)";
        }
	};
	private final JSplitPane consolePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	/**
	 * the upper output area (console output)
	 */
	public JTextArea output = new JTextArea();
	/**
	 * the lower input area (console input)
	 */
	public JTextArea input = new JTextArea();
	/**
	 * Vector of Strings to store the commands written into the input area (or loaded from RHistory file)
	 */
	private Vector<String> cmd_history = new Vector<String>();
	/**
	 * Current index in command history while browsing throw history 
	 */
	private int cmd_hist_index = 0;
	/**
	 * Boolean to check, if we browse the first time throw history or we should reset the current index
	 */
	private boolean cmd_hist_first = true;
	/**
	 * An object to synchronize the execution of R commands
	 */
	private ConsoleSync rSync = null;
	

	/**
     * Constructor of ShellWindow
     * @param rSync an Object of ConsoleSync to synchronize the execution of R commands
     */
    public ShellWindow(ConsoleSync rSync) {
    	super("R Console");
    	this.rSync = rSync;
    	fc.setFileFilter(ff);
    	JScrollPane sp1 = new JScrollPane(output);
    	sp1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		consolePanel.setTopComponent(sp1);
		
		JScrollPane sp2 = new JScrollPane(input);
		sp2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		consolePanel.setBottomComponent(sp2);
		consolePanel.setDividerLocation(((int) ((double) this.getHeight() * 0.65)));

		
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent evt) {
				super.componentResized(evt);
				consolePanel.setDividerLocation(((int) ((double) getHeight() * 0.65)));
			}
		});
		
		/* Window closing event ,currently unused */
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {				
			}
		});
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(consolePanel, BorderLayout.CENTER);
		this.setMinimumSize(new Dimension(555, 650));
		this.setSize(new Dimension(555,650));
		input.addKeyListener(this);
		output.setEditable(false);
				
		output.setText("\nR-Extension Notes:\n\n" +
				"------------------\n"+
				"This is a special interactive shell.\n" +
				"Please note, that if you open windows from R (e.g. plot windows)\n" +
				"they are not refreshed while you have no idle loop running\n" +
				"(see the plot example for implementing an idle loop\n" +
				"from NetLogo).\n\n" +
				"Write your R commands into the lower area (input) and drop\n" +
				"Ctrl+Enter for submitting them.\n" +
				"Use context menu to clear, save or load history.\n" +
				"Use the page up/down keys in the lower area (input) to browse\n" +
				"the current history.\n" +
				"Variable created from NetLogo live in nl.env environment.\n" +
				"Use get('varname',nl.env) to access them and\n" +
				"ls(nl.env) to get a list of variables.\n" +				
				"------------------\n\n"
				);

		/* Context menu */
		final JPopupMenu contextMenu = new JPopupMenu("Edit");
		contextMenu.add(makeMenuItem("Clear all"));
		contextMenu.add(makeMenuItem("Clear Window"));
		contextMenu.add(makeMenuItem("Clear History"));
		contextMenu.add(makeMenuItem("Save History to File"));
		contextMenu.add(makeMenuItem("Load Histroy from File"));
		output.setComponentPopupMenu(contextMenu);
		input.setComponentPopupMenu(contextMenu);
		output.setInheritsPopupMenu(true);
		input.setInheritsPopupMenu(true);
    }

    /**
     * A method to handle the context menu
     * @param e The calling action event
     */
    public void actionPerformed(ActionEvent e) {
    	output.append(e.getActionCommand() + "\n");
    	if (e.getActionCommand() == "Clear all")
    	{ 
    		startFullCompilation(); 
    		return;
    	}    
    	if (e.getActionCommand() == "Clear Window")
    	{ 
    		output.setText(""); 
    		return;
    	} 
    	if (e.getActionCommand() == "Clear History")
    	{ 
    		cmd_history.clear(); 
    		return;
    	} 
    	if (e.getActionCommand() == "Save History to File")
    	{
    		try
    		{   
    			fc.setDialogTitle("Select file where to save R History...");
    			int returnVal = fc.showSaveDialog(null);
    			if (returnVal == JFileChooser.APPROVE_OPTION) {
    	            File file = fc.getSelectedFile();
    	            String filename = file.getAbsolutePath();
    	            if (!file.getName().toLowerCase().endsWith(".rhistory"))
    	            {
    	            	filename = file.getAbsolutePath()+".RHistory";
    	            }	            	
    	            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
    	            java.util.Iterator<String> it = cmd_history.iterator();
    	            while (it.hasNext())
    	            {
        	            out.write(it.next().toString());
        	            out.newLine();
    	            }
    	            out.close();
    			}
    		}
    		catch (Exception ex)
    		{
    			System.out.println("R-Extension: Error while writing History to file!\n"+ex);
    		}
    		return;
    	}
    	if (e.getActionCommand() == "Load Histroy from File")
    	{
    		try
    		{
    			fc.setDialogTitle("Select RHistory file to be loaded...");
    			int returnVal = fc.showOpenDialog(null);
    			if (returnVal == JFileChooser.APPROVE_OPTION) {		
    				File file = fc.getSelectedFile();
    			    FileInputStream fstream = new FileInputStream(file.getAbsoluteFile());
    			    DataInputStream in = new DataInputStream(fstream);
   			        BufferedReader br = new BufferedReader(new InputStreamReader(in));
    			    String strLine;
    			    cmd_history.clear();
    			    while ((strLine = br.readLine()) != null)   {
    			    	cmd_history.add(strLine);
    			    }
    			    cmd_hist_index = cmd_history.size()-1;
    			    br.close();
    			}
    		}
    		catch (Exception ex)
    		{
    			System.out.println("R-Extension: Error while writing History to file!\n"+ex);
    		}
    		return;
    	}
    }

    /**
     * A method to create context menu
     * @param label Name of the context item added
     * @return A menu item
     */
    private JMenuItem makeMenuItem(String label) {
    	JMenuItem item = new JMenuItem(label);
    	item.addActionListener(this);
    	return item;
    }

	/**
	 * Execute a command and add it into history.
	 * @param cmd command for execution
	 */
	public void execute(String cmd) {
		String[] cmdArray = cmd.split("\n");		
		String c = null;
		for (int i = 0; i < cmdArray.length; i++) {
			c = cmdArray[i];
			rSync.triggerNotification(c.trim());
		}
	}
    
	
	/**
	 * Method to add a path to JavaClassPath
	 * @param s The path to be added
	 * @throws Exception
	 */
	public static void addPath(String s) throws Exception {
		  java.io.File f = new java.io.File(s);
		  java.net.URL u = f.toURL();
		  java.net.URLClassLoader urlClassLoader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
		  Class urlClass = java.net.URLClassLoader.class;
		  Method method = urlClass.getDeclaredMethod("addURL", new Class[]{java.net.URL.class});
		  method.setAccessible(true);
		  method.invoke(urlClassLoader, new Object[]{u});
	}
    
    /* -------------- stuff due to KeyListener --------------------*/
	/**
	 * currently unused
	 * @param ke The key pressed
	 */
	public void keyTyped(KeyEvent ke) {
	}
		
	/**
	 * Method to handle key pressed event
	 * @param ke The key pressed
	 */
	public void keyPressed(KeyEvent ke) {
		if (ke.isControlDown())
		{
			if (ke.getKeyCode() == KeyEvent.VK_ENTER )
			{
				String cmd = input.getText().trim();
				// add command to history
				cmd_history.add(cmd);
				cmd_hist_index = cmd_history.size()-1;
				cmd_hist_first = true;
				// reset input area
				input.setText("");
				input.setCaretPosition(0);
				input.requestFocus();
				// avoid closing the R shell
				if(cmd.contains("q()"))
				{
					output.append("You cannot execute q() from inside this shell. \n Please close this window and then NetLogo.");
				}
				else
				{
					execute(cmd);
				}
			}
		}
	}

	/**
	 * Method to handle key released event
	 * @param ke The key released
	 */
	public void keyReleased(KeyEvent ke) {
		if (ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN || ke.getKeyCode() == KeyEvent.VK_PAGE_UP)
		{
			if (cmd_history.size() > 0)
			{
				if (ke.getKeyCode() == KeyEvent.VK_PAGE_UP)
				{
					if (!cmd_hist_first)
					{cmd_hist_index--;}
					if (cmd_hist_index < 0)
					{ cmd_hist_index = cmd_history.size()-1; }
				}
				else 
				{
					cmd_hist_index++;
					if (cmd_hist_index >= cmd_history.size())
					{ cmd_hist_index = 0; }
				}
				cmd_hist_first = false;
				input.setText(cmd_history.get(cmd_hist_index));
			}
		}
	}	

	/* --------the part for Interfaces REngineOutputInterface and REngineInputInterface -----------*/

	/**
	 * currently unused
	 * @param eng The REngine
	 */
	@Override
	public void RFlushConsole(REngine eng) {	
	}

	/**
	 * Method to open a message dialog with an R message
	 * @param eng The REngine
	 * @param message The message text 
	 */
	@Override
	public void RShowMessage(REngine eng, java.lang.String message) {		
		JOptionPane.showMessageDialog(this, message, "R Message",
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Method to write to the R console (and output area)
	 * @param eng The REngine
	 * @param text The command
	 * @param oType Type of command, error or normal 
	 */
	@Override
	public void RWriteConsole(REngine eng, java.lang.String text, int oType) {
		output.append(text);
		output.setCaretPosition( output.getDocument().getLength() );
	}

	/**
	 * Method to read from the R console (and write result to output area)
	 * @param eng The REngine
	 * @param prompt The prompt sign
	 * @param addToHistory currently unused
	 */
	@Override
	public String RReadConsole(REngine eng, java.lang.String prompt, int addToHistory) {
		output.append(prompt);	
		String s = rSync.waitForNotification();
		try {
			output.append(s + "\n");
			output.setCaretPosition( output.getDocument().getLength() );
		} catch (Exception e) {
		}
		return (s == null || s.length() == 0) ? "\n" : s + "\n";
	}

	
	/*---------------------------------stuff due to ExtensionsManager -------------------*/

	/**
	 * Method used to wrap the isVisible method
	 */
	@Override
	public boolean anyExtensionsLoaded() {
		return this.isVisible();
	}

	/**
	 * Method used to wrap the setVisible(true) method call
	 */
	@Override
	public void finishFullCompilation() {
		this.setVisible(true);
	}
	
	/** 
	 * Method used to wrap the setVisible(false) method call 
	 */
	@Override
	public void reset() {
		this.setVisible(false);
	}
	
	/** 
	 * Method used to empty the output area and history 
	 */
	@Override
	public void startFullCompilation() {
		output.setText("");
		cmd_history.clear();
		cmd_hist_first = true;
	}

	/**
	 * Method used to activate JavaGD-plot support
	 * JavaGD-Package must be installed.
	 * @param arg0 unused
	 */
	@Override
	public void storeObject(Object arg0) {	
		try
		{
			/*
			// old way with environment variable JAVAGD_HOME
			final String filesep = System.getProperty("file.separator");
			String filepath = System.getenv("JAVAGD_HOME");
        	JavaLibraryPath.addFile(filepath+filesep+"/java/javaGD.jar");
			JavaLibraryPath.addFile("extensions/r/rplot.jar");			
			org.nlogo.extension.r.plot.JavaGDFrame.engine = Entry.rConn.rConnection;			
	        */
			Entry.rConn.execute(Entry.rConn.rConnection, "require(JavaGD)", Entry.rConn.WorkingEnvironment, true);
			// new way: first load JavaGD package, then get path to the package and load the javaGD.jar
			String filepath = Entry.rConn.execute(Entry.rConn.rConnection, ".path.package(\"JavaGD\")", null, true).asString();
	        final String filesep = System.getProperty("file.separator");
			JavaLibraryPath.addFile(filepath+filesep+"/java/javaGD.jar");
			JavaLibraryPath.addFile("extensions/r/rplot.jar");			
			org.nlogo.extension.r.plot.JavaGDFrame.engine = Entry.rConn.rConnection;				        
	        Entry.rConn.execute(Entry.rConn.rConnection, "Sys.setenv('JAVAGD_CLASS_NAME'='org/nlogo/extension/r/plot/JavaGDFrame')", Entry.rConn.WorkingEnvironment, true);
	        Entry.rConn.execute(Entry.rConn.rConnection, "options(device=JavaGD)", Entry.rConn.WorkingEnvironment, true);
		}
		catch (Exception ex)
		{
        	JOptionPane.showMessageDialog(null, "Error during configuration of JavaGD plot device: "+ex, "Error in R-Extension", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	
	/*------------------------- unused methods, due to ExtensionManager Interface --------------------*/
	
	/**
	 * currently unused
	 */
	@Override
	public void addToLibraryPath(Object arg0, String arg1) {
	}
	
	/**
	 * currently unused
	 */
	@Override
	public String dumpExtensionPrimitives() {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public String dumpExtensions() {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public List<String> getExtensionNames() {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public org.nlogo.api.File getFile(String arg0) throws ExtensionException {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public List<String> getJarPaths() {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public String getSource(String arg0) throws IOException {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public void importExtension(String arg0, ErrorSource arg1)
			throws CompilerException {		
	}

	/**
	 * currently unused
	 */
	@Override
	public ExtensionObject readExtensionObject(String arg0, String arg1,
			String arg2) throws CompilerException {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public Object readFromString(String arg0) throws CompilerException {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public Primitive replaceIdentifier(String arg0) {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public String resolvePath(String arg0) {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public String resolvePathAsURL(String arg0) {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public Object retrieveObject() {
		return null;
	}

	/**
	 * currently unused
	 */
	@Override
	public boolean profilingEnabled() {
		return false;
	}
}