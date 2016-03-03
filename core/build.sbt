name := "telstra-sms-core"

lazy val monocleVersion = "1.1.1"
lazy val specs2Version = "3.6.6"
libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.1",
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "org.specs2" %% "specs2-core" % specs2Version % Test,
  "org.specs2" %% "specs2-scalacheck" % specs2Version % Test
)
