package energyanalysis

import java.nio.file.{ Files, Paths }
import scala.util.matching.Regex

/**
 * Command line edge of the toolchain drift check. Reads the declared versions out of the repository
 * and an upstream snapshot from a file, rather than going to the network itself.
 *
 * {{{
 * sbt "runMain energyanalysis.ToolchainCheck --snapshot toolchain-snapshot.txt"
 * }}}
 *
 * The last line written to stdout is `status: ok`, `status: drift` or `status: fail`, and the exit
 * code follows it: 0 current, 1 a newer version is available, 2 a declared version is outside
 * policy, 3 the inputs could not be read or parsed.
 */
object ToolchainCheck:

  private val ScalaVersion = """scalaVersion\s*:=\s*"([^"]+)"""".r
  private val SbtVersion   = """sbt\.version\s*=\s*(\S+)""".r

  def main(args: Array[String]): Unit =
    val arguments = args.toList
    val root      = option(arguments, "--root").getOrElse(".")

    val outcome = option(arguments, "--snapshot") match
      case None       => Left("missing --snapshot <file>")
      case Some(file) => evaluate(root, file)

    outcome match
      case Left(error) =>
        System.err.println(s"toolchain check: $error")
        println("status: fail")
        sys.exit(3)
      case Right(findings) =>
        findings.foreach(finding => println(finding.message))
        val status =
          if Toolchain.failed(findings) then ("fail", 2)
          else if findings.exists(_.isDrift) then ("drift", 1)
          else ("ok", 0)
        println(s"status: ${status._1}")
        sys.exit(status._2)

  private def evaluate(root: String, snapshot: String): Either[String, List[Toolchain.Finding]] =
    for
      jdk       <- readMise(root)
      scala     <- readMatch(root, "build.sbt", ScalaVersion)
      sbt       <- readMatch(root, "project/build.properties", SbtVersion)
      declared  <- Toolchain.declaredVersions(jdk, scala, sbt)
      published <- read(snapshot).flatMap(Toolchain.Snapshot.parse)
    yield Toolchain.check(declared, published)

  private def readMise(root: String): Either[String, String] =
    read(path(root, "mise.toml")).flatMap(Toolchain.miseJava)

  private def readMatch(root: String, file: String, pattern: Regex): Either[String, String] =
    read(path(root, file)).flatMap { text =>
      pattern
        .findFirstMatchIn(text)
        .map(found => found.group(1))
        .toRight(s"$file does not declare a version")
    }

  private def read(file: String): Either[String, String] =
    try Right(Files.readString(Paths.get(file)))
    catch case error: Exception => Left(s"cannot read $file: ${error.getMessage}")

  private def path(root: String, file: String): String = Paths.get(root).resolve(file).toString

  private def option(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case Seq(`name`, value) => value }
