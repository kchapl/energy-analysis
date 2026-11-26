package energyanalysis

import java.nio.file.{ Files, Paths }

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
      declared  <- Declarations.read(root)
      published <- readSnapshot(snapshot).flatMap(Toolchain.Snapshot.parse)
    yield Toolchain.check(declared, published)

  private def readSnapshot(file: String): Either[String, String] =
    try Right(Files.readString(Paths.get(file)))
    catch case error: Exception => Left(s"cannot read $file: ${error.getMessage}")

  private def option(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case Seq(`name`, value) => value }
