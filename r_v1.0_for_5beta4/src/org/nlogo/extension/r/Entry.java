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





import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Syntax;
import java.lang.reflect.*;
import org.rosuda.REngine.*;

/**
 * Class to provide access to Gnu R from NetLogo.
 * Contains definitions of NetLogo primitives.
 * @author JC Thiele
 * @version 1.0beta
 */
public class Entry extends org.nlogo.api.DefaultClassManager 
{
	/**
	 * Object of type ExtensionManager to store the R console instance.
	 * Needed because the dynamic library (jri.dll/jri.so) can't be loaded twice. 
	 * Because NetLogo can't cast an Object stored in the ExtensionManager via ex.storeObject/ex.retrieveObject 
   	 * to classes and interfaces other than java natives and NetLogo ones, using NetLogo interfaces is one
     * (dirty) way, to store the ShellWindow object.
	 */
	private static org.nlogo.api.ExtensionManager shellwin;
	/**
	 * Object containing the connection to R
	 */
	public static HoldRengineX rConn = null;
	/**
	 * Object to synchronize console input/execution
	 */
	/*private*/public static ConsoleSync rSync = new ConsoleSync();
	
	/**
	 * Method executed when extension is loaded and only then. 
	 * Initializes the connection to R and ShellWindow or loads the stored ShellWindow 
	 * instance from storage.
	 * @param em an instance of ExtensionManager, handled by NetLogo 
	 */
	public void runOnce(org.nlogo.api.ExtensionManager em) throws ExtensionException {
        try
        {   
        	try
        	{
	        	// dynamically load of the needed JARs from the JRI package
				final String filesep = System.getProperty("file.separator");
		  		String filepath = System.getenv("JRI_HOME");
		  		JavaLibraryPath.addFile(filepath+filesep+"JRI.jar");
		  		JavaLibraryPath.addFile(filepath+filesep+"REngine.jar");
		  		JavaLibraryPath.addFile(filepath+filesep+"JRIEngine.jar");
		  		
		  		boolean _64exists = false;
		  		boolean _32exists = false;
		  		java.io.File file = new java.io.File(filepath+"/x64/");
		  		_64exists = file.exists();
		  		
		  		file = new java.io.File(filepath+"/i386/");
		  		_32exists = file.exists();	
		  		
		  		if (System.getProperty("sun.arch.data.model", "?").contains("64") && _64exists)
		  		{
			  		JavaLibraryPath.addLibraryPath(new java.io.File(filepath+"/x64"));		  			
		  		}
		  		else if (System.getProperty("sun.arch.data.model", "?").contains("32") && _32exists)
		  		{
			  		JavaLibraryPath.addLibraryPath(new java.io.File(filepath+"/i386"));		  			
		  		}
		  		else
		  		{
		  			JavaLibraryPath.addLibraryPath(new java.io.File(filepath));
		  		}
		  		
		  		//JavaLibraryPath.addLibraryPath(new java.io.File(filepath));
		  		
		  		/*
		  		// check for 64-bit 
		  		String bits = System.getProperty("sun.arch.data.model", "?");
		  		if (bits.equals("64")) 
		  		{
		  			System.out.println("found 64bit JVM...");
			  		java.io.File file = new java.io.File(filepath+"/x64/");
			  		if (file.exists())
			  		{
			  			System.out.println("found 64bit R/JRI...");
			  			JavaLibraryPath.addLibraryPath(new java.io.File(filepath+"/x64/"));	  			  			
			  		}
			  		else
			  		{
			  			JOptionPane.showMessageDialog(null, "Found 64-bit Java, but just 32-bit R/JRI. I will try to load it.", "R-Extension",
			  					JOptionPane.INFORMATION_MESSAGE);
			  			JavaLibraryPath.addLibraryPath(new java.io.File(filepath));	  		
			  		}	  			
		  		}
		  		else
		  		{		  			
		  			System.out.println("found 32bit JVM...");
			  		java.io.File file = new java.io.File(filepath+"/i386/");
			  		if (file.exists())
			  		{
			  			System.out.println("found explicit i386 R/JRI...");
			  			JavaLibraryPath.addLibraryPath(new java.io.File(filepath+"/i386/"));	  			  			
			  		}
			  		else
			  		{
			  			System.out.println("found standard 32bit R/JRI...");
			  			JavaLibraryPath.addLibraryPath(new java.io.File(filepath));	  		
			  		}		
		  		}
		  		 */

        	}
	  		catch (UnsatisfiedLinkError localUnsatisfiedLinkError) {
	  			throw new ExtensionException("Cannot load rJava/JRI. Please check your rJava installation and JRI_HOME environment variable.\n"+localUnsatisfiedLinkError);
	  		}
	  		
	  		org.rosuda.REngine.REngine lastEngine = org.rosuda.REngine.REngine.getLastEngine();
        	// if no further REnginer was initialized
        	if (lastEngine == null)
        	{
    			Class iashell_class = Class.forName("org.nlogo.extension.r.ShellWindow");
    			Class partypes1[] = new Class[1];
    	        partypes1[0] = ConsoleSync.class;
    	        Constructor ct_is = iashell_class.getConstructor(partypes1);  
    	        Object arglist1[] = new Object[1];
    	        arglist1[0] = rSync;
    	        Object intershellObj = ct_is.newInstance(arglist1);				
    	        org.nlogo.api.ExtensionManager tc = (org.nlogo.api.ExtensionManager)intershellObj;
    			shellwin = tc;
    			Class rengineClass = Class.forName("org.rosuda.REngine.JRI.JRIEngine");    			
    			Class callbacks_class = Class.forName("org.rosuda.REngine.REngineCallbacks");
				Class partypes[] = new Class[3];
		        partypes[0] = String[].class;
		        partypes[1] = callbacks_class; 
		        partypes[2] = boolean.class;
		        Object arglist[] = new Object[3];
	            arglist[0] = new String[] {"--no-save"};
	            arglist[1] = tc;
	            arglist[2] = true;	            
    			Method thisMethod = rengineClass.getDeclaredMethod("createEngine", partypes);	            
	            REngine rToStore = (REngine)thisMethod.invoke(rengineClass, arglist);
    	  		rConn = new HoldRengineX(rToStore);
    	  		em.storeObject(tc);
        	}
        	// otherwise, reload the last REngine object and retrieve the stored ShellWindow object
        	else
        	{
    	  		// this will also create a new Environment
    	  		rConn = new HoldRengineX(lastEngine);    	  		
    	  		Object temp = em.retrieveObject();
    	  		shellwin = (org.nlogo.api.ExtensionManager)temp; 
        	}
        }
        catch (InvocationTargetException ex)
        {
        	throw new ExtensionException("Error in R-Extension: InvocationTargetException: Error in runOnce: \n"+ex+"\ncause: "+ex.getCause());
        }
        catch (Exception ex)
        {
        	throw new ExtensionException("Error in R-Extension: Error in runOnce: \n"+ex);
        }
	}
	

	/**
	 * Method to define the NetLogo primitives. 
	 * @param primManager an instance of PrimitiveManager, handled by NetLogo 
	 */	
	public void load( org.nlogo.api.PrimitiveManager primManager )
    {			
		primManager.addPrimitive( "put", new Put() );	
		primManager.addPrimitive( "putNamedList", new PutNamedList() );	
		primManager.addPrimitive( "putList", new PutList() );	
		primManager.addPrimitive( "putDataframe", new PutDataframe() );	
		primManager.addPrimitive( "putAgent", new PutAgent() );	
		primManager.addPrimitive( "putAgentDf", new PutAgentDataFrame() );	
		primManager.addPrimitive( "eval", new Eval() );	
		primManager.addPrimitive( "__evalDirect", new EvalDirect() );	
		primManager.addPrimitive( "get", new Get() );			
		primManager.addPrimitive( "clear", new ClearWorkspace() );
		primManager.addPrimitive( "clearLocal", new ClearLocalWorkspace() );
		primManager.addPrimitive( "interactiveShell", new interactiveShell() );
		primManager.addPrimitive( "setPlotDevice", new SetPlotDevice() );
    }

	
	/**
	 * Class to setup the JavaGD plot device. (Implementation of the primitive setPlotDevice) 
	 * @since new in Version 1.0beta
	 */
	public static class SetPlotDevice extends DefaultCommand
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
	        	shellwin.storeObject(null);
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in R-Extension: Error in setPlotDevice: \n"+ex);
			}
	    }
	}
	
	/**
	 * Class to setup InteractiveShell. (Implementation of the primitive interactiveShell) 
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
	    		if (!Entry.shellwin.anyExtensionsLoaded()){
	    			Entry.shellwin.finishFullCompilation();
	    		}
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in R-Extension: Error in interactiveShell: \n"+ex);
			}
	    }
	}

	
	/**
	 * Class to create a new Vector from Agent-Variables. (Implementation of the primitive putAgent) 
	 */	
	public static class PutAgent extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.AgentsetType() | Syntax.AgentType(), Syntax.StringType() | Syntax.RepeatableType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
	    		rConn.AssignAgentsetorAgent(args, false);	    		
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in PutAgent: \n"+ex);
	    	}
	    }
    }

	
	/**
	 * Class to create a new R-DataFrame from Agent-Variables. (Implementation of the primitive putAgentDf) 
	 */	
	public static class PutAgentDataFrame extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.AgentsetType() | Syntax.AgentType(), Syntax.StringType() | Syntax.RepeatableType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
	    		rConn.AssignAgentsetorAgent(args, true);
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in PutAgentDf: \n"+ex);
	    	}
	    }
    }	
	
	
	/**
	 * Class to create a new R-DataFrame from NetLogo-Values. (Implementation of the primitive putDataframe) 
	 */	
	public static class PutDataframe extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.WildcardType() | Syntax.RepeatableType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
		        java.util.Vector<String> names = new java.util.Vector<String>();
		        org.rosuda.REngine.RList rlist = new RList(); 		    	
		    	for (int i=0; i<args.length-2; i += 2)
		    	{
					names.add(args[i+1].getString());
					rlist.add(rConn.resolveNLObject(args[i+2].get()));
		    	}
				rlist.names = names;
				rConn.rConnection.assign(args[0].getString(), org.rosuda.REngine.REXP.createDataFrame(rlist), rConn.WorkingEnvironment);;
				//System.gc();
	    		//System.gc();	    		
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in PutDataFrame: \n"+ex);
	    	}
	    }
    }
	
	/**
	 * Class to create a new R-List from NetLogo-Values. (Implementation of the primitive putList) 
	 */	
	public static class PutList extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.WildcardType() | Syntax.RepeatableType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{		    
		        java.util.Vector<String> names = new java.util.Vector<String>();
		        org.rosuda.REngine.RList rlist = new RList(); 
			    for (int i=0; i<args.length-1; i++)
			    {
			    	names.add(((Integer)i).toString());
			    	rlist.add(rConn.resolveNLObject(args[i+1].get()));
			    }
				rlist.names = names;
				rConn.rConnection.assign(args[0].getString(), new REXPGenericVector(rlist), rConn.WorkingEnvironment);
				//System.gc();
	    		//System.gc();
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in PutVector: \n"+ex);
	    	}
	    }
    }
	
	
	/**
	 * Class to create a new named R-List from NetLogo-Values. (Implementation of the primitive putNamedList) 
	 */	
	public static class PutNamedList extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.WildcardType() | Syntax.RepeatableType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
		        java.util.Vector<String> names = new java.util.Vector<String>();
		        org.rosuda.REngine.RList rlist = new RList(); 
		    	for (int i=0; i<args.length-2; i += 2)
		    	{
					names.add(args[i+1].getString());
					rlist.add(rConn.resolveNLObject(args[i+2].get()));
		    	}
				rlist.names = names;
				rConn.rConnection.assign(args[0].getString(), new REXPGenericVector(rlist), rConn.WorkingEnvironment);
		    	//System.gc();
	    		//System.gc();
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in PutNamedList: \n"+ex);
	    	}
	    }
    }
	
	
	
	/**
	 * Class to create a new R-Variable/Array from NetLogo-Values. (Implementation of the primitive put) 
	 */
	public static class Put extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.WildcardType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
	    	try
	    	{
	    		rConn.rConnection.assign(args[0].getString(), rConn.resolveNLObject(args[1].get()), rConn.WorkingEnvironment);
	    		//System.gc();
	    		//System.gc();
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in Put. \n"+ex);
	    	}
	    
	    }
    }
	
	
	/**
	 * Class to evaluate submitted String in R without results. (Implementation of the primitive eval) 
	 */	
	public static class Eval extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		REXP returnVal =  rConn.execute(rConn.rConnection, args[0].getString(), rConn.WorkingEnvironment, true);
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in R-Extension: Error in Eval: \n"+ex);
		}
	    }
    }

	
	/**
	 * Class to evaluate submitted String directly in R Console without results. 
	 * (Implementation of the primitive evalDirect)
	 * Some packages (e.g. ggplot2) doesn't work with eval 
	 */	
	public static class EvalDirect extends DefaultCommand
    {	
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.StringType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    { 
	    	try
	    	{
	    		String[] cmdArray = args[0].getString().split("\n");		
	    		String c = null;
	    		for (int i = 0; i < cmdArray.length; i++) {
	    			c = cmdArray[i];
	    			Entry.rSync.triggerNotification(c.trim());
	    		}
	    		//REXP returnVal =  rConn.execute(rConn.rConnection, args[0].getString(), rConn.WorkingEnvironment, true);
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in R-Extension: Error in Eval: \n"+ex);
		}
	    }
    }

	
	/**
	 * Class to evaluate submitted String in R, and send back the results to NetLogo. (Implementation of the primitive get) 
	 */	
    public static class Get extends DefaultReporter
	{
	    public Syntax getSyntax() {
	        return Syntax.reporterSyntax(new int[] {Syntax.StringType()}, Syntax.WildcardType());
	    }  
	    public Object report(Argument args[], Context context) throws ExtensionException, LogoException
	    {
	    	try
	    	{
	    		REXP returnVal =  rConn.execute(rConn.rConnection, args[0].getString(), rConn.WorkingEnvironment, true);
	    		return rConn.returnObject(returnVal);
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in Get. \n"+ex);
	    	}	    	
	    } 
    }
    
	 
	/**
	 * Class to clear R workspace. (Implementation of the primitive clear) 
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
	    		REXP returnVal =  rConn.execute(rConn.rConnection, "rm(list=ls())", rConn.WorkingEnvironment, true);
	    		rConn.rConnection.parseAndEval("rm(list=ls())");
	    		rConn.rConnection.parseAndEval("gc()");
	    		rConn.sendEnvironmentToGlobal();
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in R-Extension: Error in ClearWorkspace: \n"+ex);
		}
	    }
    }
	
	/**
	 * Class to clear local (nl.env) R workspace. (Implementation of the primitive clear) 
	 */	  
	public static class ClearLocalWorkspace extends DefaultCommand
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
	    		REXP returnVal =  rConn.execute(rConn.rConnection, "rm(list=ls())", rConn.WorkingEnvironment, true);
	    		REXP returnVal2 =  rConn.execute(rConn.rConnection, "gc()", rConn.WorkingEnvironment, true);
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in R-Extension: Error in ClearLocalWorkspace: \n"+ex);
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
    		REXP returnVal =  rConn.execute(rConn.rConnection, "rm(list=ls())", rConn.WorkingEnvironment, true);
    		rConn.rConnection.parseAndEval("rm(list=ls())");
    		rConn.rConnection.parseAndEval("gc()");
      	}
    	catch (Exception ex)
    	{
    		throw new ExtensionException("Error in R-Extension: Error in unload: \n"+ex);
    	}
    	try
    	{
    		// check if ShellWindow is open - if so, close it...
    		if (Entry.shellwin.anyExtensionsLoaded())
    		{
    			Entry.shellwin.reset();
    		}
      	}
    	catch (Exception ex)
    	{
    		throw new ExtensionException("Error in R-Extension: Error in making interactiveShell invisible: \n"+ex);
    	}
    }
	
}