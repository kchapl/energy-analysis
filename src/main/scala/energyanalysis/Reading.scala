package energyanalysis

import energyanalysis.Time.{ isSameOrAfter, isSameOrBefore }
import energyanalysis.Kwhs.Kwh

import java.time.{ LocalDate, ZonedDateTime }

case class Reading(consumption: Double, start: ZonedDateTime, end: ZonedDateTime)

object Reading:

  def fromLine(line: String): Reading =
    val fields = line.split(',').map(_.trim)
    Reading(fields(0).toDouble, ZonedDateTime.parse(fields(1)), ZonedDateTime.parse(fields(2)))

  def electricityUse(readings: List[Reading])(start: LocalDate, end: LocalDate): Kwh =
    readings
      .filter(reading => isSameOrAfter(start)(reading.start.toLocalDate))
      .filter(reading => isSameOrBefore(end)(reading.start.toLocalDate))
      .foldLeft(Kwh(0))((acc, reading) => acc + Kwh(reading.consumption))

  def gasUse(readings: List[Reading])(start: LocalDate, end: LocalDate): Kwh =
    Kwh.fromCubicMetresGas(
      readings
        .filter(reading => isSameOrAfter(start)(reading.start.toLocalDate))
        .filter(reading => isSameOrBefore(end)(reading.start.toLocalDate))
        .foldLeft(0d)((acc, reading) => acc + reading.consumption)
    )
