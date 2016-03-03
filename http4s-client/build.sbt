name := "telstra-sms-http4s"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-argonaut" % Versions.http4s,
  "org.http4s" %% "http4s-client" % Versions.http4s,
  "org.http4s" %% "http4s-dsl" % Versions.http4s % Test,
  "org.log4s" %% "log4s" % "1.1.5",
  "org.slf4j" % "slf4j-simple" % "1.7.18" % Test,
  "org.specs2" %% "specs2-matcher-extra" % Versions.specs2 % Test
)