package energyanalysis

import energyanalysis.Number.*
import energyanalysis.Reading.*
import energyanalysis.Time.*

import java.time.{LocalDate, ZonedDateTime}
import scala.annotation.tailrec
import scala.io.Source

@main
def main(): Unit =
  val electricityReadings = File.fetch("../../Desktop/EnergyData/electricity.csv").drop(1)
  val gasReadings = File.fetch("../../Desktop/EnergyData/gas.csv").drop(1)
  val date1 = LocalDate.of(2022, 11, 1)
  val date2 = LocalDate.of(2022, 11, 30)
  val electricityBetween = electricityUse(electricityReadings.map(Reading.fromLine))
  val gasBetween = gasUse(gasReadings.map(Reading.fromLine))
  val electricity = electricityBetween(date1, date2)
  val gas = gasBetween(date1, date2)
  println(s"$date1: $date2: ${electricity.round}: ${gas.round}")
