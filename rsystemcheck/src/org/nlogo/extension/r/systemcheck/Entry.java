package org.nlogo.extension.r.systemcheck;


import java.io.InputStreamReader;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Syntax;

import java.io.*;

public class Entry extends org.nlogo.api.DefaultClassManager {

	
	/**
	 * Method to define the NetLogo primitives. 
	 * @param primManager an instance of PrimitiveManager, handled by NetLogo 
	 */	
	public void load( org.nlogo.api.PrimitiveManager primManager )
    {			
		primManager.addPrimitive( "BasicCheck", new BasicCheck() );	
		primManager.addPrimitive( "JavaCheck", new JavaCheck() );	
		primManager.addPrimitive( "RCheck", new RCheck() );	
		primManager.addPrimitive( "rJavaCheck1", new RJavaCheck1() );	
		primManager.addPrimitive( "rJavaCheck2", new RJavaCheck2() );	
		primManager.addPrimitive( "jriCheck", new JRICheck() );	
		primManager.addPrimitive( "JavaGDCheck", new JavaGDCheck() );	
   }
	

	
	public static class BasicCheck extends DefaultCommand
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
	        	// check up functions
        		String message = new String();
        
        		message += "Operation system name: "+System.getProperty("os.name")+".\n";
        		message += "Operation system version: "+System.getProperty("os.version")+".\n";
        		message += "Operation system runs as: "+System.getProperty("os.arch")+" (Note: i386 & x86 = 32-bit).\n";
        		message += "JVM name: "+System.getProperty("java.vm.name")+".\n";
        		message += "VM vendor: "+System.getProperty("java.vm.vendor")+".\n";
        		message += "VM version: "+System.getProperty("java.vm.version")+".\n";
        		message += "VM info: "+System.getProperty("java.vm.info")+".\n";
        		message += "runtime name: "+System.getProperty("java.runtime.name")+".\n";
        		message += "runtime version: "+System.getProperty("java.runtime.version")+".\n\n";
        		message += "Java is running with: "+System.getProperty("sun.arch.data.model", "?")+" bits.\n\n";
        		
        		message += "Your JRI_HOME variable is: "+System.getenv("JRI_HOME")+"\n";
            
            
            
            if (System.getProperty("os.name").startsWith("Window")) 
            {
	      java.io.File file = new java.io.File(System.getenv("JRI_HOME")+"/jri.dll");
	      message += "Found jri.dll: "+file.exists()+"\n";
              file = new java.io.File(System.getenv("JRI_HOME")+"/i386/jri.dll");
              message += "Found explicit 32-bit jri.dll: "+file.exists()+"\n";
              file = new java.io.File(System.getenv("JRI_HOME")+"/x64/jri.dll");
              message += "Found explicit 64-bit jri.dll: "+file.exists()+"\n";
            }
            else
            {
	      java.io.File file = new java.io.File(System.getenv("JRI_HOME")+"/libjri.so");
	      message += "Found libjri.so: "+file.exists()+"\n";
              file = new java.io.File(System.getenv("JRI_HOME")+"/i386/libjri.so");
              message += "Found explicit 32-bit libjri.so: "+file.exists()+"\n";
              file = new java.io.File(System.getenv("JRI_HOME")+"/x64/libjri.so");
              message += "Found explicit 64-bit libjri.so: "+file.exists()+"\n";
            }
            
        	message += "Your R_HOME variable is: "+System.getenv("R_HOME")+"\n";
        	if (System.getProperty("os.name").startsWith("Window"))
    		{
	    		message += "Your PATH variable is:";
	    		String filepath = System.getenv("PATH");
	    		String substr = filepath;
	    		String rhome = System.getenv("R_HOME");
	    		String rpath = new String();
	    		while (substr.contains(";") && substr.length()>3)
	    		{
		    		int i = substr.indexOf(";");
		    		substr = substr.substring(i+1);	  
		    		int end = substr.indexOf(";");
		    		String path = new String();
		    		if (end == -1)
		    		{
		    			path = substr;
		    		}
		    		else
		    		{
		    			path = substr.substring(0, end);
		    		}
		    		if (path.contains(rhome))
		    		{
		    			rpath = path;
		    		}
		    		message += path+"\n"; 
	    		}
    		}
        	XFrame f = new XFrame(message, "Basic Check");
	    	}
	    	catch (Exception ex)
			{
				throw new ExtensionException("Error in RSystmCheck-Extension: Error in check: \n"+ex);
			}
	    }
	}
      

    public static class JavaCheck extends DefaultCommand
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
	    	String rBits = new String();
	    	String message = new String();
	    	String javaterminal = new String();
	    	message += "Your JAVA_HOME variable is:\n"+System.getenv("JAVA_HOME")+"\n\n\n";
	    	message += "Try to do an Java version check from terminal:\n\n";
	    	try
	    	{
	        	// check up functions
	    		Runtime rt = Runtime.getRuntime();
	    		Process p = rt.exec("java -version");
	    		String s = null;
	    		BufferedReader stdInput = new BufferedReader(new 
	                    InputStreamReader(p.getInputStream()));
	            BufferedReader stdError = new BufferedReader(new 
	                    InputStreamReader(p.getErrorStream()));
	            // read the output from the command
	            while ((s = stdInput.readLine()) != null) {
	            	javaterminal += s+"\n";
	            }
	            // read any errors from the attempted command
	            while ((s = stdError.readLine()) != null) {
	            	javaterminal += s+"\n";
	            }
	            message += javaterminal;
	        }
	        catch (IOException e) {
	            message +="exception happened - here's what I know:\n"+e;
	            e.printStackTrace();
	        }

	        message += "\n\nComparison with the underlaying Java version here: \n\n";
	        String javahere = new String();
	        javahere += "java version \""+System.getProperty("java.version")+"\"\n";
	        javahere += System.getProperty("java.runtime.name");
	        javahere += " (build "+System.getProperty("java.runtime.version")+")\n";
	        javahere += System.getProperty("java.vm.name");
	        javahere += " (build "+System.getProperty("java.vm.version")+", "+System.getProperty("java.vm.info")+")\n";
	        
	        message += javahere;
	        
	        if (!javaterminal.contains(javahere))
	        {
	        	message += "\n\nProblem:\nJava version mismatch! If they differ between 32- and 64-bit version, rJava will not work correctly.\n"; 
	        }
	        XFrame f = new XFrame(message, "Java Check");
	    }
	}
    
	
    public static class RCheck extends DefaultCommand
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
	    	String rBits = new String();
	    	String message = new String();
	    	message += "Your R_HOME variable is:\n"+System.getenv("R_HOME")+"\n\n\n";
	    	
	    	try
	    	{
	    		message += "Try to load the R library (from PATH variable):\n";
			if (!System.getProperty("os.name").startsWith("Window"))
			{
			  JavaLibraryPath.addLibraryPath(new java.io.File(System.getenv("R_HOME")));
			  JavaLibraryPath.addLibraryPath(new java.io.File(System.getenv("R_HOME")+"/lib"));
			}
	    		System.loadLibrary("R");
	    		
	    		message += "...Successfull!\n\n\n";
	    	}
	    	catch (Exception e)
	    	{
	    		message += "...loading the library failed.\nPlease check you R_HOME and PATH environment variables!\n"+e+"\n\n\n";	    		
	    		if (System.getProperty("os.name").startsWith("Window"))
	    		{
		    		message += "Your PATH variable is:";
		    		String filepath = System.getenv("PATH");
		    		String substr = filepath;
		    		String rhome = System.getenv("R_HOME");
		    		String rpath = new String();
		    		while (substr.contains(";") && substr.length()>3)
		    		{
			    		int i = substr.indexOf(";");
			    		substr = substr.substring(i+1);	  
			    		int end = substr.indexOf(";");
			    		String path = new String();
			    		if (end == -1)
			    		{
			    			path = substr;
			    		}
			    		else
			    		{
			    			path = substr.substring(0, end);
			    		}
			    		if (path.contains(rhome))
			    		{
			    			rpath = path;
			    		}
			    		message += path+"\n"; 
		    		}
	    		}
	    	}
	    	message += "Try to do an R version check:\n\n";
	    	try
	    	{
	        	// check up functions
	    		Runtime rt = Runtime.getRuntime();
	    		Process p = rt.exec("R --version");
	    		String s = null;
	    		BufferedReader stdInput = new BufferedReader(new 
	                    InputStreamReader(p.getInputStream()));
	            BufferedReader stdError = new BufferedReader(new 
	                    InputStreamReader(p.getErrorStream()));
	            // read the output from the command
	            while ((s = stdInput.readLine()) != null) {
	            	message += s+"\n";
	            	if (message.contains("Platform:"))
	            	{
	            		int iplatform = message.indexOf("Platform:");
	            		String substr = message.substring(iplatform, message.length());
	            		rBits = substr.substring(substr.indexOf("(")+1,substr.indexOf(")"));	            	}
	            }
	            // read any errors from the attempted command
	            while ((s = stdError.readLine()) != null) {
	            	message += s+"\n";
	            	if (message.contains("Platform:"))
	            	{
	            		int iplatform = message.indexOf("Platform:");
	            		String substr = message.substring(iplatform, message.length());
	            		rBits = substr.substring(substr.indexOf("(")+1,substr.indexOf(")"));
	            	}
	            }
	        }
	        catch (IOException e) {
	            message +="exception happened - here's what I know:\n"+e;
	            e.printStackTrace();
	        }

	        String javaBits = System.getProperty("sun.arch.data.model", "?");
	        if (rBits.contains(javaBits)) {
	        	message += "\n\nCongratulations:\nJava version fits R version regarding the bits.\n(Java: "+javaBits+"-bit, R: "+rBits+")\n";
	        }
	        else
	        {
	        	message += "\n\nProblem:\nJava version doesn't fit R version regarding the bits.\n(Java: "+javaBits+"-bit, R: "+rBits+")\n";
	        }
	        XFrame f = new XFrame(message, "R Check");
	    }
	}
    
    
    public static class RJavaCheck1 extends DefaultCommand
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
	    	String rBits = new String();
	    	String message = new String();
	    	message += "Your JRI_HOME variable is:\n"+System.getenv("JRI_HOME")+"\n\n\n";
	    	message += "Try to do an rJava/JRI package check:\n\n";
	    	try
	    	{
	        	// check up functions
	    		Runtime rt = Runtime.getRuntime();
	    		Process p = rt.exec("R CMD check \""+System.getenv("JRI_HOME")+"/..\"");
	    		String s = null;
	    		BufferedReader stdInput = new BufferedReader(new 
	                    InputStreamReader(p.getInputStream()));
	            BufferedReader stdError = new BufferedReader(new 
	                    InputStreamReader(p.getErrorStream()));
	            // read the output from the command
	            while ((s = stdInput.readLine()) != null) {
	            	message += s+"\n";
	            }
	            // read any errors from the attempted command
	            while ((s = stdError.readLine()) != null) {
	            	message += s+"\n";
	            }
	        }
	        catch (IOException e) {
	            message += "exception happened - here's what I know:\n"+e;
	            e.printStackTrace();
	        }

	        message += "\n\nNote: It isn't a problem, if just the check for source package failed.";
	        XFrame f = new XFrame(message, "rJava Check1");
	    }
	}
    
    
    
    public static class RJavaCheck2 extends DefaultCommand
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
	    	String rBits = new String();
	    	String message = new String();
	    	message += "Your JRI_HOME variable is:\n"+System.getenv("JRI_HOME")+"\n\n\n";
	    	message += "Try to load rJava/JRI package check:\n\n";
	    	try
	    	{
	        	// check up functions
	    		Runtime rt = Runtime.getRuntime();
	    		String cmd = new String();
				if (System.getProperty("os.name").startsWith("Window")) 
				{
					cmd = "R --vanilla -e \"library(rJava); .path.package('rJava')\"";
				}
				else
				{
					cmd = "R --vanilla -e "+"lapply(c(\"library(rJava)\",\".path.package('rJava')\"),function(x){eval(parse(text=x))})";
				}
	    	
			Process p = rt.exec(cmd);

	    		//Process p = rt.exec("R --vanilla -e \"library(rJava); .path.package(\"rJava\")\"");

	    		String s = null;
	    		BufferedReader stdInput = new BufferedReader(new 
	                    InputStreamReader(p.getInputStream()));
	            BufferedReader stdError = new BufferedReader(new 
	                    InputStreamReader(p.getErrorStream()));
	            // read the output from the command
	            while ((s = stdInput.readLine()) != null) {
	            	message += s+"\n";
	            }
	            // read any errors from the attempted command
	            while ((s = stdError.readLine()) != null) {
	            	message += s+"\n";
	            }
	        }
	        catch (IOException e) {
	            message += "exception happened - here's what I know:\n"+e;
	            e.printStackTrace();
	        }
	        XFrame f = new XFrame(message, "rJava Check2");
	    }
	}
    
    
    public static class JRICheck extends DefaultCommand
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
	    	
	    	String message = new String();
	    	message += "Your JRI_HOME variable is:\n"+System.getenv("JRI_HOME")+"\n\n\n";
	    	
	    	try
	    	{    	
				final String filesep = System.getProperty("file.separator");
		  		String filepath = System.getenv("JRI_HOME");
		  		File file = new File(filepath+filesep+"JRI.jar");
		  		message += "Found JRI.jar: "+file.exists()+"\n";
		  		file = new File(filepath+filesep+"REngine.jar");
		  		message += "Found REngine.jar: "+file.exists()+"\n";
		  		file = new File(filepath+filesep+"JRIEngine.jar");
		  		message += "Found JRIEngine.jar: "+file.exists()+"\n";

	    		message += "Try to load the JRI library (from JRI_HOME variable):\n";
	    		// warning message - it can close the application if this fails!
		  		int n = javax.swing.JOptionPane.showConfirmDialog(
		  			    null,
		  			    "If loading the JRI library fails, it can happen that NetLogo closes immediately.\n" +
		  			    "Do you want to proceed?",
		  			    "Warning",
		  			    javax.swing.JOptionPane.YES_NO_OPTION);
		  		if (n == 0)
		  		{
			  		JavaLibraryPath.addLibraryPath(new java.io.File(filepath));
		    		System.loadLibrary("jri");
		    		
		    		message += "...Successfull!\n\n\n";
		  		}
		  		else {
		  			message += "Loading JRI library aborted by user!\n";
		  		}
	    	}
	    	catch (Exception e)
	    	{
	    		message += "...loading the library failed.\nPlease check you R_HOME and PATH environment variables!\n"+e+"\n\n\n";	    		
	    	}
	        XFrame f = new XFrame(message, "JRI Check");
	    }
	}
    
    
    public static class JavaGDCheck extends DefaultCommand
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
	    	String rBits = new String();
	    	String message = new String();
	    	message += "Try to do a JavaGD package check:\n\n";
	    	try
	    	{
	        	// check up functions
	    		Runtime rt = Runtime.getRuntime();
	    	
	    		String cmd = new String();
				if (System.getProperty("os.name").startsWith("Window")) 
				{
					cmd = "R --vanilla -e \"library(JavaGD); .path.package('JavaGD')\"";
				}
				else
				{
					cmd = "R --vanilla -e "+"lapply(c(\"library(JavaGD)\",\".path.package('JavaGD')\"),function(x){eval(parse(text=x))})";
				}
				Process p = rt.exec(cmd);

	    		//Process p = rt.exec("R --vanilla -e \"library(JavaGD); .path.package(\"JavaGD\")\"");
	    		String s = null;
	    		BufferedReader stdInput = new BufferedReader(new 
	                    InputStreamReader(p.getInputStream()));
	            BufferedReader stdError = new BufferedReader(new 
	                    InputStreamReader(p.getErrorStream()));
	            // read the output from the command
	            while ((s = stdInput.readLine()) != null) {
	            	message += s+"\n";
	            }
	            // read any errors from the attempted command
	            while ((s = stdError.readLine()) != null) {
	            	message += s+"\n";
	            }
	        }
	        catch (IOException e) {
	            message += "exception happened - here's what I know:\n"+e;
	            e.printStackTrace();
	        }
	        XFrame f = new XFrame(message, "JavaGD Check");
	    }
	}
}
