package energyanalysis

import scala.annotation.tailrec

object Number {

  def round(d: Double, numSigFigs: Int): Double =
    @tailrec
    def go(dbl: Double, multiple: Double): Double =
      if dbl > 10 then go(dbl / 10, multiple * 10)
      else if dbl < 1 then go(dbl * 10, multiple / 10)
      else multiple
    val firstMultiple = go(math.abs(d), 1)
    val secondMultiple = math.pow(10, numSigFigs - 1)
    (d * secondMultiple / firstMultiple).round / secondMultiple * firstMultiple
}
