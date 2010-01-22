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

import java.util.Properties;
import java.io.IOException;
import java.io.FileInputStream;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Syntax;

import com.sun.org.apache.bcel.internal.generic.Type;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.io.Console;
import java.lang.reflect.Method;

/**
 * Class to provide access to Gnu R from NetLogo.
 * Contains definitions of NetLogo primitives.
 * @author JC Thiele
 * @version 0.3
 */
public class Entry extends org.nlogo.api.DefaultClassManager 
{
	/**
	 * Object of type ExtensionManager to store the R instance.
	 * Needed becausse the dynamic library (jri.dll/jri.so) can't loaded twice. 
	 * Because NetLogo can't cast an Object stored in the ExtensionManager via ex.storeObject/ex.retrieveObject 
   	 * to classes and interfaces other than java natives and NetLogo ones, using NetLogo interfaces is one
     * (dirty) way, to store the HoldRengineX object - containing the initialization of the jri-object with
     * dynamic library loader.
	 */
	private static org.nlogo.api.ExtensionManager rConn = null;
		
	/**
	 * Method executed when extension is loaded and only then. 
	 * Initializes the connection to R or loades the stored R instance from storage.
	 * @param em an instance of ExtensionManager, handled by NetLogo 
	 */
	public void runOnce(org.nlogo.api.ExtensionManager em) throws ExtensionException {
        try
        {    
        	if (em.retrieveObject() == null)
        	{
    			  final String filesep = System.getProperty("file.separator");
    	  		String filepath = System.getenv("JRI_HOME");
    	  		JavaLibraryPath.addFile(filepath+filesep+"JRI.jar");
    	  		JavaLibraryPath.addLibraryPath(new java.io.File(filepath));
        		org.rosuda.JRI.Rengine rToStore = new org.rosuda.JRI.Rengine(new String[] {"--no-save"}, false, null);
        		
        		// try to start R
        		if (!rToStore.waitForR())
        		{
        			throw new ExtensionException("Cannot load R!");
        		}
        		// check versions
        		if (!rToStore.versionCheck())
        		{
        			throw new ExtensionException("R-Extension: Verison mismatch!");
        		}
        		HoldRengineX htest = new HoldRengineX(rToStore);
        		// store object global
        		em.storeObject(htest);	
        	}
        	// get global object
        	Object obj = em.retrieveObject();
        	rConn = (org.nlogo.api.ExtensionManager)obj;
        }
        catch (Exception ex)
        {
        	throw new ExtensionException("Error in runOnce: \n"+ex);
        }
	}

	/**
	 * Method to define the NetLogo primitives. 
	 * @param primManager an instance of PrimitiveManager, handled by NetLogo 
	 */	
	public void load( org.nlogo.api.PrimitiveManager primManager )
    {			
		primManager.addPrimitive( "put", new Put() );	
		primManager.addPrimitive( "putNamedList", new PutNamedVector() );	
		primManager.addPrimitive( "putList", new PutVector() );	
		primManager.addPrimitive( "putDataframe", new PutDataframe() );	
		primManager.addPrimitive( "putAgent", new PutAgent() );	
		primManager.addPrimitive( "putAgentDf", new PutAgentDataFrame() );	
		primManager.addPrimitive( "eval", new Eval() );	
		primManager.addPrimitive( "get", new Get() );			
		primManager.addPrimitive( "clear", new ClearWorkspace() );
    primManager.addPrimitive( "interactiveShell", new interactiveShell() );
		primManager.addPrimitive( "messageWindow", new messageWindow() );
		primManager.addPrimitive( "startDebug", new startDebug() );		
		primManager.addPrimitive( "stopDebug", new stopDebug() );	
		primManager.addPrimitive( "startJRIDebug", new startJRIDebug() );
		primManager.addPrimitive( "stopJRIDebug", new stopJRIDebug() );
    
		// does not work currently
		//primManager.addPrimitive( "loadHistory", new LoadHistory() );
		// does not work currently
		//primManager.addPrimitive( "saveHistory", new SaveHistory() );
    }

	/**
	 * Class to open MessageWindow. (Impelentation of the primitve messageWindow) 
   * @since new in Version 0.3
	 */	
	public static class messageWindow extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("windowConsole", "", "");
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in messageWindow: \n"+ex);
			}
	    }
	}
	
	/**
	 * Class to setup InteractiveShell. (Impelentation of the primitve interactiveShell) 
   * @since new in Version 0.3
	 */
	public static class interactiveShell extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		// (dirty) hack, for checking, if console is running. NetLogo doesn't throw the Exception when trying System.console(), 
	    		// because it doesn't find the method and print this into the not existing console!
	    		// With reflection it is possible to check for the method and throw an exceptin if it isn't available, i.e. no console is available.
	    		Method console_method;
	    		try
	    		{
	    			Class sys_class = Class .forName("java.lang.System");
	    			console_method = sys_class.getDeclaredMethod("console");
				}
	    		catch (Exception ex) 
	    		{
	    			throw new ExtensionException("Error in interactiveShell: \n\n Are you running NetLogo from Console? \n If not, start it again from console! \n\n "+ex);
	    		}
	    		if (console_method != null) 
	    		{
		    		rConn.readExtensionObject("interactiveShell", "", "");	    			
	    		}
	    		else 
	    		{
	    			throw new ExtensionException("Error in interactiveShell: \n No console available!. Start NetLogo from shell/console!");
	    		}
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in interactiveShell: \n"+ex);
			}
	    }
	}

	/**
	 * Class to set Debug of R extension. (Impelentation of the primitve startDebug) 
   * @since new in Version 0.3
	 */
	public static class startDebug extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("startDebug", "", "");
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in startDebug: \n"+ex);
			}
	    }
	}
	
	/**
	 * Class to stop Debug of R extension. (Impelentation of the primitve stopDebug) 
   * @since new in Version 0.3
	 */
	public static class stopDebug extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("stopDebug", "", "");
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in stopDebug: \n"+ex);
			}
	    }
	}
	
	/**
	 * Class to set Debug of JRI. (Impelentation of the primitve startJRIDebug) 
   * @since new in Version 0.3
	 */	
	public static class startJRIDebug extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("startJRIDebug", "", "");
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in startJRIDebug: \n"+ex);
			}
	    }
	}

	/**
	 * Class to stop Debug of JRI. (Impelentation of the primitve stopJRIDebug) 
   * @since new in Version 0.3
	 */		
	public static class stopJRIDebug extends DefaultCommand
	{
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("stopJRIDebug", "", "");
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in stopJRIDebug: \n"+ex);
			}
	    }
	}
	
	/**
	 * Class to create a new Vector from Agent-Variables. (Impelentation of the primitve putAgent) 
	 */	
	public static class PutAgent extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING, Syntax.TYPE_AGENTSET | Syntax.TYPE_AGENT, Syntax.TYPE_STRING | Syntax.TYPE_REPEATABLE});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
	    		AssignAgentsetorAgent(args, "putnamedvector");	    		
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in PutAgent: \n"+ex);
	    	}
	    }
    }

	
	/**
	 * Class to create a new R-DataFrame from Agent-Variables. (Impelentation of the primitve putAgentDf) 
	 */	
	public static class PutAgentDataFrame extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING, Syntax.TYPE_AGENTSET | Syntax.TYPE_AGENT, Syntax.TYPE_STRING | Syntax.TYPE_REPEATABLE});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
	    		AssignAgentsetorAgent(args, "putdataframe");
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in PutAgentDf: \n"+ex);
	    	}
	    }
    }	
	
	
	/**
	 * Class to create a new R-DataFrame from NetLogo-Values. (Impelentation of the primitve putDataframe) 
	 */	
	public static class PutDataframe extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING, Syntax.TYPE_WILDCARD | Syntax.TYPE_REPEATABLE});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
		    	String rname = args[0].getString();
		    		    	
		    	Object[] input = new Object[args.length-1];
		    
		    	LinkedHashMap<String, Object> hm = new LinkedHashMap<String, Object>();
		    	hm.put(rname, null);
		    	hm.put("putdataframe", null);
		    	
		    	for (int i=0; i<args.length-2; i += 2)
		    	{
		    		String varname = args[i+1].getString();
		    		input[i] = args[i+2].get();	
	    			hm.put(varname, input[i]);
		    	}
		    	rConn.storeObject(hm);
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in PutDataFrame: \n"+ex);
	    	}
	    }
    }
	
	/**
	 * Class to create a new R-Vector from NetLogo-Values. (Impelentation of the primitve putList) 
	 */	
	public static class PutVector extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING, Syntax.TYPE_WILDCARD | Syntax.TYPE_REPEATABLE});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
		    	String rname = args[0].getString();
		    	
		    	Object[] input = new Object[args.length-1];
		    
		    	LinkedHashMap<String, Object> hm = new LinkedHashMap<String, Object>();
		    	hm.put(rname, null);
		    	
			    hm.put("putnamedvector", null);
			    for (int i=0; i<args.length-1; i++)
			    {
			    	String varname = ((Integer)i).toString();
			    	input[i] = args[i+1].get();	
		    		hm.put(varname, input[i]);
			    }

		    	rConn.storeObject(hm);
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in PutVector: \n"+ex);
	    	}
	    }
    }
	
	
	/**
	 * Class to create a new named R-Vector from NetLogo-Values. (Impelentation of the primitve putNamedList) 
	 */	
	public static class PutNamedVector extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING, Syntax.TYPE_WILDCARD | Syntax.TYPE_REPEATABLE});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
		    	String rname = args[0].getString();
		    		    	
		    	Object[] input = new Object[args.length-1];
		    
		    	LinkedHashMap<String, Object> hm = new LinkedHashMap<String, Object>();
		    	hm.put(rname, null);
		    	
		    	if (args[1].get() instanceof String)
		    	{
			    	hm.put("putnamedvector", null);
			    	for (int i=0; i<args.length-2; i += 2)
			    	{
			    		String varname = args[i+1].getString();
			    		input[i] = args[i+2].get();	
		    			hm.put(varname, input[i]);
			    	}
		    	}
		    	rConn.storeObject(hm);
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in PutNamedVector: \n"+ex);
	    	}
	    }
    }
	
	
	
	/**
	 * Class to create a new R-Variable/Array from NetLogo-Values. (Impelentation of the primitve put) 
	 */
	public static class Put extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING, Syntax.TYPE_WILDCARD});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
		    	String rname = args[0].getString();		    	
		    	Object input = args[1].get();
	
		    	LinkedHashMap<String, Object> hm = new LinkedHashMap<String, Object>();
		    	hm.put("put",null);
		    	hm.put(rname, input);
		    	rConn.storeObject(hm);
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in perform. \n"+ex);
	    	}
	    
	    }
    }
	
	
	/**
	 * Class to evaluate submitted String in R without results. (Impelentation of the primitve eval) 
	 */	
	public static class Eval extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		String command = args[0].getString();
		    	rConn.readFromString(command);
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in Eval: \n"+ex);
		}
	    }
    }

	/**
	 * Class to evaluate submitted String in R, and send back the results to NetLogo. (Impelentation of the primitve get) 
	 */	
    public static class Get extends DefaultReporter
	{
	    public Syntax getSyntax() {
	        return Syntax.reporterSyntax(new int[] {Syntax.TYPE_STRING}, Syntax.TYPE_WILDCARD);
	    }  

	    private Object returnObject(Object result)
	    {
	    	if (result instanceof java.lang.String)
	    	{
	    		return (String)result;
	    	}

	    	if (result instanceof java.lang.Double)
	    	{
	    		return (Double)result;
	    	}

	    	if (result instanceof java.lang.Boolean)
	    	{
	    		return (Boolean)result;
	    	}
	    	
	    	if (result instanceof double[])
	    	{
	    		double[] dbarray = (double[])result;
	    		if (dbarray.length < 2)
	    		{
	    			return dbarray[0];
	    		}
	    		org.nlogo.api.LogoList llist = new org.nlogo.api.LogoList();
	    		for (int i=0; i<dbarray.length; i++)
	    		{
	    			llist.add((Double)dbarray[i]);
	    		}
	    		return llist;
	    	}
	    	
	    	if (result instanceof int[])
	    	{
	    		int[] intarray = (int[])result;
	    		if (intarray.length < 2)
	    		{
	    			return (Double)((Integer)intarray[0]).doubleValue();
	    		}
	    		org.nlogo.api.LogoList llist = new org.nlogo.api.LogoList();		    		
	    		for (int i=0; i<intarray.length; i++)
	    		{
	    			llist.add( new Double(((Integer)intarray[i]).doubleValue()) );
	    		}
	    		return llist;
	    	}

	    	if (result instanceof String[])
	    	{
	    		String[] strarray = (String[])result;
	    		if (strarray.length < 2)
	    		{
	    			return (String)strarray[0];
	    		}
	    		org.nlogo.api.LogoList llist = new org.nlogo.api.LogoList();		    		
	    		for (int i=0; i<strarray.length; i++)
	    		{
	    			llist.add( (String)strarray[i] );
	    		}
	    		return llist;
	    	}

	    	if (result instanceof boolean[])
	    	{
	    		Boolean[] boolarray = (Boolean[])result;
	    		if (boolarray.length < 2)
	    		{
	    			return ((Boolean)boolarray[0]).booleanValue();
	    		}
	    		org.nlogo.api.LogoList llist = new org.nlogo.api.LogoList();		    		
	    		for (int i=0; i<boolarray.length; i++)
	    		{
	    			llist.add( ((Boolean)boolarray[i]).booleanValue() );
	    		}
	    		return llist;
	    	}
	    	return null;
	    }
	    
	    
	    public Object report(Argument args[], Context context) throws ExtensionException, LogoException
	    {
    		try
    		{
    			String command = args[0].getString();
	    		
	    		// send command to HoldRengineX, evaluate it and get back the result as Object
	    		Object result = rConn.readFromString(command);

		    	Object singleobj = returnObject(result);
		    	if (singleobj != null)
		    	{
		    		return singleobj;
		    	}
		    	
		    	if (result instanceof org.rosuda.JRI.RVector)
		    	{
		    		org.nlogo.api.LogoList list1 = new org.nlogo.api.LogoList();
		    		
		    		Iterator it = ((org.rosuda.JRI.RVector)result).listIterator();
		    		while (it.hasNext())
		    		{
		    			Object obj = it.next();
		    			Object objnow = returnObject(((org.rosuda.JRI.REXP)obj).getContent());
		    			list1.add(objnow);	
		    		}
		    		return list1;
		    	}

		    	if (result instanceof org.rosuda.JRI.RBool)
		    	{
		    		int bool = 0;
		    		org.rosuda.JRI.RBool boolhere = (org.rosuda.JRI.RBool)result;
		    		if (boolhere.isTRUE())
		    		{
		    			bool = 1;
		    		}
		    		if (boolhere.isNA())
		    		{
		    			bool = -1;
		    		}
		    		return bool;	
		    	}

		    	if (result instanceof org.rosuda.JRI.RFactor)
		    	{
		    		org.nlogo.api.LogoList list1 = new org.nlogo.api.LogoList();
		    		
		    		org.rosuda.JRI.RFactor factorhere = (org.rosuda.JRI.RFactor)result;
		    		
		    		for (int i=0; i<factorhere.size(); i++)
		    		{
		    			String fac = factorhere.at(i);
		    			list1.add(fac);	
		    		}		    		
		    		return list1;	
		    	}
    		}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in perform. \n"+ex);
	    	}
	    	
    		return null;
	    } 
    }
	 
	/**
	 * Class to clear R workspace. (Impelentation of the primitve clear) 
	 */	  
	public static class ClearWorkspace extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("", "", "rm(list=ls())");
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in ClearWorkspace: \n"+ex);
		}
	    }
    }

	/**
	 * Class to load R history from file. (Impelentation of the primitve loadHistory) 
	 * Currently not supported!
	 */	    
	public static class LoadHistory extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject("",args[0].getString(),"");
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in LoadHistory: \n"+ex);
		}
	    }
    }

	/**
	 * Class to save R history into file. (Impelentation of the primitve saveHistory) 
	 * Currently not supported!
	 */	 	
	public static class SaveHistory extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		rConn.readExtensionObject(args[0].getString(),"","");
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in SaveHistory: \n"+ex);
		}

	    }
    }
	
	/**
	 * Method executed when extension is unloaded. 
	 * Clears the R workspace, destroy MessageWindow (if created) and reset Debugging.
	 */
    public void unload() throws ExtensionException
    {
    	// clear workspace
    	try
      {
    		// clear workspace
    		rConn.readExtensionObject("", "", "rm(list=ls())");
    		// reset debugging and destroy MessageWindow
    		rConn.retrieveObject();
    	}
    	catch (Exception ex)
    	{
    		throw new ExtensionException("Error in unload: \n"+ex);
    	}
    }
	
	/**
	 * Method to create an new R-Dataset from Agent or Agentset
	 * @param args[] submitted agents/data
	 * @param outtype String describing the type of inputs, supported: "putnamedvector" and "putdataframe"
	 * @exception ExtensionException
	 */
	private static void AssignAgentsetorAgent(Argument args[], String outtype) throws ExtensionException
	{
		try
		{
			String rname = args[0].getString();			
			Object ag = args[1].get();
			String[] varnames = new String[args.length-2];
			/* get variable names */
			for (int i=0; i<args.length-2; i++)
	    	{
	    		varnames[i] = args[i+2].getString();	
	    	}	    		
			LinkedHashMap<String, Object> hm = new LinkedHashMap<String, Object>();
	    	hm.put(rname, null);
	    	hm.put(outtype, null);
			/* if input is an agentset */
			if (ag instanceof org.nlogo.agent.AgentSet)
			{
				org.nlogo.agent.AgentSet agentset = (org.nlogo.agent.AgentSet)ag;
				
		    	for (int j = 0; j<varnames.length; j++)
				{
		    		org.nlogo.api.LogoList varvalue = new org.nlogo.api.LogoList();
		    		org.nlogo.agent.AgentSet.Iterator it = agentset.iterator();
	    			org.nlogo.agent.Agent[] ags = new org.nlogo.agent.Agent[agentset.count()];
	    			int i = 0;
		    		while (it.hasNext())
		    		{
		    			ags[i] = (org.nlogo.agent.Agent)it.next();
	
			    		int varindex = ags[i].world().indexOfVariable(ags[i], varnames[j].toUpperCase());	
	    				
	    				if (ags[i] instanceof org.nlogo.agent.Turtle)
			    		{
	    					varvalue.add(ags[i].getTurtleVariable(varindex));
	    				}
			    		if (ags[i] instanceof org.nlogo.agent.Link)
	    				{
			    			varvalue.add(ags[i].getLinkVariable(varindex));
	    				}
		    			if (ags[i] instanceof org.nlogo.agent.Patch)
		    			{
		    				varvalue.add(ags[i].getPatchVariable(varindex));
		    			}
		    			i++;
	    			}
		    		hm.put(varnames[j], varvalue);
				}
			}	
			/* if input isn't an agentset but agent */
			if (ag instanceof org.nlogo.agent.Agent)
			{
				org.nlogo.agent.Agent agent = (org.nlogo.agent.Agent)ag; 
		    	for (int j = 0; j<varnames.length; j++)
				{
		    		int varindex = agent.world().indexOfVariable(agent, varnames[j].toUpperCase());	
		    		org.nlogo.api.LogoList varvalue = new org.nlogo.api.LogoList();	
					if (agent instanceof org.nlogo.agent.Turtle)
		    		{
						varvalue.add(agent.getTurtleVariable(varindex));
					}
		    		if (agent instanceof org.nlogo.agent.Link)
					{
		    			varvalue.add(agent.getLinkVariable(varindex));
					}
	    			if (agent instanceof org.nlogo.agent.Patch)
	    			{
	    				varvalue.add(agent.getPatchVariable(varindex));
	    			}
		    		hm.put(varnames[j], varvalue);
				}
			}  							
			rConn.storeObject(hm);	
		}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in PutAgentDf: \n"+ex);
		}
	}
}