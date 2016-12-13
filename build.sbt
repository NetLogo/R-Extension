import org.nlogo.build.NetLogoExtension

enablePlugins(NetLogoExtension)

scalaVersion := "2.12.1"

netLogoExtName := "r"
netLogoVersion := "6.0.0-BETA2"
netLogoClassManager := "org.nlogo.extension.r.Entry"
netLogoZipSources := false

netLogoTarget :=
  NetLogoExtension.directoryTarget(baseDirectory.value)

javacOptions ++=
  "-target 1.8 -source 1.8 -g -deprecation -Xlint:all -Xlint:-serial -Xlint:-fallthrough -encoding us-ascii -Xlint:-path -Werror"
    .split(" ").toSeq

def cclArtifacts(path: String): String =
  s"http://ccl-artifacts.s3-website-us-east-1.amazonaws.com/$path"

libraryDependencies ++= Seq(
  "jri" % "jri"        % "0.9-8" % "provided" from cclArtifacts("JRI.jar"),
  "jri" % "jri-engine" % "0.9-8" % "provided" from cclArtifacts("JRIEngine.jar"),
  "jri" % "r-engine"   % "0.9-8" % "provided" from cclArtifacts("REngine.jar"),
  "javagd" %  "javagd" % "0.6-1" % "provided" from cclArtifacts("javaGD.jar")
)
