package energyanalysis

object Kwhs:
  opaque type Kwh = Double

  object Kwh:
    def apply(d: Double): Kwh = d
    // TODO: calculate
    def fromCubicMetresGas(d: Double): Kwh = d

  extension (usage: Kwh)
    def +(kwh: Kwh): Kwh = usage + kwh
    def round: Kwh = Number.round(usage, 4)
