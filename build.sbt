lazy val root = project
  .in(file("."))
  .settings(
    name         := "energy-analysis",
    scalaVersion := "3.7.3",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"     % "1.2.0"  % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )
