package energyanalysis

import org.scalatest.funsuite.AnyFunSuite

class NumberTest extends AnyFunSuite {
  test("round should handle regular numbers") {
    assert(Number.round(123.456, 3) == 123.0)
    assert(Number.round(1234.56, 3) == 1230.0)
    assert(Number.round(12.3456, 3) == 12.3)
  }

  test("round should handle small numbers") {
    assert(Number.round(0.123456, 3) == 0.123)
    assert(Number.round(0.0123456, 3) == 0.0123)
  }

  test("round should handle negative numbers") {
    assert(Number.round(-123.456, 3) == -123.0)
    assert(Number.round(-0.123456, 3) == -0.123)
  }
}
