package energyanalysis

object Kwhs:
  opaque type Kwh = Double

  object Kwh:
    def apply(d: Double): Kwh = d
    def fromCubicMetresGas(d: Double): Kwh = d * 11.2 // Standard conversion factor: 1m³ = 11.2 kWh

  extension (usage: Kwh)
    def +(kwh: Kwh): Kwh = usage + kwh
    def round: Kwh       = Number.round(usage, 4)
