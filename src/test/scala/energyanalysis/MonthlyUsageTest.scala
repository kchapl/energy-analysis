package energyanalysis

import org.scalatest.funsuite.AnyFunSuite

import java.time.{ Year, YearMonth }

class MonthlyUsageTest extends AnyFunSuite {

  private val header      = "consumption,start,end"
  private val juneReading = "10.0,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"
  private val malformed   = "invalid,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"

  private def reportFor(electricityLines: List[String], gasLines: List[String], year: Int = 2023) =
    MonthlyUsage.report(electricityLines, gasLines, year)

  private def month(report: Report, month: Int): MonthUsage =
    report.months.find(_.month == YearMonth.of(2023, month)).get

  test("report should return one month usage per month of the year") {
    val report = reportFor(List(header), List(header)).toOption.get

    assert(report.months.map(_.month) == (1 to 12).map(YearMonth.of(2023, _)).toList)
  }

  test("report should total each fuel in the month its readings fall in") {
    val gas    = "2.0,2023-07-15T10:00:00+01:00,2023-07-15T11:00:00+01:00"
    val report = reportFor(List(header, juneReading), List(header, gas)).toOption.get

    assert(month(report, 6).electricity == Kwhs.Kwh(10.0))
    assert(month(report, 6).gas == Kwhs.Kwh.fromCubicMetresGas(0.0))
    assert(month(report, 7).electricity == Kwhs.Kwh(0.0))
    assert(month(report, 7).gas == Kwhs.Kwh.fromCubicMetresGas(2.0))
  }

  test("report should skip a malformed line and report it as a problem") {
    val report = reportFor(List(header, juneReading, malformed), List(header)).toOption.get

    assert(month(report, 6).electricity == Kwhs.Kwh(10.0))
    assert(report.problems.size == 1)
    assert(report.problems.head.source == "electricity")
    assert(report.problems.head.error.isInstanceOf[InvalidConsumption])
  }

  test("report should not treat the header line as a problem") {
    val report = reportFor(List(header), List(header)).toOption.get

    assert(report.problems.isEmpty)
  }

  test("report should return InvalidYear for a year java.time cannot represent") {
    val year = Year.MAX_VALUE + 1

    assert(reportFor(List(header), List(header), year) == Left(InvalidYear(year)))
  }

  test("report should return twelve zero month usages for header-only files") {
    val report = reportFor(List(header), List(header)).toOption.get

    assert(report.months.forall(usage => usage.electricity == Kwhs.Kwh(0.0)))
    assert(report.months.forall(_.gas == Kwhs.Kwh.fromCubicMetresGas(0.0)))
  }
}
