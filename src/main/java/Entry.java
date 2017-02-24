package org.nlogo.extension.r;

/*
This file is part of NetLogo-R-Extension.

Contact: jthiele at gwdg.de
Copyright (C) 2009-2012 Jan C. Thiele

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

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JOptionPane;
import org.nlogo.api.Argument;
import org.nlogo.api.Command;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Reporter;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.workspace.ExtensionManager$;
import org.rosuda.REngine.*;

/**
 * Class to provide access to Gnu R from NetLogo. Contains definitions of NetLogo primitives.
 *
 * @author JC Thiele
 * @version 1.0beta
 */
public class Entry extends org.nlogo.api.DefaultClassManager {
  /**
   * Object of type ExtensionManager to store the R console instance. Needed because the dynamic
   * library (jri.dll/jri.so) can't be loaded twice. Because NetLogo can't cast an Object stored in
   * the ExtensionManager via ex.storeObject/ex.retrieveObject to classes and interfaces other than
   * java natives and NetLogo ones, using NetLogo interfaces is one (dirty) way, to store the
   * ShellWindow object.
   */
  private static org.nlogo.api.ExtensionManager shellwin;
  /** Object containing the connection to R */
  public static HoldRengineX rConn = null;
  /**
	 * Object to synchronize console input/execution
	 *//*private*/ public static ConsoleSync rSync = new ConsoleSync();

  static String osName = System.getProperty("os.name").toLowerCase();

  public interface LibC extends Library {
    public int setenv(String name, String value, int overwrite);
  }

  public interface WinLibC extends Library {
    public int _putenv_s(String key, String value);
  }

  public interface WinLib32 extends Library {
    public int SetDllDirectoryA(String directory);
  }

  static LibC libc = null;
  static WinLibC winLibc = null;
  static WinLib32 winLib32 = null;

  static {
    try {
      if (osName.startsWith("windows", 0)) {
        String msvcrLibName =
            System.getProperty("org.nlogo.r.extension.msvcr.lib.name", "msvcr120");
        winLibc = (WinLibC) Native.loadLibrary(msvcrLibName, WinLibC.class);
        winLib32 = (WinLib32) Native.loadLibrary("kernel32", WinLib32.class);
      } else {
        libc = (LibC) Native.loadLibrary("c", LibC.class);
      }
    } catch (Throwable t) {
      System.err.println("Error loading native library: " + t.getMessage());
      t.printStackTrace();
    }
  }

  Configuration configuration = null;

  /**
   * Method executed when extension is loaded and only then. Initializes the connection to R and
   * ShellWindow or loads the stored ShellWindow instance from storage.
   *
   * @param em an instance of ExtensionManager, handled by NetLogo
   */
  public void runOnce(org.nlogo.api.ExtensionManager em) throws ExtensionException {
    configuration = Configuration.fromRExtensionProperties();
    Path rHome = validateRHome();
    try {
      // dynamically load of the needed JARs from the JRI package
      loadJRILibraries(findJRIHomePath(configuration), rHome);

      org.rosuda.REngine.REngine lastEngine = org.rosuda.REngine.REngine.getLastEngine();
      // if no further REnginer was initialized

      // Check for headless mode
      // if NetLogo running headless, do not create interactiveShell and REngineCallbacks
      // Don't forget to call "stop" in the model!
      if (System.getProperty("java.awt.headless", "false") == "true"
          || System.getProperty("org.nlogo.preferHeadless") == "true") {
        rConn = headlessREngine();
        addRLibPaths(rConn, false);
      } else {
        // NetLogo running in GUI mode
        if (lastEngine == null) {
          rConn = guiREngine();
          addRLibPaths(rConn, true);
          em.storeObject(shellwin);
        }
        // otherwise, reload the last REngine object and retrieve the stored ShellWindow object
        else {
          // this will also create a new Environment
          rConn = new HoldRengineX(lastEngine);
          shellwin = (org.nlogo.api.ExtensionManager) em.retrieveObject();
        }
      }
    } catch (UnsatisfiedLinkError ex) {
      throw new ExtensionException("Error loading JRI library (Error #03): \n" + ex);
    } catch (ExtensionException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ExtensionException(
          "Error in R-Extension: Error in runOnce (Error #04): \n" + ex, ex);
    }
  }

  /** Returns the headless R engine */
  HoldRengineX headlessREngine() throws ExtensionException {
    try {
      Class<?> rengineClass = Class.forName("org.rosuda.REngine.JRI.JRIEngine");
      Class<?> callbacks_class = Class.forName("org.rosuda.REngine.REngineCallbacks");
      Method thisMethod = rengineClass.getDeclaredMethod("createEngine");
      REngine rToStore = (REngine) thisMethod.invoke(rengineClass);
      return new HoldRengineX(rToStore);
    } catch (Exception ex) {
      throw new ExtensionException("Error Initializing Headless R Extension (Error #04).\n", ex);
    }
  }

  private static class ExitTrappedException extends SecurityException {}

  static final SecurityManager exitPreventingSecurityManager =
      new SecurityManager() {
        public void checkPermission(Permission permission) {
          if (permission.getName().startsWith("exitVM")) {
            throw new ExitTrappedException();
          }
        }
      };

  /** Returns the GUI R engine */
  HoldRengineX guiREngine() throws ExtensionException {
    // We do a bit of a song and dance here because JRIEngine.createEngine invokes
    // a method that will perform a hard exit (System.exit(1)) if it can't find the
    // appropriate library. This is exactly the sort of error we would like to report
    // to the user. So we install a security manager that will prevent exiting and
    // raise an exception if someone tries it, then we catch that exception and
    // percolate the error to the user.
    try {
      Class<?> iashell_class = Class.forName("org.nlogo.extension.r.ShellWindow");
      Class<?> partypes1[] = new Class<?>[] {ConsoleSync.class};
      Constructor<?> shellConstructor = iashell_class.getConstructor(partypes1);
      Object arglist1[] = new Object[] {rSync};
      Object shell = shellConstructor.newInstance(arglist1);
      org.nlogo.api.ExtensionManager tc = (org.nlogo.api.ExtensionManager) shell;
      shellwin = tc;
      Class<?> rengineClass = Class.forName("org.rosuda.REngine.JRI.JRIEngine");
      Class<?> callbacks_class = Class.forName("org.rosuda.REngine.REngineCallbacks");
      Class<?> partypes[] = new Class<?>[] {String[].class, callbacks_class, boolean.class};
      Object arglist[] = new Object[] {new String[] {"--no-save"}, tc, true};
      Method thisMethod = rengineClass.getDeclaredMethod("createEngine", partypes);
      System.setSecurityManager(exitPreventingSecurityManager);
      REngine rToStore = (REngine) thisMethod.invoke(rengineClass, arglist);
      return new HoldRengineX(rToStore);
    } catch (ExitTrappedException ex) {
      throw new ExtensionException("Could not load R libraries. (Error #06)\n", ex);
    } catch (ClassNotFoundException ex) {
      throw new ExtensionException("Error initializing R extension. (Error #04)\n" + ex, ex);
    } catch (NoSuchMethodException ex) {
      throw new ExtensionException("Error initializing R extension. (Error #04)\n" + ex, ex);
    } catch (IllegalAccessException ex) {
      throw new ExtensionException("Error initializing R extension. (Error #04)\n" + ex, ex);
    } catch (InstantiationException ex) {
      throw new ExtensionException("Error initializing R extension. (Error #04)\n" + ex, ex);
    } catch (InvocationTargetException ex) {
      if (ex.getCause() instanceof ExitTrappedException) {
        throw new ExtensionException("Could not load R libraries. (Error #06)\n", ex);
      } else {
        throw new ExtensionException(
            "Error initializing R extension. (Error #04)\n" + ex + " " + ex.getCause(), ex);
      }
    } finally {
      System.setSecurityManager(null);
    }
  }

  /**
   * Validates that R_HOME is set to a valid path, sets it from property if not set in environment.
   */
  public Path validateRHome() throws ExtensionException {
    String rHomeEnv = System.getenv("R_HOME");
    if (rHomeEnv == null || rHomeEnv.isEmpty()) {
      if (!Files.exists(configuration.rHomePath())) {
        throw new ExtensionException(
            "Could not find R Home. Please set R home in the environment or in user.properties (Error #01)\n");
      }
      int setResult = 0;
      try {
        if (osName.startsWith("windows", 0) && winLibc != null) {
          setResult =
              winLibc._putenv_s("R_HOME", configuration.rHomePath().toAbsolutePath().toString());
        } else {
          setResult =
              libc.setenv("R_HOME", configuration.rHomePath().toAbsolutePath().toString(), 1);
        }
      } catch (Exception e) {
        setResult = -1;
      }
      if (setResult != 0) throw new ExtensionException("Error setting R_HOME (#05).\n");
    } else {
      Path rHomePath = Paths.get(System.getenv("R_HOME"));
      if (!Files.exists(rHomePath)) {
        throw new ExtensionException(
            "Could not find R at: "
                + System.getenv("R_HOME")
                + " . Please set R home in the environment or in user.properties (Error #01)\n");
      }
      configuration.setRHomePath(rHomePath);
    }
    return configuration.rHomePath();
  }

  static Path findJRIHomePath(Configuration configuration) throws ExtensionException {
    return configuration
        .jriHomePaths()
        .stream()
        .filter(path -> Files.exists(path.resolve("JRI.jar")))
        .findFirst()
        .orElseThrow(
            () ->
                new ExtensionException(
                    "Cannot locate rJava/JRI. Please check the location of your rJava installation and add a user.properties file in the r extension directory (Error #02).\n"));
  }

  /** Adds standard JRI Libraries */
  void loadJRILibraries(Path jriHomePath, Path rHome) throws ExtensionException {
    List<String> jarList = Arrays.asList(new String[] {"JRI.jar", "REngine.jar", "JRIEngine.jar"});

    for (String jar : jarList) {
      try {
        JavaLibraryPath.addFile(jriHomePath.resolve(jar).toFile());
      } catch (IOException ex) {
        throw new ExtensionException("Error loading JRI Libraries (Error #04)\n", ex);
      }
    }

    Path jriLib = jriHomePath;
    Path jri64Lib = jriHomePath.resolve("x64");
    Path jri32Lib = jriHomePath.resolve("i386");
    Optional<Path> rLibPath = Optional.empty();
    String dataModel = System.getProperty("sun.arch.data.model", "?");

    if (Files.exists(jri64Lib) && dataModel.contains("64")) {
      jriLib = jri64Lib;
      if (winLib32 != null) {
        rLibPath = Optional.of(rHome.resolve("bin/x64"));
      }
    } else if (Files.exists(jri32Lib) && dataModel.contains("32")) {
      jriLib = jri32Lib;
      if (winLib32 != null) {
        rLibPath = Optional.of(rHome.resolve("bin/i386"));
      }
    }

    try {
      try {
        JavaLibraryPath.addLibraryPath(jriLib.toFile());
      } catch (Exception ex) {
        throw new ExtensionException(
            "Error Initializing R Extension: could not add JRI to library path (Error #04).\n", ex);
      }
      rLibPath.ifPresent(libPath -> winLib32.SetDllDirectoryA(libPath.toAbsolutePath().toString()));
    } catch (UnsatisfiedLinkError localUnsatisfiedLinkError) {
      throw new ExtensionException(
          "Cannot load rJava libraries. Please check your rJava installation. (Error #03)\n"
              + localUnsatisfiedLinkError);
    }
  }

  private void addRLibPaths(HoldRengineX rConn, boolean guiPresent) {
    try {
      if (! configuration.rLibPaths().isEmpty()) {
        StringBuilder pathsString = new StringBuilder("c(");
        for (Path p : configuration.rLibPaths()) {
          // if the file path has backslashes, we need to escape them. If this isn't done, we get a hard
          // crash on Windows RG 2017-2-24
          pathsString.append("'" + p.toString().replace("\\", "\\\\") + "',");
        }
        pathsString.deleteCharAt(pathsString.length() - 1);
        pathsString.append(")");
        rConn.execute(
            rConn.rConnection, ".libPaths(" + pathsString.toString() + ")", rConn.WorkingEnvironment, true);
      }
    } catch (Exception ex) {
      if (guiPresent) {
        JOptionPane.showMessageDialog(
            null,
            "Error while configuring r library paths: " + ex,
            "Error in R-Extension",
            JOptionPane.INFORMATION_MESSAGE);
      } else {
        System.err.println("Error while configuring r library paths, continuing: " + ex);
      }
    }
  }

  /**
   * Method to define the NetLogo primitives.
   *
   * @param primManager an instance of PrimitiveManager, handled by NetLogo
   */
  public void load(org.nlogo.api.PrimitiveManager primManager) {
    primManager.addPrimitive("put", new Put());
    primManager.addPrimitive("putNamedList", new PutNamedList());
    primManager.addPrimitive("putList", new PutList());
    primManager.addPrimitive("putDataframe", new PutDataframe());
    primManager.addPrimitive("putAgent", new PutAgent());
    primManager.addPrimitive("putAgentDf", new PutAgentDataFrame());
    primManager.addPrimitive("eval", new Eval());
    primManager.addPrimitive("__evalDirect", new EvalDirect());
    primManager.addPrimitive("get", new Get());
    primManager.addPrimitive("gc", new GC());
    primManager.addPrimitive("clear", new ClearWorkspace());
    primManager.addPrimitive("clearLocal", new ClearLocalWorkspace());
    primManager.addPrimitive("interactiveShell", new interactiveShell());
    primManager.addPrimitive("setPlotDevice", new SetPlotDevice());
    primManager.addPrimitive("stop", new Stop());
    primManager.addPrimitive("r-home", new DebugPrim(new RPath()));
    primManager.addPrimitive("jri-path", new DebugPrim(new JRIPath()));
  }

  @FunctionalInterface
  interface DebugSupplier {
    public String get() throws ExtensionException;
  }

  class RPath implements DebugSupplier {
    @Override
    public String get() throws ExtensionException {
      return configuration.rHomePath().toString();
    }
  }

  class JRIPath implements DebugSupplier {
    @Override
    public String get() throws ExtensionException {
      return findJRIHomePath(configuration).toString();
    }
  }

  public static class DebugPrim implements Reporter {
    final DebugSupplier supplier;

    public DebugPrim(DebugSupplier getValue) {
      supplier = getValue;
    }

    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax(Syntax.StringType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      return supplier.get();
    }
  }

  /**
   * Class to stop the R connection. Needed for (true) headless runs ("java.awt.headless" ==
   * "true"). (Implementation of the primitive stop)
   *
   * @since new in Version 1.1
   */
  public static class Stop implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        if (System.getProperty("java.awt.headless", "false") == "true") {
          rConn.rConnection.close();
        }
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in stop: \n" + ex);
      }
    }
  }

  /**
   * Class to setup the JavaGD plot device. (Implementation of the primitive setPlotDevice)
   *
   * @since new in Version 1.0beta
   */
  public static class SetPlotDevice implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        if (!System.getProperty("java.awt.headless", "false").equals("true")) {
          shellwin.storeObject(null);
        }
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in setPlotDevice: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to setup InteractiveShell. (Implementation of the primitive interactiveShell)
   *
   * @since new in Version 0.3
   */
  public static class interactiveShell implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        if (!System.getProperty("java.awt.headless", "false").equals("true")) {
          if (!Entry.shellwin.anyExtensionsLoaded()) {
            Entry.shellwin.finishFullCompilation();
          }
        }
      } catch (Exception ex) {
        throw new ExtensionException(
            "Error in R-Extension: Error in interactiveShell: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to create a new Vector from Agent-Variables. (Implementation of the primitive putAgent)
   */
  public static class PutAgent implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(
          new int[] {
            Syntax.StringType(),
            Syntax.AgentsetType() | Syntax.AgentType(),
            Syntax.StringType() | Syntax.RepeatableType()
          });
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        rConn.AssignAgentsetorAgent(args, false);
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in PutAgent: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to create a new R-DataFrame from Agent-Variables. (Implementation of the primitive
   * putAgentDf)
   */
  public static class PutAgentDataFrame implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(
          new int[] {
            Syntax.StringType(),
            Syntax.AgentsetType() | Syntax.AgentType(),
            Syntax.StringType() | Syntax.RepeatableType()
          });
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        rConn.AssignAgentsetorAgent(args, true);
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in PutAgentDf: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to create a new R-DataFrame from NetLogo-Values. (Implementation of the primitive
   * putDataframe)
   */
  public static class PutDataframe implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(
          new int[] {Syntax.StringType(), Syntax.WildcardType() | Syntax.RepeatableType()});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        java.util.Vector<String> names = new java.util.Vector<String>();
        org.rosuda.REngine.RList rlist = new RList();
        for (int i = 0; i < args.length - 2; i += 2) {
          names.add(args[i + 1].getString());
          rlist.add(rConn.resolveNLObject(args[i + 2].get()));
        }
        rlist.names = names;
        rConn.rConnection.assign(
            args[0].getString(),
            org.rosuda.REngine.REXP.createDataFrame(rlist),
            rConn.WorkingEnvironment);
        ;
        // clean up
        names.clear();
        names = null;
        rlist = null;
        //System.gc();
        //System.gc();
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in PutDataFrame: \n" + ex, ex);
      }
    }
  }

  /** Class to create a new R-List from NetLogo-Values. (Implementation of the primitive putList) */
  public static class PutList implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(
          new int[] {Syntax.StringType(), Syntax.WildcardType() | Syntax.RepeatableType()});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        java.util.Vector<String> names = new java.util.Vector<String>();
        org.rosuda.REngine.RList rlist = new RList();
        for (int i = 0; i < args.length - 1; i++) {
          names.add(((Integer) i).toString());
          rlist.add(rConn.resolveNLObject(args[i + 1].get()));
        }
        rlist.names = names;
        rConn.rConnection.assign(
            args[0].getString(), new REXPGenericVector(rlist), rConn.WorkingEnvironment);
        // clean up
        names.clear();
        names = null;
        rlist = null;
        //System.gc();
        //System.gc();
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in PutVector: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to create a new named R-List from NetLogo-Values. (Implementation of the primitive
   * putNamedList)
   */
  public static class PutNamedList implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(
          new int[] {Syntax.StringType(), Syntax.WildcardType() | Syntax.RepeatableType()});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        java.util.Vector<String> names = new java.util.Vector<String>();
        org.rosuda.REngine.RList rlist = new RList();
        for (int i = 0; i < args.length - 2; i += 2) {
          names.add(args[i + 1].getString());
          rlist.add(rConn.resolveNLObject(args[i + 2].get()));
        }
        rlist.names = names;
        rConn.rConnection.assign(
            args[0].getString(), new REXPGenericVector(rlist), rConn.WorkingEnvironment);
        // clean up
        names.clear();
        names = null;
        rlist = null;
        //System.gc();
        //System.gc();
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in PutNamedList: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to create a new R-Variable/Array from NetLogo-Values. (Implementation of the primitive
   * put)
   */
  public static class Put implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {Syntax.StringType(), Syntax.WildcardType()});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        REXP val = rConn.resolveNLObject(args[1].get());
        rConn.rConnection.assign(args[0].getString(), val, rConn.WorkingEnvironment);
        val = null;
        //System.gc();
        //System.gc();
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in Put. \n" + ex, ex);
      }
    }
  }

  /**
   * Class to evaluate submitted String in R without results. (Implementation of the primitive eval)
   */
  public static class Eval implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {Syntax.StringType()});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        REXP returnVal =
            rConn.execute(rConn.rConnection, args[0].getString(), rConn.WorkingEnvironment, true);
        returnVal = null;
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in Eval: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to evaluate submitted String directly in R Console without results. (Implementation of
   * the primitive evalDirect) Some packages (e.g. ggplot2) doesn't work with eval
   */
  public static class EvalDirect implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {Syntax.StringType()});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        String[] cmdArray = args[0].getString().split("\n");
        String c = null;
        for (int i = 0; i < cmdArray.length; i++) {
          c = cmdArray[i];
          Entry.rSync.triggerNotification(c.trim());
          // clean up
          c = null;
        }
        //REXP returnVal =  rConn.execute(rConn.rConnection, args[0].getString(), rConn.WorkingEnvironment, true);
        // clean up
        cmdArray = null;
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in Eval: \n" + ex, ex);
      }
    }
  }

  /**
   * Class to evaluate submitted String in R, and send back the results to NetLogo. (Implementation
   * of the primitive get)
   */
  public static class Get implements Reporter {
    public Syntax getSyntax() {
      return SyntaxJ.reporterSyntax(new int[] {Syntax.StringType()}, Syntax.WildcardType());
    }

    public Object report(Argument args[], Context context)
        throws ExtensionException, LogoException {
      try {
        REXP returnVal =
            rConn.execute(rConn.rConnection, args[0].getString(), rConn.WorkingEnvironment, true);
        Object retObj = rConn.returnObject(returnVal);
        // clean up
        returnVal = null;
        return retObj;
        //return rConn.returnObject(returnVal);
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in Get. \n" + ex, ex);
      }
    }
  }

  /**
   * Class to perform Java and R Garbage Collection. (Implementation of the primitive javagc)
   *
   * @since new in version 1.2
   */
  public static class GC implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        System.gc();
        rConn.execute(rConn.rConnection, "gc(reset=T)", rConn.WorkingEnvironment, true);
        rConn.rConnection.parseAndEval("gc(reset=T)");
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in GC: \n" + ex, ex);
      }
    }
  }

  /** Class to clear R workspace. (Implementation of the primitive clear) */
  public static class ClearWorkspace implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        System.gc();
        REXP returnVal =
            rConn.execute(rConn.rConnection, "rm(list=ls())", rConn.WorkingEnvironment, true);
        returnVal = null;
        rConn.rConnection.parseAndEval("rm(list=ls())");
        rConn.rConnection.parseAndEval("gc(reset=T)");
        System.gc();
        rConn.sendEnvironmentToGlobal();
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in ClearWorkspace: \n" + ex, ex);
      }
    }
  }

  /** Class to clear local (nl.env) R workspace. (Implementation of the primitive clear) */
  public static class ClearLocalWorkspace implements Command {
    public Syntax getSyntax() {
      return SyntaxJ.commandSyntax(new int[] {});
    }

    public String getAgentClassString() {
      return "OTPL";
    }

    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
      try {
        System.gc();
        REXP returnVal =
            rConn.execute(rConn.rConnection, "rm(list=ls())", rConn.WorkingEnvironment, true);
        returnVal = null;
        REXP returnVal2 =
            rConn.execute(rConn.rConnection, "gc(reset=T)", rConn.WorkingEnvironment, true);
        returnVal2 = null;
        System.gc();
      } catch (Exception ex) {
        throw new ExtensionException(
            "Error in R-Extension: Error in ClearLocalWorkspace: \n" + ex, ex);
      }
    }
  }

  /**
   * Method executed when extension is unloaded. Clears the R workspace, destroy MessageWindow (if
   * created) and reset Debugging.
   */
  public void unload() throws ExtensionException {
    // run unload only when NetLogo is runnning in GUI mode
    if (!System.getProperty("java.awt.headless", "false").equals("true")) {
      // clear workspace
      try {
        // clear workspace
        System.gc();
        REXP returnVal =
            rConn.execute(rConn.rConnection, "rm(list=ls())", rConn.WorkingEnvironment, true);
        returnVal = null;
        rConn.rConnection.parseAndEval("rm(list=ls())");
        rConn.rConnection.parseAndEval("gc(reset=T)");
        System.gc();
      } catch (Exception ex) {
        throw new ExtensionException("Error in R-Extension: Error in unload: \n" + ex, ex);
      }
      try {
        // check if ShellWindow is open - if so, close it...
        if (Entry.shellwin.anyExtensionsLoaded()) {
          Entry.shellwin.reset();
        }
      } catch (Exception ex) {
        throw new ExtensionException(
            "Error in R-Extension: Error in making interactiveShell invisible: \n" + ex, ex);
      }
    }
  }
}
