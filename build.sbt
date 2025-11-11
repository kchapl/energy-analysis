lazy val root = project
  .in(file("."))
  .settings(
    name         := "energy-analysis",
    scalaVersion := "3.7.4",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"     % "1.2.1"  % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )
