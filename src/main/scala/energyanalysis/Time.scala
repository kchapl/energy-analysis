package energyanalysis

import java.time.{ LocalDate, YearMonth }

object Time {

  def isSameOrBefore(ref: LocalDate)(date: LocalDate): Boolean =
    date == ref || date.isBefore(ref)

  def isSameOrAfter(ref: LocalDate)(date: LocalDate): Boolean =
    date == ref || date.isAfter(ref)

  def monthsInYear(year: Int): Seq[YearMonth] =
    for (month <- 1 to 12) yield YearMonth.of(year, month)
}
