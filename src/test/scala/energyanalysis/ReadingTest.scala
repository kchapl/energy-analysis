package energyanalysis

import org.scalatest.funsuite.AnyFunSuite
import java.time.{LocalDate, ZonedDateTime}

class ReadingTest extends AnyFunSuite {
  test("fromLine should successfully parse valid CSV line") {
    val line = "10.5,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"
    val result = Reading.fromLine(line)
    assert(result.isRight)
    val reading = result.toOption.get
    assert(reading.consumption == 10.5)
    assert(reading.start.toString == "2023-06-15T10:00:00+01:00")
    assert(reading.end.toString == "2023-06-15T11:00:00+01:00")
  }

  test("fromLine should handle invalid field count") {
    val line = "10.5,2023-06-15T10:00:00+01:00"
    val result = Reading.fromLine(line)
    assert(result.isLeft)
    assert(result.left.toOption.get.isInstanceOf[InvalidFieldCount])
  }

  test("fromLine should handle invalid consumption") {
    val line = "invalid,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"
    val result = Reading.fromLine(line)
    assert(result.isLeft)
    assert(result.left.toOption.get.isInstanceOf[InvalidConsumption])
  }

  test("fromLine should handle negative consumption") {
    val line = "-10.5,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"
    val result = Reading.fromLine(line)
    assert(result.isLeft)
    assert(result.left.toOption.get.isInstanceOf[NegativeConsumption])
  }

  test("fromLine should handle invalid datetime") {
    val line = "10.5,invalid-date,2023-06-15T11:00:00+01:00"
    val result = Reading.fromLine(line)
    assert(result.isLeft)
    assert(result.left.toOption.get.isInstanceOf[InvalidDateTime])
  }

  test("fromLine should handle invalid time range") {
    val line = "10.5,2023-06-15T11:00:00+01:00,2023-06-15T10:00:00+01:00"
    val result = Reading.fromLine(line)
    assert(result.isLeft)
    assert(result.left.toOption.get.isInstanceOf[InvalidTimeRange])
  }

  test("electricityUse should sum valid readings in date range") {
    val readings = List(
      Reading.fromLine("10.0,2023-06-14T10:00:00+01:00,2023-06-14T11:00:00+01:00"),
      Reading.fromLine("20.0,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"),
      Reading.fromLine("30.0,2023-06-16T10:00:00+01:00,2023-06-16T11:00:00+01:00")
    )
    
    val result = Reading.electricityUse(readings)(LocalDate.of(2023, 6, 14), LocalDate.of(2023, 6, 15))
    assert(result == Kwhs.Kwh(30.0))
  }

  test("electricityUse should skip invalid readings") {
    val readings = List(
      Reading.fromLine("10.0,2023-06-14T10:00:00+01:00,2023-06-14T11:00:00+01:00"),
      Reading.fromLine("invalid,2023-06-15T10:00:00+01:00,2023-06-15T11:00:00+01:00"),
      Reading.fromLine("30.0,2023-06-16T10:00:00+01:00,2023-06-16T11:00:00+01:00")
    )
    
    val result = Reading.electricityUse(readings)(LocalDate.of(2023, 6, 14), LocalDate.of(2023, 6, 15))
    assert(result == Kwhs.Kwh(10.0))
  }
}
