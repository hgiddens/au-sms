name := "au-sms"
organization in ThisBuild := "com.github.hgiddens"

scalaVersion in ThisBuild := "2.11.8"
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-Xlint",
  "-Xlog-free-terms",
  "-Ywarn-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)
scalacOptions in Test in ThisBuild += "-Yrangepos"

libraryDependencies in ThisBuild += compilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
dependencyOverrides in ThisBuild ++= Set(
  "org.log4s" %% "log4s" % Versions.log4s,
  "org.scala-lang" % "scala-library" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalaz" %% "scalaz-core" % Versions.scalaz
)

autoAPIMappings in ThisBuild := true
homepage in ThisBuild := Some(url("https://github.com/hgiddens/au-sms"))
licenses in ThisBuild := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

lazy val core = project
lazy val telstraClient = project.
  in(file("telstra-client")).
  dependsOn(core % "compile->compile;test->test")
lazy val smsCentralClient = project.
  in(file("smscentral-client")).
  dependsOn(core % "compile->compile;test->test")
