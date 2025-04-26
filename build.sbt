import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(ExtensionDocumentationPlugin, NetLogoExtension)

version := "1.2.4"
isSnapshot := true

scalaVersion := "2.13.16"

netLogoExtName := "r"
netLogoVersion := "7.0.0-internal1"
netLogoClassManager := "org.nlogo.extension.r.Entry"
netLogoPackageExtras ++=
  Seq(
    (baseDirectory.value / "dist" / "user.properties"  -> Some("user.properties"))
  , (baseDirectory.value / "dist" / "GPL.txt"          -> Some("GPL.txt"))
  , (baseDirectory.value / "examples" / "rfunction1.r" -> Some("models/rfunction1.r"))
  )

netLogoPackageExtras ++= ((baseDirectory.value / "examples") ** "*.nlogo").get.map(f => f -> Some(s"models/${f.getName}"))

javacOptions ++=
  "--release 11 -g -deprecation -Xlint:all -Xlint:-serial -Xlint:-fallthrough -encoding us-ascii -Xlint:-path -Werror"
    .split(" ").toSeq

def cclArtifacts(path: String): String =
  s"https://s3.amazonaws.com/ccl-artifacts/$path"

libraryDependencies ++= Seq(
  "jri"              % "jri"        % "0.9-8" % "provided" from cclArtifacts("JRI.jar"),
  "jri"              % "jri-engine" % "0.9-8" % "provided" from cclArtifacts("JRIEngine.jar"),
  "jri"              % "r-engine"   % "0.9-8" % "provided" from cclArtifacts("REngine.jar"),
  "javagd"           %  "javagd"    % "0.6-1" % "provided" from cclArtifacts("javaGD.jar"),
  "net.java.dev.jna" % "jna"        % "4.2.2"
)

Test / fork := true

Test / test / javaOptions += "-Dorg.nlogo.r.extension.msvcr.lib.name=msvcr100"
