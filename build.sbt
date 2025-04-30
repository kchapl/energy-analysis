lazy val root = project
  .in(file("."))
  .settings(
    name                                   := "energy-analysis",
    scalaVersion                           := "3.6.4",
    libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test
  )
