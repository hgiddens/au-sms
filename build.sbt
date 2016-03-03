name := "telstra-sms"
organization in ThisBuild := "com.github.hgiddens"

scalaVersion in ThisBuild := "2.11.7"
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

autoAPIMappings in ThisBuild := true

lazy val core = project
lazy val http4sClient = project.
  in(file("http4s-client")).
  dependsOn(core % "compile->compile;test->test")
