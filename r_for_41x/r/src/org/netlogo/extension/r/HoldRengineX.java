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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Method;
import org.nlogo.api.CompilerException;
import org.nlogo.api.ErrorSource;
import org.nlogo.api.ExtensionObject;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.File;
import org.nlogo.api.Primitive;


/**
 * Class to store the R connection and to transform data.
 * Because NetLogo can't cast an Object stored in the ExtensionManager via ex.storeObject/ex.retrieveObject 
	 * to classes and interfaces other than java natives and NetLogo ones, using NetLogo interfaces is one
 * (dirty) way, to store the HoldRengineX object - containing the initialization of the jri-object with
 * dynamic library loader.
 * @author JC Thiele
 * @version 0.2
 */
public class HoldRengineX implements org.nlogo.api.ExtensionManager 
{ 
	/**
	 * An instance of the R-Engine, used for communication with R.
	 */
	private org.rosuda.JRI.Rengine rConnection = null;
	/**
	 * An instance of WindowConsole, used to show R messages.
	 */
	private org.netlogo.extension.r.WindowConsole winconsole = null;
	/**
	 * DEBUG parameter, if > 0: DEGBUG on, EVAL-Commands will be printed to MessageWindow or Console.
	 */
	private int DEBUG = 0;
	/**
	 * Parameter to check, if InteractiveShell is open.
	 */
	private boolean shellOpen = false;
	/**
	 * Parameter to check, if MessageWindow is open.
	 */
	private boolean messWinOpen = false;
  
	/**
	 * Constructor
	 * @param reng Instance of org.rosuda.JRI.Rengine
	 */
	public HoldRengineX(org.rosuda.JRI.Rengine reng)
	{
		this.rConnection = reng;
	}

	/**
	 * Method to evaluate a String in R 
	 * and to send back the result as an Object of standard Java data types.
	 * @param arg0 String with R command
	 * @return Object with the return value of the R command, null if an error occurs
	 */
	//@Override
	public Object readFromString(String arg0) {
		try
		{
			org.rosuda.JRI.REXP result =  rConnection.eval(arg0);
      if (this.DEBUG > 0)
      { System.out.println(result); }
			return result.getContent();
		}
		catch (Exception ex)
		{
			System.out.println("Error in eval: "+ex);
			new ExtensionException(ex);
		}
		return null;
	}

	/**
	 * Method to find out class of in-object and put via rni
	 * and to send back the result as an Object of standard Java data types.
	 * @param obj Object containing data, which should be send to rni
	 * @return long containing rni reference value
	 */
	private long putRNI(Object obj)
	{
		long invalue = 0;
		try
		{
		  // create an R-variable with String-value
    	if (obj instanceof String)
    	{
    		invalue = rConnection.rniPutString((String)obj);
    	}
    	// create an R-variable with Double-Value
    	if (obj instanceof Double)
    	{
    		double[] doublein = new double[1];
    		doublein[0] = ((Double)obj).doubleValue();
    		invalue = rConnection.rniPutDoubleArray(doublein);
    	}  
    	// create an R-variable with Boolean-Value
    	if (obj instanceof java.lang.Boolean)
    	{
    		boolean[] boolin = new boolean[1];
    		boolin[0] = (Boolean)obj;
    		invalue = rConnection.rniPutBoolArray(boolin); 	
    	} 
    	if (obj instanceof org.nlogo.api.LogoList)
    	{
    		invalue = putRNI_LogoList(obj);
    	}
		}
		catch (Exception ex)
		{
			System.out.println("Error in putRNI: "+ex);
			new ExtensionException(ex);
		}
		return invalue;
	}

	/**
	 * Method to transform a NetLogo List and put via rni.
	 * @param obj instance of LogoList
	 * @return long containing rni reference value
	 */	
	private long putRNI_LogoList(Object obj)
	{
		long invalue = 0;
		try
		{
    		// cast LogoList
    		org.nlogo.api.LogoList logolist = (org.nlogo.api.LogoList)obj;
    	    // create an R-variable with String[]-value
    	    if (logolist.get(0) instanceof java.lang.String)
    	    {
           		String[] stringlist = new String[logolist.size()];
				for (int i=0; i<logolist.size(); i++)
				{
					stringlist[i] = (String)logolist.get(i);
				}			           		
				invalue = rConnection.rniPutStringArray(stringlist);
    	    }		    	    
    	    // create an R-variable with Double[]-Value
    	    if (logolist.get(0) instanceof java.lang.Double)
    	    {
				double[] dblist = new double[logolist.size()];
				for (int i=0; i<logolist.size(); i++)
				{
					dblist[i] = ((java.lang.Double)logolist.get(i)).doubleValue();
				}     	
				invalue = rConnection.rniPutDoubleArray(dblist); 	
    	   }   		    	   
    	   // create an R-variable with Boolean[]/int[]-Value
    	   if (logolist.get(0) instanceof java.lang.Boolean)
    	   {
    	       	int[] intbool= new int[logolist.size()];
    	       
				for (int i=0; i<logolist.size(); i++)
				{
					if ((Boolean)logolist.get(i))
    	       			intbool[i] = 1;
    	       		else
    	       			intbool[i] = 0;	
				}
				invalue = rConnection.rniPutBoolArrayI(intbool); 	
    	   }
		}
		catch (Exception ex)
		{
			System.out.println("Error in putRNI: "+ex);
			new ExtensionException(ex);
		}
		return invalue;
	}
	
	
	/**
	 * Method to create String-Array with enumeration over value index.
	 * @param obj instance of LogoList
	 * @return String-Array containing enumeration
	 */	
	private String[] iterateOverValues(Object obj)
	{
		String nostring[] = null;
		try
		{
	    	if (obj instanceof org.nlogo.api.LogoList)
	    	{
	    		org.nlogo.api.LogoList logolist = (org.nlogo.api.LogoList)obj;
	    		nostring = new String[logolist.size()];
	    		for (int t=1; t<=logolist.size(); t++)
	    		{
	    			nostring[t-1] = ((Integer)t).toString();
	    		}
	    	}
	    	else
	    	{
	    		nostring = new String[1];
	    		nostring[0] = "1";
	    	}
		}
		catch (Exception ex)
		{
			System.out.println("Error in iterateOverValues: "+ex);
			new ExtensionException(ex);
		}
		return nostring;
	}
	
	/**
	 * Method to create a new R-Variable with Java-Input-Values.
	 * @param arg0 LinkedHashMap
	 */	
	@Override
	public void storeObject(Object arg0) {
		try
		{
			LinkedHashMap inhash = (LinkedHashMap)arg0;
				
			if (inhash.containsKey("put"))
			{
				long inlong = 0;
				String rname = "";
				try
				{
					Iterator it = inhash.keySet().iterator();
			        while (it.hasNext()) {
			        	Object key = it.next();
			        	Object value = inhash.get(key);
			        	if ((key instanceof String) && (value != null))
			        	{
			        		rname = (String)key;
			        		inlong = putRNI(value);
				        	rConnection.rniAssign(rname, inlong, 0); 
			        	}
			        }
				}
				catch (Exception ex)
				{
					System.out.println("Error in storeObject: "+ex);
					new ExtensionException(ex);
				}
			}
	
			/* 
			 	currently not used - unnamed vector will be build with namedvector and
			 	counted rows as names
			if (inhash.containsKey("putvector"))
			{
				long[] inlist = new long[inhash.size()-2];
		       	String rname = "";
		       	int j = 0;
		       	Iterator it = inhash.keySet().iterator();
		       	while (it.hasNext()) {
		       		Object key = it.next();
		       		Object value = inhash.get(key);
		            if (value != null)
		            {
			           	inlist[j] = putRNI(value);
			           	j++;
		            }
		            else
	                {
   		            	if ((String)key != "putvector")
		            		rname = (String)key;
	                }    
		       	}
		       	long vec = rConnection.rniPutVector(inlist);        		
	        	rConnection.rniAssign(rname, vec, 0);
			}
			*/
			
			if (inhash.containsKey("putnamedvector"))
			{
				long[] inlist = new long[inhash.size()-2];
		       	String[] instring = new String[inhash.size()-2];
		       	String rname = "";
		       	int j = 0;
		       	Iterator it = inhash.keySet().iterator();
		       	while (it.hasNext()) {
		       		Object key = it.next();
		       		Object value = inhash.get(key);
		            if (value != null)
		            {
			           	instring[j] = (String)key;
			           	inlist[j] = putRNI(value);
			           	j++;
		            }
		            else
	                {
		            	if ((String)key != "putnamedvector")
		            		rname = (String)key;
	                }    
		       	}
		       	long vec = rConnection.rniPutVector(inlist);
	        	long names = rConnection.rniPutStringArray(instring);
	        		
	        	rConnection.rniSetAttr(vec, "names", names);
	        	rConnection.rniAssign(rname, vec, 0);	        	
			}
			
			if (inhash.containsKey("putdataframe"))
			{
				long[] inlist = new long[inhash.size()-2];
		       	String[] instring = new String[inhash.size()-2];
		       	boolean nostringset = false;
		       	String[] nostring = null;
		       	
		       	String rname = "";
		       	int j = 0;
		       	Iterator it = inhash.keySet().iterator();
		       	while (it.hasNext()) {
		       		Object key = it.next();
		       		Object value = inhash.get(key);
		            if (value != null)
		            {
		            	if (nostringset == false)
		            	{
		            		nostringset = true;
		            		nostring = iterateOverValues(value);	
		            	}
			           	instring[j] = (String)key;
			           	inlist[j] = putRNI(value);
			           	nostring[j] = ((Integer)j).toString();
			           	j++;
		            }
		            else
	                {
   		            	if ((String)key != "putdataframe")
		            		rname = (String)key;	                
   		            }    
		       	}
		       	long vec = rConnection.rniPutVector(inlist);
	        	long names = rConnection.rniPutStringArray(instring);
	        		
	        	rConnection.rniSetAttr(vec, "names", names);
	        	
	        	long no = rConnection.rniPutStringArray(nostring);
	        	rConnection.rniSetAttr(vec, "row.names", no);
	        	long type = rConnection.rniPutString("data.frame");
	        	rConnection.rniSetAttr(vec, "class", type);
	        	
	        	rConnection.rniAssign(rname, vec, 0);        	
			}		
		}
		catch (Exception ex)
		{
			System.out.println(" Error in storeObject... \n"+ex);
			new ExtensionException(ex);
		}
	}
	
	/**
	 * Method to terminate R thread.
	 */	
	//@Override
	public Object retrieveObject() {
		rConnection.end();
		return null;
	}

	/**
	 * Method to clear R session, to set Debugging, to open MessageWindow and InteractiveShell.
	 * @param arg0 String, identifing the action to perform (open Interactive Shell, open WindowConsole, set/end Debugging
	 * @param arg1 Path to history file for loading history
	 * @param arg2 String evaluated by R, used for clear workspace, destroy MessageWindow and reset debugging options
	 * @return null
	 */	
	@Override
	public ExtensionObject readExtensionObject(String arg0, String arg1,
			String arg2) {
		// open/close shell, window, start/stop Debugging
		if (arg0.length() > 0)
		{
			// attach InteractiveShell - only possible, if NetLogo is running from shell/console
			if (arg0 == "interactiveShell")
			{
				attachInteractiveShell();	
			}
			// attach WindowConsole/MessageWindow
			else if (arg0 == "windowConsole")
			{
				attachWindowConsole();
			}
			// set Debugging mode (write results of eval command)
			if (arg0 == "startDebug")
			{
				this.DEBUG = 1;
			}
			// stop Debugging mode
			if (arg0 == "stopDebug")
			{
				this.DEBUG = 0;				
			}
			// set Debugging mode of JRI library 
			if (arg0 == "startJRIDebug")
			{
				rConnection.DEBUG = 1;
			}
			// stop Debugging mode of JRI library
			if (arg0 == "stopJRIDebug")
			{
				rConnection.DEBUG = 0;		
			}
			//rConnection.jriSaveHistory(arg0);
		}
		// load history
		if (arg1.length() > 0)
		{
			//rConnection.jriLoadHistory(arg1);
		}	
		if (arg2.length() > 0)
		{
			// clear workspace
			rConnection.eval(arg2);
			// if MessageWindow was created, then destroy it
			if (winconsole != null)
			{
				winconsole.close();
				winconsole = null;
				messWinOpen = false;
			}
			// reset Debugging parameters
			rConnection.DEBUG = 0;
			this.DEBUG = 0;
		}
		return null;
	}

	
	/* ---------------------------------------------- */
	/* unused methods, added for NetLogo 4.1RC4 */
	/* ---------------------------------------------- */
	
	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public void addToLibraryPath(Object arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public boolean anyExtensionsLoaded() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public void cleanUp() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public String dumpExtensionPrimitives() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public String dumpExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public List<String> getExtensionNames() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public File getFile(String arg0) throws ExtensionException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public List<String> getJarPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public String getSource(String arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public void importExtension(String arg0, ErrorSource arg1)
			throws CompilerException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public Primitive replaceIdentifier(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public String resolvePath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * unused method, added for NetLogo 4.1RC4
	 */
	@Override
	public String resolvePathAsURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Method used to redirect the R shell into the shell/console with interactive mode. 
	 * NetLogo has to be started from console. 
	 */
	public void attachInteractiveShell()
	{
		try
		{
			if (!shellOpen)
			{
				// if MessageWindow was created, destroy it, before creating InteractiveShell
				if (messWinOpen)
				{		
					winconsole.close();
					winconsole = null;
					messWinOpen = false;
				}
				// because we load the jri library at runtime, we can't invoke the addMainLoopCallbacks-Method directly. 
				// Therefore we use the way via reflections - a little bit complicated but keep the extension clean from the rJava/jri stuff.
				// otherwise we would need to have a copy of the jri.jar in the classpath. 
				Class iashell_class = Class .forName("org.netlogo.extension.r.InteractiveShell");
				Object intershellObj = iashell_class.newInstance();
				InteractiveShell tc = (InteractiveShell) intershellObj;
				Class callbacks_class = Class.forName("org.rosuda.JRI.RMainLoopCallbacks");
				Class is_params[] = new Class[] {callbacks_class};
				Class thisClass = Class.forName("org.rosuda.JRI.Rengine");
				Method thisMethod = thisClass.getDeclaredMethod("addMainLoopCallbacks", is_params);
				thisMethod.invoke(rConnection, tc);
	    	    System.out.println("Now the console is yours ... have fun");
	    	    rConnection.startMainLoop();
	    	    shellOpen = true;
			}
		}
		catch (Exception ex)
		{
			System.out.println("error occured in attachInteractiveShell: "+ex);
			new ExtensionException(ex);
		}	
	}
	
	/**
	 * Method used to redirect the outputs of the R shell into a JFrame Window (no interactive mode)e. 
	 */
	public void attachWindowConsole()
	{
		try
		{
			// if winconsole was already created in this session, just reopen it
			if (winconsole == null)
			{
				// because we load the jri library at runtime, we can't invoke the addMainLoopCallbacks-Method directly. 
				// Therefore we use the way via reflections - a little bit complicated but keep the extension clean from the rJava/jri stuff.
				// otherwise we would need to have a copy of the jri.jar in the classpath. 
				Class winconsole_class = Class .forName("org.netlogo.extension.r.WindowConsole");
				Object winconsole_obj = winconsole_class.newInstance();
				winconsole = (WindowConsole) winconsole_obj;
				Class callbacks_class = Class.forName("org.rosuda.JRI.RMainLoopCallbacks");
				Class wc_args[] = new Class[] {callbacks_class};
				Class reng_class = Class.forName("org.rosuda.JRI.Rengine");
				Method thisMethod = reng_class.getDeclaredMethod("addMainLoopCallbacks", wc_args);
				thisMethod.invoke(rConnection, winconsole);
				Class[] opst_args = new Class[] {reng_class};
				Method initros_method = winconsole_class.getDeclaredMethod("init_ROutputStream", opst_args);
				initros_method.invoke(winconsole, rConnection);
				messWinOpen = true;
				shellOpen = false;
			}
			else
			{
				winconsole.reopen(rConnection);
			}
		}
		catch (Exception ex)
		{
			System.out.println("An error occured in attachWindowConsole: "+ex);
			new ExtensionException(ex);
		}
	}	
}
