package energyanalysis

import energyanalysis.Number.*
import energyanalysis.Reading.*
import energyanalysis.Time.*

import java.time.{ LocalDate, ZonedDateTime }
import scala.annotation.tailrec
import scala.io.Source

@main
def main(): Unit =
  val electricityReadings = File.fetch("../../Desktop/EnergyData/electricity.csv").drop(1)
  val gasReadings         = File.fetch("../../Desktop/EnergyData/gas.csv").drop(1)
  val electricityBetween  = electricityUse(electricityReadings.map(Reading.fromLine))
  val gasBetween          = gasUse(gasReadings.map(Reading.fromLine))
  for (month <- monthsInYear(2022))
    val start       = month.atDay(1)
    val end         = month.atEndOfMonth
    val electricity = electricityBetween(start, end)
    val gas         = gasBetween(start, end)
    println(s"$start: $end: ${electricity.round}: ${gas.round}")
