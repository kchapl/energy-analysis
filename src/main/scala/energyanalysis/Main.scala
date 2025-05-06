package energyanalysis

import energyanalysis.Number.*
import energyanalysis.Reading.*
import energyanalysis.Time.*

import java.time.{ LocalDate, YearMonth }
import scala.annotation.tailrec

@main
def main(electricityPath: String, gasPath: String, year: Int = YearMonth.now().getYear): Unit =
  def processFile(path: String): Either[FileError, List[String]] =
    File.fetch(path).map(_.drop(1))

  def handleErrors[E](source: String)(result: Either[E, ?]): Unit =
    result match
      case Left(error) => println(s"Error processing $source: $error")
      case Right(_)    => ()

  for
    electricityLines <- processFile(electricityPath)
    gasLines         <- processFile(gasPath)
  do
    val electricityReadings = electricityLines.map(Reading.fromLine)
    val gasReadings         = gasLines.map(Reading.fromLine)

    // Log any parsing errors
    electricityReadings.collect { case Left(error) => handleErrors("electricity")(Left(error)) }
    gasReadings.collect { case Left(error) => handleErrors("gas")(Left(error)) }

    val electricityBetween = electricityUse(electricityReadings)
    val gasBetween         = gasUse(gasReadings)

    for (month <- monthsInYear(year))
      val start       = month.atDay(1)
      val end         = month.atEndOfMonth
      val electricity = electricityBetween(start, end)
      val gas         = gasBetween(start, end)
      println(s"$start: $end: ${electricity.round}: ${gas.round}")
