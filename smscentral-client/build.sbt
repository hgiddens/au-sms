name := "au-sms-smscentral"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-argonaut" % Versions.http4s,
  "org.http4s" %% "http4s-client" % Versions.http4s,
  "org.http4s" %% "http4s-dsl" % Versions.http4s % Test,
  "org.http4s" %% "http4s-scala-xml" % Versions.http4s,
  "org.log4s" %% "log4s" % Versions.log4s,
  "org.slf4j" % "slf4j-simple" % "1.7.18" % Test
)
