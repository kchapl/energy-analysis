package energyanalysis

import scala.annotation.tailrec

object Number {

  def round(d: Double, numSigFigs: Int): Double = {
    if (d == 0.0) return 0.0
    if (!d.isFinite) return d
    if (numSigFigs <= 0)
      throw new IllegalArgumentException("Number of significant figures must be positive")

    // Convert to scientific notation with the correct precision
    val formatted = String.format("%." + (numSigFigs - 1) + "e", d)

    // Parse back to a double
    formatted.toDouble
  }
}
