package energyanalysis

import scala.io.Source
import scala.util.{Try, Using}
import java.io.{FileNotFoundException, IOException}

sealed trait FileError
case class FileNotFound(path: String) extends FileError
case class SecurityError(path: String) extends FileError
case class IOError(path: String, message: String) extends FileError
case class UnexpectedError(path: String, message: String) extends FileError

object File:
  def fetch(path: String): Either[FileError, List[String]] =
    Try {
      Using(Source.fromFile(path)) { source =>
        source.getLines.toList
      }.get
    }.toEither.left.map {
      case e: FileNotFoundException => FileNotFound(path)
      case e: SecurityException => SecurityError(path)
      case e: IOException => IOError(path, e.getMessage)
      case e => UnexpectedError(path, e.getMessage)
    }
