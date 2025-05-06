package energyanalysis

import org.scalatest.funsuite.AnyFunSuite
import java.time.{ LocalDate, YearMonth }

class TimeTest extends AnyFunSuite {
  val referenceDate = LocalDate.of(2023, 6, 15)

  test("isSameOrBefore should work correctly") {
    assert(Time.isSameOrBefore(referenceDate)(referenceDate))
    assert(Time.isSameOrBefore(referenceDate)(referenceDate.minusDays(1)))
    assert(!Time.isSameOrBefore(referenceDate)(referenceDate.plusDays(1)))
  }

  test("isSameOrAfter should work correctly") {
    assert(Time.isSameOrAfter(referenceDate)(referenceDate))
    assert(Time.isSameOrAfter(referenceDate)(referenceDate.plusDays(1)))
    assert(!Time.isSameOrAfter(referenceDate)(referenceDate.minusDays(1)))
  }

  test("monthsInYear should return all months") {
    val months = Time.monthsInYear(2023)
    assert(months.size == 12)
    assert(months.head == YearMonth.of(2023, 1))
    assert(months.last == YearMonth.of(2023, 12))
  }
}
