[![CI](https://github.com/kchapl/energy-analysis/actions/workflows/ci.yaml/badge.svg)](https://github.com/kchapl/energy-analysis/actions/workflows/ci.yaml)

## Energy analysis

## Toolchain policy

The JDK and Scala track the latest LTS release; sbt tracks the latest stable release.

That rule is stated once, as data: `Toolchain.policy` in
`src/main/scala/energyanalysis/Toolchain.scala`. This section deliberately does not restate which
versions it allows, so the documented rule and the enforced rule cannot drift apart.

Each version is declared in exactly one place:

| Tool  | Declared in                   |
| ----- | ----------------------------- |
| JDK   | `java` in `mise.toml`         |
| Scala | `scalaVersion` in `build.sbt` |
| sbt   | `project/build.properties`    |

`mise.toml` names the JDK as distribution and release, `java = "temurin-25"`, so CI and a
developer machine resolve the same build; the `sbt` entry beside it installs the launcher only,
because `project/build.properties` decides the sbt the build runs. CI installs those tools with
`jdx/mise-action`, so the pipeline reads the same file as your machine:

```
mise install
```

The scheduled `Toolchain drift` workflow compares those declarations against what upstream
publishes and reports drift. It runs on a schedule and on demand, not as a pull-request check, so
a stale version never blocks an unrelated change. To run it yourself, pass a snapshot instead of
going to the network:

```
printf 'jdk latest=27.0.1 lts=21.0.13,25.0.5\nscala latest=3.10.1 lts=3.3.9,3.9.0\nsbt latest=2.1.1\n' \
  > toolchain-snapshot.txt
sbt "runMain energyanalysis.ToolchainCheck --snapshot toolchain-snapshot.txt"
```

It prints one line per tool, then `status: ok`, `status: drift` or `status: fail`, and exits 0, 1,
2 or 3 to match, so a script can branch on either the status line or the exit code.
