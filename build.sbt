import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(ExtensionDocumentationPlugin, NetLogoExtension)

scalaVersion := "2.12.12"

version := "1.2.3"

resolvers += "netlogo" at "https://dl.cloudsmith.io/public/netlogo/netlogo/maven/"

netLogoExtName := "r"
netLogoVersion := "6.2.0-d27b502"
netLogoClassManager := "org.nlogo.extension.r.Entry"
netLogoZipSources := false
netLogoPackageExtras ++=
  Seq(
    (baseDirectory.value / "dist" / "user.properties") -> "user.properties",
    (baseDirectory.value / "dist" / "GPL.txt")         -> "GPL.txt")

netLogoPackageExtras ++=
  ((baseDirectory.value / "examples") ** "*.nlogo").get.map(f => f -> s"models/${f.getName}") :+
    (baseDirectory.value / "examples" / "rfunction1.r" -> "models/rfunction1.r")

netLogoTarget :=
  NetLogoExtension.directoryTarget(baseDirectory.value)

javacOptions ++=
  "-target 1.8 -source 1.8 -g -deprecation -Xlint:all -Xlint:-serial -Xlint:-fallthrough -encoding us-ascii -Xlint:-path -Werror"
    .split(" ").toSeq

def cclArtifacts(path: String): String =
  s"https://s3.amazonaws.com/ccl-artifacts/$path"

libraryDependencies ++= Seq(
  "jri" % "jri"        % "0.9-8" % "provided" from cclArtifacts("JRI.jar"),
  "jri" % "jri-engine" % "0.9-8" % "provided" from cclArtifacts("JRIEngine.jar"),
  "jri" % "r-engine"   % "0.9-8" % "provided" from cclArtifacts("REngine.jar"),
  "javagd" %  "javagd" % "0.6-1" % "provided" from cclArtifacts("javaGD.jar"),
  "net.java.dev.jna" % "jna" % "4.2.2",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

fork in Test := true

javaOptions in (test in Test) += "-Dorg.nlogo.r.extension.msvcr.lib.name=msvcr100"
isSnapshot := true
