package energyanalysis

import energyanalysis.Kwhs.Kwh
import energyanalysis.Reading.{ electricityUse, gasUse }
import energyanalysis.Time.monthsInYear

import java.time.{ Year, YearMonth }

sealed trait ReportError

/** A year outside the range `java.time` can represent. */
case class InvalidYear(year: Int) extends ReportError

case class MonthUsage(month: YearMonth, electricity: Kwh, gas: Kwh)

/** A reading the report could not parse, and the fuel it came from. */
case class Problem(source: String, error: ReadingError)

case class Report(months: List[MonthUsage], problems: List[Problem])

object MonthlyUsage:

  def validateYear(year: Int): Either[ReportError, Unit] =
    Either.cond(year >= Year.MIN_VALUE && year <= Year.MAX_VALUE, (), InvalidYear(year))

  /** One month usage per month of `year`, from raw file lines that start with a header. */
  def report(
      electricityLines: List[String],
      gasLines: List[String],
      year: Int
  ): Either[ReportError, Report] =
    validateYear(year).map { _ =>
      val electricity = parse("electricity", electricityLines)
      val gas         = parse("gas", gasLines)

      val months = monthsInYear(year).toList.map { month =>
        val start = month.atDay(1)
        val end   = month.atEndOfMonth
        MonthUsage(
          month,
          electricityUse(electricity.readings)(start, end),
          gasUse(gas.readings)(start, end)
        )
      }

      Report(months, electricity.problems ++ gas.problems)
    }

  private case class Parsed(readings: List[Either[ReadingError, Reading]], problems: List[Problem])

  private def parse(source: String, lines: List[String]): Parsed =
    val readings = lines.drop(1).map(Reading.fromLine)
    Parsed(readings, readings.collect { case Left(error) => Problem(source, error) })
