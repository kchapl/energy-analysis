val scala3Version = "3.5.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "energy-analysis",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test
  )
