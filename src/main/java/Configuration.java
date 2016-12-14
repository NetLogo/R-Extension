package org.nlogo.extension.r;

/*
This file is part of NetLogo-R-Extension.
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.nlogo.workspace.ExtensionManager$;

public class Configuration {

  /**
   * pathsFromString takes a pathSeparator-separated string and returns a collection of paths
   * representing each path in the string. Interpolates "~" to user.home.
   */
  static List<Path> pathsFromString(String pathString) {
    if (pathString == null || pathString.isEmpty()) return new ArrayList<Path>();
    else {
      String pathComponents[] = pathString.split(File.pathSeparator);
      ArrayList<Path> paths = new ArrayList<Path>(pathComponents.length);
      String home = System.getProperty("user.home");
      for (String path : pathComponents) {
        try {
          paths.add(Paths.get(path.replace("~", home)));
        } catch (InvalidPathException ex) {
        }
      }
      return paths;
    }
  }

  /** Method to get the properties of the R extension */
  static Properties getRExtensionProperties() {
    Properties extensionProperties = new Properties();
    String osName = System.getProperty("os.name").toLowerCase();

    String propertiesFileName = null;
    if (osName.startsWith("mac", 0)) {
      propertiesFileName = "mac.properties";
    } else if (osName.startsWith("windows", 0)) {
      propertiesFileName = "windows.properties";
    } else if (osName.startsWith("linux", 0)) {
      propertiesFileName = "linux.properties";
    }

    if (propertiesFileName != null) {
      InputStream propertiesStream =
          Entry.class.getClassLoader().getResourceAsStream(propertiesFileName);
      try {
        extensionProperties.load(propertiesStream);
      } catch (IOException e) {
        System.err.println("Error reading default properties from file");
      } catch (IllegalArgumentException e) {
        System.err.println("Illegal character in default properties file");
      }

      // check the R extension directory for user properties
      Path userPropertiesPath =
          Paths.get(ExtensionManager$.MODULE$.extensionPath(), "r", "user.properties");
      if (Files.exists(userPropertiesPath)) {
        try {
          BufferedReader userPropertiesReader = Files.newBufferedReader(userPropertiesPath);
          Properties userProperties = new Properties(extensionProperties);
          userProperties.load(userPropertiesReader);
          extensionProperties = userProperties;
        } catch (IOException e) {
          System.err.println("Error reading user properties file");
        } catch (IllegalArgumentException e) {
          System.err.println("Illegal character in user properties file");
        } catch (SecurityException e) {
          System.err.println("Security exception while reading user properties file");
        }
      }
    }
    return extensionProperties;
  }

  public static Configuration fromRExtensionProperties() {
    return new Configuration(getRExtensionProperties());
  }

  private final Properties properties;
  private Path rHomePath;
  private Optional<Path> selectedJRIPath = Optional.empty();

  public Configuration(Properties properties) {
    this.properties = properties;
    rHomePath = Paths.get(properties.getProperty("r.home", ""));
  }

  public List<Path> jriHomePaths() {
    return pathsFromString(properties.getProperty("jri.home.paths", ""));
  }

  public Path rHomePath() {
    return rHomePath;
  }

  public void setRHomePath(Path newRHomePath) {
    rHomePath = newRHomePath;
  }

  public String extensionMode() {
    return properties.getProperty("extension.mode", "");
  }

  public Optional<Path> selectedJRIPath() {
    return selectedJRIPath;
  }

  public void selectJRIPath(Path p) {
    selectedJRIPath = Optional.of(p);
  }
}
