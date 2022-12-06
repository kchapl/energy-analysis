package energyanalysis

import scala.io.Source

object File:

  def fetch(path: String): List[String] =
    val src = Source.fromFile(path)
    val lines = src.getLines.toList
    src.close()
    lines
