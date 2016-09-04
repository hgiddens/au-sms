name := "au-sms-telstra"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-jawn" % Versions.circe,
  "org.http4s" %% "http4s-client" % Versions.http4s,
  "org.http4s" %% "http4s-dsl" % Versions.http4s % Test,
  "org.http4s" %% "http4s-jawn" % Versions.http4s,
  "org.log4s" %% "log4s" % Versions.log4s,
  "org.slf4j" % "slf4j-simple" % Versions.slf4j % Test
)
