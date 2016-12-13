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

import java.util.Iterator;
import org.nlogo.api.Argument;
import org.nlogo.api.ExtensionException;
import org.rosuda.REngine.*;

/**
 * Class to handle the R connection and to transform data.
 *
 * @author JC Thiele
 * @version 1.0beta
 */
public class HoldRengineX {

  /** An instance of the R-Engine, used for communication with R. */
  public REngine rConnection = null;
  /** An object storing the local R environment (accessible from outside via nl.env) */
  public REXP WorkingEnvironment = null;

  /**
   * Constructor
   *
   * @param reng Instance of org.rosuda.REngine.REngine
   */
  public HoldRengineX(REngine reng) {
    this.rConnection = reng;
    this.WorkingEnvironment = null;
    try {
      if (rConnection.supportsEnvironments()) {
        WorkingEnvironment = rConnection.newEnvironment(null, false);
        sendEnvironmentToGlobal();
      } else {
        new ExtensionException("Error in R-Extension: Please use a newer version of JRI/rJava.");
      }
    } catch (Exception ex) {
      new ExtensionException("Error in R-Extension: Error in environment creation: " + ex);
    }
  }

  /**
   * Method to execute an R command
   *
   * @param engine The REngine
   * @param cmd The command to be executed
   * @param environment The current local working R environment
   * @throws REngineException
   * @throws REXPMismatchException
   * @return an REXP object containing the R return value
   */
  public org.rosuda.REngine.REXP execute(
      REngine engine, String cmd, REXP environment, boolean resolve)
      throws org.rosuda.REngine.REngineException, org.rosuda.REngine.REXPMismatchException,
          org.nlogo.api.ExtensionException {
    org.rosuda.REngine.REXP r;
    r = engine.parseAndEval("try(" + cmd + ",silent=TRUE)", environment, resolve);
    //r = engine.parseAndEval("try(" + cmd + ",silent=TRUE)", this.WorkingEnvironment, resolve);
    if (r.inherits("try-error")) {
      String error = r.asString();
      System.err.println("R Error: " + error);
      throw new org.nlogo.api.ExtensionException(error);
    }
    return r;
  }

  /**
   * Method to cast a NetLogo data type (including Lists) to a corresponding R data type
   *
   * @param obj The NetLogo data type to be casted
   * @return REXP object representing the NetLogo list
   */
  public REXP resolveNLObject(Object obj) throws ExtensionException {
    if (obj instanceof org.nlogo.core.LogoList) {
      org.nlogo.core.LogoList logoli = (org.nlogo.core.LogoList) obj;
      // if it is an empty NetLogo List
      if (logoli.size() == 0) {
        REXP ret = new REXPNull();
        // clean up
        logoli = null;
        return ret;
        //return new REXPNull();
      }
      try {
        if (logoli.get(0) instanceof java.lang.String) {
          String[] array = new String[logoli.size()];
          int i = 0;
          Iterator<Object> it = logoli.javaIterator();
          while (it.hasNext()) {
            array[i] = ((String) it.next());
            i++;
          }
          REXP ret = new REXPString(array);
          // clean up
          array = null;
          it = null;
          logoli = null;
          return ret;
          //return new REXPString(array);
        }
        // create an R-variable with Double[]-Value
        if (logoli.get(0) instanceof java.lang.Double) {
          double[] array = new double[logoli.size()];
          int i = 0;
          Iterator<Object> it = logoli.javaIterator();
          while (it.hasNext()) {
            array[i] = ((Double) it.next()).doubleValue();
            i++;
          }
          REXP ret = new REXPDouble(array);
          // clean up
          array = null;
          it = null;
          logoli = null;
          return ret;
          //return new REXPDouble(array);
        }
        // create an R-variable with Boolean[]-Value
        if (logoli.get(0) instanceof java.lang.Boolean) {
          boolean[] array = new boolean[logoli.size()];
          int i = 0;
          Iterator<Object> it = logoli.javaIterator();
          while (it.hasNext()) {
            array[i] = ((Boolean) it.next()).booleanValue();
            i++;
          }
          REXP ret = new REXPLogical(array);
          // clean up
          array = null;
          it = null;
          logoli = null;
          return ret;
          //return new REXPLogical(array);
        }
        if (logoli.get(0) instanceof org.nlogo.core.LogoList) {
          org.rosuda.REngine.RList rlistxx = new RList();
          for (int i = 0; i < logoli.size(); i++) {
            rlistxx.add(resolveNLObject(logoli.get(i)));
          }
          REXP ret = new REXPGenericVector(rlistxx);
          // clean up
          rlistxx = null;
          logoli = null;
          return ret;
          //return new REXPGenericVector(rlistxx);
        }
      }
      // if there is an error in casting the list, than we assume that
      // there is an NetLogo list with different types inside
      // than we try to cast it to an R list instead of an vector
      catch (Exception ex) {
        try {
          //System.out.println("error in loopOverListTest - try other way...");
          REXP[] rexparray = new REXP[logoli.size()];
          int i = 0;
          Iterator<Object> it = logoli.javaIterator();
          while (it.hasNext()) {
            Object ob = it.next();
            if (ob instanceof String) {
              rexparray[i] = new REXPString((String) ob);
            }
            if (ob instanceof Boolean) {
              rexparray[i] = new REXPLogical(((Boolean) ob).booleanValue());
            }
            if (ob instanceof Double) {
              rexparray[i] = new REXPDouble(((Double) ob).doubleValue());
            }
            if (ob instanceof org.nlogo.core.LogoList) {
              rexparray[i] = resolveNLObject(ob);
            }
            i++;
            // clean up
            ob = null;
          }
          RList rlist = new RList(rexparray);
          REXP ret = new REXPGenericVector(rlist);
          // clean up
          rlist = null;
          rexparray = null;
          it = null;
          logoli = null;
          return ret;
          //return new REXPGenericVector(new RList(rexparray));
        } catch (Exception ex2) {
          throw new ExtensionException("Error in R-Extension: Error in loopOverList:\n" + ex2);
        }
      }
    } else {
      REXP ret = rConnection.wrap(obj);
      obj = null;
      return ret;
      //return(rConnection.wrap(obj));
    }
    return null;
  }

  /**
   * Method to create an new R-Dataset from Agent or Agentset
   *
   * @param args[] submitted agents/data
   * @param outtype String describing the type of inputs, supported: "putnamedvector" and
   *     "putdataframe"
   * @exception ExtensionException
   */
  public void AssignAgentsetorAgent(Argument args[], boolean as_dataframe)
      throws ExtensionException {
    try {
      // get agent set or agent
      Object ag = args[1].get();
      /* get variable names */
      java.util.Vector<String> names = new java.util.Vector<String>();
      for (int i = 0; i < args.length - 2; i++) {
        names.add(args[i + 2].getString());
      }
      org.rosuda.REngine.RList rlist_base = new RList();
      /* if input is an agentset */
      if (ag instanceof org.nlogo.agent.AgentSet) {
        /* create LogoLists as temporary storage */
        org.nlogo.api.LogoListBuilder[] logoliarr = new org.nlogo.api.LogoListBuilder[names.size()];
        for (int i = 0; i < logoliarr.length; i++) {
          logoliarr[i] = new org.nlogo.api.LogoListBuilder();
        }
        org.nlogo.agent.AgentSet agentset = (org.nlogo.agent.AgentSet) ag;
        org.nlogo.agent.AgentIterator it = agentset.iterator();
        // iterate over agents
        while (it.hasNext()) {
          org.nlogo.agent.Agent agent = it.next();
          // iterate over variables
          if (agent instanceof org.nlogo.agent.Turtle) {
            for (int j = 0; j < names.size(); j++) {
              int varindex = agent.world().indexOfVariable(agent, names.get(j).toUpperCase());
              logoliarr[j].add(agent.getTurtleVariable(varindex));
            }
          } else if (agent instanceof org.nlogo.agent.Link) {
            for (int j = 0; j < names.size(); j++) {
              int varindex = agent.world().indexOfVariable(agent, names.get(j).toUpperCase());
              logoliarr[j].add(agent.getLinkVariable(varindex));
            }
          } else if (agent instanceof org.nlogo.agent.Patch) {
            for (int j = 0; j < names.size(); j++) {
              int varindex = agent.world().indexOfVariable(agent, names.get(j).toUpperCase());
              logoliarr[j].add(agent.getPatchVariable(varindex));
            }
          }
          // clean up
          agent = null;
        }
        for (int i = 0; i < names.size(); i++) {
          rlist_base.add(resolveNLObject(logoliarr[i].toLogoList()));
        }
        // clean up
        logoliarr = null;
        agentset = null;
        it = null;
      }
      /* if input isn't an agentset but agent */
      else if (ag instanceof org.nlogo.agent.Agent) {
        org.nlogo.agent.Agent agent = (org.nlogo.agent.Agent) ag;
        for (int j = 0; j < names.size(); j++) {
          org.rosuda.REngine.RList rlist = new RList();
          // get index of agent variables
          int varindex = agent.world().indexOfVariable(agent, names.get(j).toUpperCase());

          if (agent instanceof org.nlogo.agent.Turtle) {
            rlist.add(resolveNLObject(agent.getTurtleVariable(varindex)));
          }
          if (agent instanceof org.nlogo.agent.Link) {
            rlist.add(resolveNLObject(agent.getLinkVariable(varindex)));
          }
          if (agent instanceof org.nlogo.agent.Patch) {
            rlist.add(resolveNLObject(agent.getPatchVariable(varindex)));
          }
          rlist_base.add(new REXPGenericVector(rlist));
          // clean up
          rlist = null;
        }
        // clean up
        agent = null;
      }
      rlist_base.names = names;
      if (as_dataframe) {
        rConnection.assign(
            args[0].getString(),
            org.rosuda.REngine.REXP.createDataFrame(rlist_base),
            this.WorkingEnvironment);
        ;
      } else {
        rConnection.assign(
            args[0].getString(), new REXPGenericVector(rlist_base), this.WorkingEnvironment);
      }

      // release the variable contents (for cleaning up)
      rlist_base = null;
      ag = null;
      names.clear();
      names = null;
      //System.gc();
      //System.gc();
    } catch (Exception ex) {
      throw new ExtensionException("Error in AssignAgentsetorAgent: \n" + ex);
    }
  }

  /**
   * Method to resolve R Lists and R Vectors to Java arrays
   *
   * @param result An REXP object containing an R List or Vector
   * @return An Object containing an Java array
   * @throws REXPMismatchException
   */
  private Object resolveListAndVector(REXP result) throws REXPMismatchException {
    if (result.isNumeric()) {
      double[] dbarray = result.asDoubles();
      org.nlogo.api.LogoListBuilder llist =
          new org.nlogo.api.LogoListBuilder(); //new org.nlogo.core.LogoList();
      for (int i = 0; i < dbarray.length; i++) {
        llist.add((Double) dbarray[i]);
      }
      // clean up
      dbarray = null;
      result = null;
      return llist.toLogoList();
    }
    if (result.isInteger()) {
      int[] intarray = result.asIntegers();

      org.nlogo.api.LogoListBuilder llist =
          new org.nlogo.api.LogoListBuilder(); //new org.nlogo.core.LogoList();
      for (int i = 0; i < intarray.length; i++) {
        llist.add(new Double(((Integer) intarray[i]).doubleValue()));
      }
      // clean up
      intarray = null;
      result = null;
      return llist.toLogoList();
    }
    if (result.isString()) {
      String[] strarray = result.asStrings();
      org.nlogo.api.LogoListBuilder llist =
          new org.nlogo.api.LogoListBuilder(); //new org.nlogo.core.LogoList();
      for (int i = 0; i < strarray.length; i++) {
        llist.add(strarray[i]);
      }
      // clean up
      strarray = null;
      result = null;
      return llist.toLogoList();
    }
    if (result.isLogical()) {
      int[] boolarray = result.asIntegers();
      org.nlogo.api.LogoListBuilder llist =
          new org.nlogo.api.LogoListBuilder(); //new org.nlogo.core.LogoList();
      for (int i = 0; i < boolarray.length; i++) {
        if (boolarray[i] == 1) {
          llist.add(true);
        } else {
          llist.add(false);
        }
      }
      // clean up
      boolarray = null;
      result = null;
      return llist.toLogoList();
    }
    result = null;
    return null;
  }

  /**
   * Method to cast an REXP object to it underlaying Java type
   *
   * @param result The REXP object
   * @return An Object containing the casted REXP object
   * @throws REXPMismatchException
   */
  public Object returnObject(REXP result) throws REXPMismatchException {
    if (result.length() == 1) {

      if (result.isString() || result.isNull()) {
        return result.asString();
      }
      if (result.isNumeric()) {
        return result.asDouble();
      }
      if (result.isInteger()) {
        return result.asInteger();
      }
      if (result.isLogical()) {
        if (result.asInteger() == 1) {
          return new Boolean(true);
        } else {
          return new Boolean(false);
        }
      }
    }
    if (result.isList()) {
      org.nlogo.api.LogoListBuilder llist =
          new org.nlogo.api.LogoListBuilder(); //new org.nlogo.core.LogoList();
      java.util.ListIterator<?> li = result.asList().listIterator();
      while (li.hasNext()) {
        REXP val = (REXP) li.next();
        llist.add(returnObject(val));
        // clean up
        val = null;
      }
      // clean up
      li = null;
      result = null;
      return llist.toLogoList();
    }
    if (result.isVector()) {
      return resolveListAndVector(result);
    }
    return null;
  }

  /**
   * Method to create an R variable nl.env containing a reference to the local environment
   *
   * @throws ExtensionException
   */
  public void sendEnvironmentToGlobal() throws ExtensionException {
    try {
      this.execute(this.rConnection, "nl.env <<- environment()", this.WorkingEnvironment, true);
    } catch (Exception ex) {
      throw new ExtensionException(
          "Error in R-Extension: An error occured while sending local environment to global: "
              + ex);
    }
  }
}
