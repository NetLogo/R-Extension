package org.nlogo.extension.r

import java.io.File
import java.nio.file.Paths
import java.util.Properties
import org.scalatest.FunSuite
import scala.collection.JavaConverters._

class ConfigurationTests extends FunSuite {
  if (System.getProperty("os.name").toLowerCase.startsWith("mac")) {
    test("finds mac.properties") {
      val extensionProperties = Configuration.getRExtensionProperties()
      assertResult("normal")(extensionProperties.getProperty("extension.mode"))
    }
  }

  if (System.getProperty("os.name").toLowerCase.startsWith("windows")) {
    test("pathsFromString doesn't return invalid paths") {
      assert(Configuration.pathsFromString("|||").isEmpty)
    }
    test("finds windows.properties") {
      val extensionProperties = Configuration.getRExtensionProperties()
      assert(extensionProperties.getProperty("r.home").nonEmpty)
    }
  }

  test("pathsFromString returns an empty Iterable when passed null") {
    assert(Configuration.pathsFromString(null).isEmpty)
  }

  test("pathsFromString returns an empty Iterable when passed an empty string") {
    assert(Configuration.pathsFromString("").isEmpty)
  }

  test("pathsFromString returns a single valid path") {
    assertResult(Seq(Paths.get("foo")))(Configuration.pathsFromString("foo").asScala)
  }

  test("pathsFromString returns two valid paths") {
    assertResult(Seq(Paths.get("foo"), Paths.get("bar")))(Configuration.pathsFromString(s"foo${File.pathSeparator}bar").asScala)
  }

  test("pathsFromString doesn't return empty paths") {
    assert(Configuration.pathsFromString(File.pathSeparator).isEmpty)
  }

  test("pathsFromString interpolates ~ to user.home") {
    val home = System.getProperty("user.home")
    assertResult(Seq(Paths.get(s"$home${File.separator}foo")))(Configuration.pathsFromString(s"~${File.separator}foo").asScala)
  }

  test("Configuration lists JRI paths") {
    val props = new Properties()
    props.setProperty("jri.home.paths", s"foo/bar${File.pathSeparator}baz/qux")
    val config = new Configuration(props)
    assertResult(Seq(Paths.get("foo/bar"), Paths.get("baz/qux")))(config.jriHomePaths.asScala)
  }

  test("Configuration lists r home path") {
    val props = new Properties()
    props.setProperty("r.home", s"foo/bar")
    val config = new Configuration(props)
    assertResult(Paths.get("foo/bar"))(config.rHomePath)
  }

  test("Configuration lists empty r lib paths") {
    val props = new Properties()
    props.setProperty("r.lib.paths", "")
    val config = new Configuration(props)
    assert(config.rLibPaths.isEmpty)
  }

  test("Configuration lists r lib paths") {
    val props = new Properties()
    props.setProperty("r.lib.paths", s"foo/bar:~/R/win-library/3.3")
    val config = new Configuration(props)
    assertResult(Seq(Paths.get("foo/bar"), Paths.get(System.getProperty("user.home") + s"${File.separator}/R/win-library/3.3")))(config.rLibPaths.asScala)
  }

  test("Configuration has a selected JRI path") {
    val config = new Configuration(new Properties())
    assertResult(java.util.Optional.empty())(config.selectedJRIPath)
    config.selectJRIPath(Paths.get("foo/bar"))
    assertResult(java.util.Optional.of(Paths.get("foo/bar")))(config.selectedJRIPath)
  }

  test("the rpath primitive returns the r path") {
    val entry = new Entry()
    entry.runOnce(new org.nlogo.api.DummyExtensionManager())
    val p = new entry.RPath()
    assertResult(Configuration.fromRExtensionProperties.rHomePath.toString)(p.get())
  }

  test("the jri path primitive returns the selected jri path") {
    val entry = new Entry()
    entry.runOnce(new org.nlogo.api.DummyExtensionManager() {
      override def retrieveObject: AnyRef = null
    })
    val p = new entry.JRIPath()
    assert(p.get().endsWith("jri"))
  }
}
