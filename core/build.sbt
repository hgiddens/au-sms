name := "au-sms-core"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.1",
  "com.github.julien-truffaut" %% "monocle-core" % Versions.monocle,
  "com.github.julien-truffaut" %% "monocle-macro" % Versions.monocle,
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.1.7" % Test,
  "org.specs2" %% "specs2-matcher-extra" % Versions.specs2 % Test,
  "org.specs2" %% "specs2-scalacheck" % Versions.specs2 % Test
)

publishArtifact in Test := true
