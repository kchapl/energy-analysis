val scala3Version = "3.4.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "energy-analysis",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )
