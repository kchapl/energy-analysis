package energyanalysis

import java.time.LocalDate

object Time {

  def isSameOrBefore(ref: LocalDate)(date: LocalDate): Boolean =
    date == ref || date.isBefore(ref)

  def isSameOrAfter(ref: LocalDate)(date: LocalDate): Boolean =
    date == ref || date.isAfter(ref)
}
