package energyanalysis

import org.scalatest.funsuite.AnyFunSuite

import energyanalysis.Toolchain.{ Declared, Tool, Version }

class DeclarationsTest extends AnyFunSuite:

  private def version(raw: String): Version =
    Version.parse(raw).fold(error => fail(error), identity)

  private val files = Map(
    "mise.toml"                -> "[tools]\njava = \"temurin-25\"\n",
    "build.sbt"                -> "scalaVersion := \"3.9.0\"\n",
    "project/build.properties" -> "sbt.version=2.1.1\n"
  )

  private def texts(contents: Map[String, String]): Declarations.Texts =
    file => contents.get(file).toRight(s"cannot read $file")

  private def declared(contents: Map[String, String]): Either[String, List[Declared]] =
    Declarations.from(texts(contents))

  test("from reads the three declarations in the order the tools are declared") {
    assert(
      declared(files) == Right(
        List(
          Declared(Tool.Jdk, version("25")),
          Declared(Tool.Scala, version("3.9.0")),
          Declared(Tool.Sbt, version("2.1.1"))
        )
      )
    )
  }

  test("from keeps a jdk request that names no distribution") {
    val noVendor = files.updated("mise.toml", "[tools]\njava = \"25\"\n")

    assert(
      declared(noVendor).map(_.map(_.version)) ==
        Right(List(version("25"), version("3.9.0"), version("2.1.1")))
    )
  }

  test("from names the file and the tool when a file declares no version") {
    val missing = files.updated("project/build.properties", "# no version declared\n")

    assert(
      declared(missing).left.exists(_.contains("project/build.properties does not declare sbt"))
    )
  }

  test("from names the tool when a declared value is not a version") {
    val malformed = files.updated("build.sbt", "scalaVersion := \"next\"\n")

    assert(declared(malformed).left.exists(_.contains("scala:")))
  }

  test("from reports a file it cannot read") {
    val unreadable = files - "mise.toml"

    assert(declared(unreadable).left.exists(_.contains("cannot read mise.toml")))
  }

  test("read finds the declarations the repository itself carries") {
    val declarations = Declarations.read(".").fold(error => fail(error), identity)

    assert(declarations.map(_.tool) == List(Tool.Jdk, Tool.Scala, Tool.Sbt))
  }
