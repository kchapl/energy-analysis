# Energy analysis

Reads metered electricity and gas readings and reports monthly energy use for a year, and keeps the
toolchain versions it declares honest.

## Language

### Energy

**Reading**:
One metered interval: the energy used, and the times it started and ended.
_Avoid_: sample, record

**Consumption**:
The energy a reading reports, in the unit its meter records — kWh for electricity, cubic metres for
gas.
_Avoid_: usage, amount

**Kwh**:
Energy normalised to kilowatt hours, which is what a report prints for both fuels.
_Avoid_: kilowatt hour

**Month usage**:
The electricity and the gas one report attributes to one calendar month of its year.
_Avoid_: monthly total, bucket

**Report**:
One run over both files for a year: twelve month usages, plus the readings it could not parse.
_Avoid_: output, result set

### Toolchain

**Declaration**:
The one place in the repository where a tool's version is written.
_Avoid_: pin, version file

**Declarations**:
The module that knows which file holds each declaration, and how to read it.
_Avoid_: loader, config reader

**Toolchain policy**:
The rule that the JDK and Scala track the latest LTS release and sbt tracks the latest stable
release.
_Avoid_: version rule, upgrade policy

**Drift**:
The verdict that upstream publishes something newer than a declaration.
_Avoid_: outdated, lag

**Launcher**:
The program that starts the build tool version the build declares; its own version does not decide
what the build runs.
_Avoid_: bootstrap, wrapper
