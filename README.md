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
| JDK   | `.java-version`               |
| Scala | `scalaVersion` in `build.sbt` |
| sbt   | `project/build.properties`    |

`.java-version` is read by CI through `actions/setup-java`, and by mise, asdf and jenv, so the
pipeline and your machine run the same JDK. `mise.toml` enables mise's idiomatic version-file
support for Java, which is how mise picks the JDK up without restating it:

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
