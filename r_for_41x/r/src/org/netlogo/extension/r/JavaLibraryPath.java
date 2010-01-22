package org.netlogo.extension.r;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
 
/** 
 * This code came from:
 * http://forum.java.sun.com/thread.jspa?threadID=627890&start=15
 *
 * The Terms of Use for the website, in the "Content Submitted to Sun"
 * section states: You (the author) grant Sun and all other users of
 * the Website an irrevocable, worldwide, royalty-free, nonexclusive
 * license to use, reproduce, modify, distribute, transmit, display,
 * perform, adapt, resell and publish such Content (including in
 * digital form).
 *
 * http://www.sun.com/termsofuse.jsp
 *
 * The username of the original post has no name or email associated
 * with it.
 *
 * Adds a path to the java.library.path System property 
 * and updates the ClassLoader. Uses reflection to allow
 * update to private system members. Will not work if JVM
 * security policy gets in the way (like in an applet).
 * Will not work if Sun changes the private members.
 * This really shouldn't be used at all...
 */
public class JavaLibraryPath
{
	public static void addLibraryPath(File path) throws Exception
	{
		// Append the specified path to the
		// existing java.library.path (if there is one already)
		String newLibraryPath = System.getProperty("java.library.path");
		if (newLibraryPath == null || newLibraryPath.length() < 1)
		{
			newLibraryPath = path.getCanonicalPath();
		}
		else
		{
			newLibraryPath += File.pathSeparator +
				path.getCanonicalPath();
		}
 
		// Reflect into java.lang.System to get the 
		// static Properties reference
		Field f = System.class.getDeclaredField("props");
		f.setAccessible(true);
		Properties props = (Properties) f.get(null);
		// replace the java.library.path with our new one
		props.put("java.library.path", newLibraryPath);
 
		// The classLoader may have already been initialized,
		// so it needs to be fixed up.
		// Reflect into java.lang.ClassLoader to get the 
		// static String[] of user paths to native libraries
		Field usr_pathsField =
				ClassLoader.class.getDeclaredField("usr_paths");
		usr_pathsField.setAccessible(true);
		String[] usr_paths = (String[]) usr_pathsField.get(null);
		String[] newUsr_paths = new String[usr_paths == null ? 1 : 
			usr_paths.length + 1];
		if (usr_paths != null)
		{
			System.arraycopy(usr_paths, 0, newUsr_paths,
				0, usr_paths.length);
		}
		// Add the specified path to the end of a new String[]
		// of user paths to native libraries
		newUsr_paths[newUsr_paths.length - 1] = path.getAbsolutePath();
		usr_pathsField.set(null, newUsr_paths);
	}
	
	private static final Class[] parameters = new Class[]{URL.class};

	public static void addFile(String s) throws IOException {
		File f = new File(s);
		addFile(f);
	}//end method
 
	public static void addFile(File f) throws IOException {
		addURL(f.toURL());
	}//end method
 
	public static void addURL(URL u) throws IOException {
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL",parameters);
			method.setAccessible(true);
			method.invoke(sysloader,new Object[]{ u });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}//end try catch
	}//end method
}
