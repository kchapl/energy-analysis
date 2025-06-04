lazy val root = project
  .in(file("."))
  .settings(
    name         := "energy-analysis",
    scalaVersion := "3.7.1",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"     % "1.1.1"  % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )
