package org.nlogo.extension.r;

import org.rosuda.REngine.Rserve.*;
import org.rosuda.REngine.*;

public class Standalone_Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try
		{
			RConnection c = new RConnection("localhost",6622);
			System.out.println("login required: "+c.needLogin());
			c.login("jthiele", "test");
			
			REXP x = c.eval("R.version.string");
			System.out.println(x.asString());
			
			double[] d= c.eval("rnorm(100)").asDoubles();
			System.out.println("d[1]: "+d[1]);
			
			
			double[] dataX = {465,75,23,523,64,23};
			double[] dataY = {235,23,64,23,4,125};
			c.assign("x", dataX);
			c.assign("y", dataY);
			RList l = c.eval("lowess(x,y)").asList();
			System.out.println("l[1]: "+l);
			
			double[] lx = l.at("x").asDoubles();
			double[] ly = l.at("y").asDoubles();
			System.out.println("lx[1]: "+lx[1]);
			System.out.println("ly[1]: "+ly[1]);
			
			double[] dataYY = {23.4,2131,32,23,53,12,43,64};
			double[] _c = {2,1,3,3,3,2,4,6};
			double[] _b = {4,1,2,3,5,1,4,4};
			double[] _a = {9,2,2,2,5,1,4,6};
			c.assign("a", _a);
			c.assign("b", _b);
			c.assign("c", _c);
			c.assign("y", dataYY);
			c.voidEval("m<-lm(y~a+b+c)");
			double [] coeff = c.eval("coefficients(m)").asDoubles();
			System.out.println("coeff: "+coeff);
			
			String myCode = "test <- 999";			
			REXP r = c.parseAndEval("try("+myCode+",silent=TRUE)");
			if (r.inherits("try-error")) 
				System.err.println("Error: "+r.asString());
			else { 
				// success ...
				System.out.println("tippitoppi1: "+r);
			}
			
			/*
			myCode = "require(JavaGD)";
			r = c.parseAndEval("try("+myCode+",silent=TRUE)");
			if (r.inherits("try-error")) 
				System.err.println("Error: "+r.asString());
			else { 
				// success ...
				System.out.println("tippitoppi1: "+r);
			}
			
			myCode = ".path.package('JavaGD')";
			r = c.parseAndEval("try("+myCode+",silent=TRUE)");
			if (r.inherits("try-error")) 
				System.err.println("Error: "+r.asString());
			else { 
				// success ...
				System.out.println("tippitoppi1: "+r.asString());
			}
			*/
			
			/*
			c.assign(".tmp.", myCode);
			REXP r3 = c.parseAndEval("try(eval(parse(text=.tmp.)),silent=TRUE)");
			if (r3.inherits("try-error")) 
			{
				System.err.println("Error: "+r3.toString());
			}
			else { 
				// success .. 
				System.out.println("tippitoppi2: "+r);
			}
			*/
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}
	}

}
