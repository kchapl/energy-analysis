package energyanalysis

import org.scalatest.funsuite.AnyFunSuite

class KwhsTest extends AnyFunSuite {
  test("Kwh addition should work correctly") {
    val kwh1 = Kwhs.Kwh(10.0)
    val kwh2 = Kwhs.Kwh(20.0)
    assert((kwh1 + kwh2) == Kwhs.Kwh(30.0))
  }

  test("Kwh rounding should work correctly") {
    val kwh = Kwhs.Kwh(123.456789)
    assert(kwh.round == Kwhs.Kwh(123.5))
  }

  test("fromCubicMetresGas should convert correctly") {
    val cubicMetres = 10.0
    val kwh         = Kwhs.Kwh.fromCubicMetresGas(cubicMetres)
    assert(kwh == Kwhs.Kwh(10.0))
  }
}
