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

import javax.swing.JOptionPane;

import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

/**
 * Class to provide access to Gnu R from NetLogo.
 * Contains definitions of NetLogo primitives.
 * @author JC Thiele
 * @version 1.0beta
 */
public class Entry extends org.nlogo.api.DefaultClassManager 
{
	/**
	 * Object containing the connection to R
	 */
	public static HoldRengineX rConn = null;

	/**
	 * Method to define the NetLogo primitives. 
	 * @param primManager an instance of PrimitiveManager, handled by NetLogo 
	 */	
	public void load( org.nlogo.api.PrimitiveManager primManager )
    {			
		primManager.addPrimitive( "init", new Init() );	
		primManager.addPrimitive( "close", new Close() );	
		primManager.addPrimitive( "setSendBufferSize", new SetSendBufferSize() );	
		primManager.addPrimitive( "isConnected", new IsConnected() );	
		
		primManager.addPrimitive( "put", new Put() );	
		primManager.addPrimitive( "putNamedList", new PutNamedList() );	
		primManager.addPrimitive( "putList", new PutList() );	
		primManager.addPrimitive( "putDataframe", new PutDataframe() );	
		primManager.addPrimitive( "putAgent", new PutAgent() );	
		primManager.addPrimitive( "putAgentDf", new PutAgentDataFrame() );	
		primManager.addPrimitive( "eval", new Eval() );		
		primManager.addPrimitive( "get", new Get() );			
		primManager.addPrimitive( "clear", new ClearWorkspace() );
    }

	/**
	 * Class to initialize the connection to R (via Rserve). (Implementation of the primitive init) 
	 */	
	public static class Init extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.NumberType(), Syntax.StringType() | Syntax.RepeatableType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
			try
			{
				if (rConn != null)
				{
					throw new ExtensionException("Error in R-Extension: Error in init: There was already an connection to R established.");
				}
				else
				{
					if (args.length > 4)
					{
						throw new ExtensionException("Error in R-Extension: Error in init: Too many arguments.");
					}
					
					int port = args[0].getIntValue();
					String host = args[1].getString();	
					RConnection rservecon = new RConnection(host,port);
					if (rservecon.needLogin())
					{
						if (args.length == 4)
						{
							String name = args[2].getString();
							String passwd = args[3].getString();
							rservecon.login(name, passwd);						
						}
						else {
							throw new ExtensionException("");
						}
					}
					rConn = new HoldRengineX(rservecon);
				}
			}
			catch (RserveException rex)
			{
				throw new ExtensionException("Error in R-Extension: Error in init: \n"+rex);	
			}
			catch (Exception ex)
			{
				throw new ExtensionException("Error in R-Extension: Error in init: \n"+ex);				
			}
	    }
    }

	/**
	 * Class to close the connection to R (via Rserve). (Implementation of the primitive close) 
	 */	
	public static class Close extends DefaultCommand
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
				if (rConn.rConnection.isConnected())
				{
					rConn.rConnection.close(); //detach(); //.close();
					rConn = null;
				}
				else
				{
					throw new ExtensionException("Error in R-Extension: Can't execute close because there is no active connection to R.");
				}
			}
			catch (Exception ex)
			{
				throw new ExtensionException("Error in R-Extension: Error in close: \n"+ex);				
			}
	    }
    }

	
	/**
	 * Class to set the buffer size for submissions to R (via Rserve). (Implementation of the primitive setSendBufferSize) 
	 */	
	public static class SetSendBufferSize extends DefaultCommand
    {
	    public Syntax getSyntax() {
	        return Syntax.commandSyntax(new int[] {Syntax.NumberType()});
	    }  
		public String getAgentClassString()
		{
			return "OTPL" ;
		}    	
	    public void perform(Argument args[], Context context) throws ExtensionException, LogoException
	    {   	    	
			try
			{
				rConn.rConnection.setSendBufferSize(args[0].getIntValue());
			}
			catch (Exception ex)
			{
				throw new ExtensionException("Error in R-Extension: Error in setSendBufferSize: \n"+ex);				
			}
	    }
    }

	
	
	/**
	 * Class to check if connection to R (via Rserve) is established. (Implementation of the primitive isConnected) 
	 */	
    public static class IsConnected extends DefaultReporter
	{
	    public Syntax getSyntax() {
	        return Syntax.reporterSyntax(new int[] {}, Syntax.BooleanType());
	    }  
	    public Object report(Argument args[], Context context) throws ExtensionException, LogoException
	    {
	    	try
	    	{
	    		if (rConn == null)
	    		{
	    			return false;
	    		}
	    		return rConn.rConnection.isConnected();
	    	}
	    	catch (Exception ex)
	    	{
	    		throw new ExtensionException("Error in R-Extension: Error in isConnected. \n"+ex);
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
				rConn.rConnection.assign(args[0].getString(), org.rosuda.REngine.REXP.createDataFrame(rlist)); //, rConn.WorkingEnvironment);;
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
				rConn.rConnection.assign(args[0].getString(), new REXPGenericVector(rlist)); //, rConn.WorkingEnvironment);
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
				rConn.rConnection.assign(args[0].getString(), new REXPGenericVector(rlist)); //, rConn.WorkingEnvironment);
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
	    		rConn.rConnection.assign(args[0].getString(), rConn.resolveNLObject(args[1].get()));//, rConn.WorkingEnvironment);
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
	    		REXP returnVal =  rConn.execute(rConn.rConnection, args[0].getString());//, rConn.WorkingEnvironment, true);
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
	    		REXP returnVal =  rConn.execute(rConn.rConnection, args[0].getString());//, rConn.WorkingEnvironment, true);
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
	    		REXP returnVal =  rConn.execute(rConn.rConnection, "rm(list=ls())"); //, rConn.WorkingEnvironment, true);
	    		rConn.rConnection.parseAndEval("rm(list=ls())");
	    		rConn.rConnection.parseAndEval("gc()");
	    	}
		catch (Exception ex)
		{
			throw new ExtensionException("Error in R-Extension: Error in ClearWorkspace: \n"+ex);
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
    		REXP returnVal =  rConn.execute(rConn.rConnection, "rm(list=ls())");//, rConn.WorkingEnvironment, true);
    		rConn.rConnection.parseAndEval("rm(list=ls())");
    		rConn.rConnection.parseAndEval("gc()");
      	}
    	catch (Exception ex)
    	{
    		throw new ExtensionException("Error in R-Extension: Error in unload: \n"+ex);
    	}
    }
	
}