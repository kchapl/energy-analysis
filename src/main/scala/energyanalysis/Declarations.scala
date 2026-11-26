package energyanalysis

import energyanalysis.Toolchain.{ Declared, Tool, Version }

import java.nio.file.{ Files, Paths }
import scala.util.matching.Regex

/**
 * Where each declared version lives, and how to read it out of the file that holds it.
 *
 * [[Toolchain]] states the policy and turns declared versions into verdicts. This module owns the
 * other half: the file names, the three formats, and the one place a declaration has to be edited.
 * Its interface is two functions — [[read]] for a checkout, and [[from]] for text a caller supplies
 * — and [[from]] is the seam tests cross without a filesystem.
 */
object Declarations:

  /** Supplies a file's text by name, or says why it cannot. */
  type Texts = String => Either[String, String]

  private val ScalaVersion = """scalaVersion\s*:=\s*"([^"]+)"""".r
  private val SbtVersion   = """sbt\.version\s*=\s*(\S+)""".r
  private val MiseJava     = """(?m)^\s*java\s*=\s*"([^"]+)"""".r

  /** A file that declares one version, and the function that reads it out of that file's text. */
  final private case class Source(file: String, extract: String => Option[String])

  /**
   * The row for one tool. A match rather than a map, so a new [[Toolchain.Tool]] fails to compile
   * until it says where its version is declared.
   */
  private def source(tool: Tool): Source = tool match
    case Tool.Jdk   => Source("mise.toml", text => first(MiseJava, text).flatMap(withoutVendor))
    case Tool.Scala => Source("build.sbt", text => first(ScalaVersion, text))
    case Tool.Sbt   => Source("project/build.properties", text => first(SbtVersion, text))

  /** Reads every declaration from the checkout rooted at `root`. */
  def read(root: String): Either[String, List[Declared]] =
    from(file => readFile(root, file))

  /**
   * Reads every declaration from the text `texts` supplies, in the order the tools are declared,
   * and stops at the first file that cannot be read or does not hold a version.
   */
  def from(texts: Texts): Either[String, List[Declared]] =
    Tool.values.toList.foldLeft[Either[String, List[Declared]]](Right(Nil)) { (rows, tool) =>
      for
        found    <- rows
        declared <- declaration(texts, tool)
      yield found :+ declared
    }

  private def declaration(texts: Texts, tool: Tool): Either[String, Declared] =
    val row = source(tool)
    for
      text    <- texts(row.file)
      request <- row.extract(text).toRight(s"${row.file} does not declare ${tool.label}")
      version <- Version.parse(request).left.map(error => s"${tool.label}: $error")
    yield Declared(tool, version)

  private def first(pattern: Regex, text: String): Option[String] =
    pattern.findFirstMatchIn(text).map(_.group(1))

  /**
   * Drops the distribution a JDK request may name: mise writes `temurin-25` for a release the
   * policy compares as `25`.
   */
  private def withoutVendor(request: String): Option[String] =
    Some(request.dropWhile(character => !character.isDigit)).filter(_.nonEmpty)

  private def readFile(root: String, file: String): Either[String, String] =
    try Right(Files.readString(Paths.get(root).resolve(file)))
    catch case error: Exception => Left(s"cannot read $file: ${error.getMessage}")
