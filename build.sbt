lazy val root = project
  .in(file("."))
  .settings(
    name         := "energy-analysis",
    scalaVersion := "3.8.3",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"     % "1.3.0"  % Test,
      "org.scalatest" %% "scalatest" % "3.2.20" % Test
    )
  )
