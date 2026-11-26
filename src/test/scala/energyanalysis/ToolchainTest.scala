package energyanalysis

import org.scalatest.funsuite.AnyFunSuite

import energyanalysis.Toolchain.*

class ToolchainTest extends AnyFunSuite {

  private def version(raw: String): Version =
    Version.parse(raw).fold(error => fail(error), identity)

  private val published = Snapshot(
    Map(
      Tool.Jdk   -> Upstream(version("27.0.1"), List(version("21.0.13"), version("25.0.5"))),
      Tool.Scala -> Upstream(version("3.10.1"), List(version("3.3.9"), version("3.9.0"))),
      Tool.Sbt   -> Upstream(version("2.1.1"), Nil)
    )
  )

  private def verdict(tool: Tool, raw: String): Verdict =
    check(List(Declared(tool, version(raw))), published).head.verdict

  test("a tool on the latest LTS line is current") {
    assert(verdict(Tool.Scala, "3.9.0") == Verdict.Current)
    assert(verdict(Tool.Jdk, "25") == Verdict.Current)
  }

  test("a tool one patch behind on the latest LTS line is behind") {
    val onePatchLater = Snapshot(
      published.entries.updated(
        Tool.Scala,
        Upstream(version("3.9.1"), List(version("3.3.9"), version("3.9.1")))
      )
    )
    val finding = check(List(Declared(Tool.Scala, version("3.9.0"))), onePatchLater).head

    assert(finding.verdict == Verdict.Behind(version("3.9.1"), major = false))
    assert(finding.isDrift)
    assert(!finding.isFailure)
  }

  test("a tool on a previous LTS line that is still supported is behind, not outside policy") {
    val finding = check(List(Declared(Tool.Scala, version("3.3.9"))), published).head

    assert(finding.verdict == Verdict.Behind(version("3.9.0"), major = false))
    assert(!finding.isFailure)
  }

  test("a non-LTS line that is numerically newer than an LTS line is outside policy") {
    val finding = check(List(Declared(Tool.Scala, version("3.8.4"))), published).head

    assert(finding.verdict == Verdict.OutsidePolicy(Reason.NotLts, Some(version("3.9.0"))))
    assert(finding.isFailure)
  }

  test("a line that has ended is outside policy") {
    val finding = check(List(Declared(Tool.Scala, version("3.7.4"))), published).head

    assert(finding.verdict == Verdict.OutsidePolicy(Reason.NotLts, Some(version("3.9.0"))))
  }

  test("the JDK is compared by feature release, so a lower patch is still current") {
    assert(verdict(Tool.Jdk, "25") == Verdict.Current)
    assert(verdict(Tool.Jdk, "25.0.1") == Verdict.Current)
  }

  test("the JDK on the previous LTS is behind") {
    val finding = check(List(Declared(Tool.Jdk, version("21"))), published).head

    assert(finding.verdict == Verdict.Behind(version("25"), major = false))
  }

  test("sbt patch updates are behind, not a failure") {
    val finding = check(List(Declared(Tool.Sbt, version("2.0.9"))), published).head

    assert(finding.verdict == Verdict.Behind(version("2.1.1"), major = false))
    assert(!finding.isFailure)
  }

  test("sbt major updates are surfaced as a major upgrade") {
    val finding = check(List(Declared(Tool.Sbt, version("1.12.11"))), published).head

    assert(finding.verdict == Verdict.Behind(version("2.1.1"), major = true))
    assert(finding.message.contains("major update available"))
  }

  test("sbt on the latest release is current") {
    assert(verdict(Tool.Sbt, "2.1.1") == Verdict.Current)
  }

  test("a declaration newer than upstream reports is outside policy") {
    val finding = check(List(Declared(Tool.Sbt, version("2.2.0"))), published).head

    assert(finding.verdict == Verdict.OutsidePolicy(Reason.AheadOfUpstream, Some(version("2.1.1"))))
  }

  test("a tool missing from the upstream snapshot is outside policy") {
    val finding = check(List(Declared(Tool.Sbt, version("2.1.1"))), Snapshot(Map.empty)).head

    assert(finding.verdict == Verdict.OutsidePolicy(Reason.NoUpstreamData, None))
  }

  test(
    "a verdict message names the tool, the declared version, the expected version and the policy"
  ) {
    val message = check(List(Declared(Tool.Scala, version("3.8.4"))), published).head.message

    assert(message.startsWith("scala: declared 3.8.4"))
    assert(message.contains("expected 3.9.0"))
    assert(message.contains("latest LTS"))
  }

  test("check reports one finding per declared tool, in declaration order") {
    val declared = List(
      Declared(Tool.Jdk, version("25")),
      Declared(Tool.Scala, version("3.9.0")),
      Declared(Tool.Sbt, version("1.12.11"))
    )
    val findings = check(declared, published)

    assert(findings.map(_.tool) == List(Tool.Jdk, Tool.Scala, Tool.Sbt))
    assert(!failed(findings))
    assert(findings.count(_.isDrift) == 1)
  }

  test("failed is true only when a declared version is outside policy") {
    assert(
      !failed(List(Finding(Tool.Sbt, version("2.0.9"), Verdict.Behind(version("2.1.1"), false))))
    )
    assert(
      failed(
        List(Finding(Tool.Scala, version("3.8.4"), Verdict.OutsidePolicy(Reason.NotLts, None)))
      )
    )
  }

  test("report prints one line per finding") {
    val findings = check(
      List(Declared(Tool.Jdk, version("25")), Declared(Tool.Sbt, version("1.12.11"))),
      published
    )

    assert(report(findings).linesIterator.size == 2)
  }

  test("Version parses major-only, full and trailing-component values") {
    assert(Version.parse("25") == Right(Version(25, 0, 0)))
    assert(Version.parse("3.10.1") == Right(Version(3, 10, 1)))
    assert(Version.parse("25.0.5.1+2") == Right(Version(25, 0, 5)))
  }

  test("Version rejects values that do not start with a number") {
    assert(Version.parse("v2.1.1").isLeft)
    assert(Version.parse("").isLeft)
  }

  test("Snapshot parses the published format, ignoring comments and blank lines") {
    val text =
      """# captured upstream
        |jdk   latest=27.0.1 lts=21.0.13,25.0.5
        |
        |scala latest=3.10.1 lts=3.3.9,3.9.0
        |sbt   latest=2.1.1
        |""".stripMargin

    val snapshot = Snapshot.parse(text).fold(error => fail(error), identity)

    assert(
      snapshot.forTool(Tool.Jdk) == Some(
        Upstream(version("27.0.1"), List(version("21.0.13"), version("25.0.5")))
      )
    )
    assert(
      snapshot.forTool(Tool.Scala) == Some(
        Upstream(version("3.10.1"), List(version("3.3.9"), version("3.9.0")))
      )
    )
    assert(snapshot.forTool(Tool.Sbt) == Some(Upstream(version("2.1.1"), Nil)))
  }

  test("Snapshot keeps an empty lts list") {
    val snapshot = Snapshot.parse("sbt latest=2.1.1 lts=").fold(error => fail(error), identity)

    assert(snapshot.forTool(Tool.Sbt) == Some(Upstream(version("2.1.1"), Nil)))
  }

  test("Snapshot rejects an unknown tool and a missing latest") {
    assert(Snapshot.parse("gradle latest=9.1.0").isLeft)
    assert(Snapshot.parse("scala lts=3.9.0").isLeft)
  }
}
