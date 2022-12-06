package energyanalysis

import scala.io.Source
import scala.util.Using

object File:

  def fetch(path: String): List[String] =
    Using(Source.fromFile(path))(_.getLines.toList).get
