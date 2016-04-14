name := "au-sms-core"

libraryDependencies ++= Seq(
  "com.github.julien-truffaut" %% "monocle-core" % Versions.monocle,
  "com.github.julien-truffaut" %% "monocle-macro" % Versions.monocle,
  "io.circe" %% "circe-core" % Versions.circe,
  "org.specs2" %% "specs2-matcher-extra" % Versions.specs2 % Test,
  "org.specs2" %% "specs2-scalacheck" % Versions.specs2 % Test
)

publishArtifact in Test := true
