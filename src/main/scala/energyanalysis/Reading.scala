package energyanalysis

import energyanalysis.Time.{ isSameOrAfter, isSameOrBefore }
import energyanalysis.Kwhs.Kwh

import java.time.{ LocalDate, ZonedDateTime }
import scala.util.Try

sealed trait ReadingError
case class InvalidFieldCount(line: String, count: Int)                extends ReadingError
case class InvalidConsumption(value: String)                          extends ReadingError
case class NegativeConsumption(value: Double)                         extends ReadingError
case class InvalidDateTime(field: String, value: String)              extends ReadingError
case class InvalidTimeRange(start: ZonedDateTime, end: ZonedDateTime) extends ReadingError

case class Reading(consumption: Double, start: ZonedDateTime, end: ZonedDateTime)

object Reading:

  def fromLine(line: String): Either[ReadingError, Reading] =
    for
      fields      <- validateFields(line)
      consumption <- parseConsumption(fields(0))
      start       <- parseDateTime("start", fields(1))
      end         <- parseDateTime("end", fields(2))
      reading     <- validateTimeRange(consumption, start, end)
    yield reading

  private def validateFields(line: String): Either[ReadingError, Array[String]] =
    val fields = line.split(',').map(_.trim)
    Either.cond(
      fields.length == 3,
      fields,
      InvalidFieldCount(line, fields.length)
    )

  private def parseConsumption(value: String): Either[ReadingError, Double] =
    Try(value.toDouble).toEither.left
      .map(_ => InvalidConsumption(value))
      .flatMap(v =>
        if (v >= 0) Right(v)
        else Left(NegativeConsumption(v))
      )

  private def parseDateTime(field: String, value: String): Either[ReadingError, ZonedDateTime] =
    Try(ZonedDateTime.parse(value)).toEither.left.map(_ => InvalidDateTime(field, value))

  private def validateTimeRange(
      consumption: Double,
      start: ZonedDateTime,
      end: ZonedDateTime
  ): Either[ReadingError, Reading] =
    Either.cond(
      !end.isBefore(start),
      Reading(consumption, start, end),
      InvalidTimeRange(start, end)
    )

  def electricityUse(
      readings: List[Either[ReadingError, Reading]]
  )(start: LocalDate, end: LocalDate): Kwh =
    readings
      .collect { case Right(reading) =>
        reading
      }
      .filter(reading => isSameOrAfter(start)(reading.start.toLocalDate))
      .filter(reading => isSameOrBefore(end)(reading.start.toLocalDate))
      .foldLeft(Kwh(0))((acc, reading) => acc + Kwh(reading.consumption))

  def gasUse(readings: List[Either[ReadingError, Reading]])(start: LocalDate, end: LocalDate): Kwh =
    Kwh.fromCubicMetresGas(
      readings
        .collect { case Right(reading) =>
          reading
        }
        .filter(reading => isSameOrAfter(start)(reading.start.toLocalDate))
        .filter(reading => isSameOrBefore(end)(reading.start.toLocalDate))
        .foldLeft(0d)((acc, reading) => acc + reading.consumption)
    )
