package energyanalysis

/**
 * The toolchain policy, and the check that says whether the declared versions still match it.
 *
 * The policy: the JDK and Scala track the latest LTS release, sbt tracks the latest stable release.
 * What is declared lives in the repository, read by [[Declarations]]; what is published arrives as
 * an [[Toolchain.Upstream]] snapshot. [[Toolchain.check]] turns the two into verdicts and performs
 * no I/O, so its verdicts are deterministic and can be tested without network access.
 */
object Toolchain:

  enum Tool(val label: String):
    case Jdk   extends Tool("jdk")
    case Scala extends Tool("scala")
    case Sbt   extends Tool("sbt")

  enum Channel(val label: String):
    case LatestLts extends Channel("latest LTS")
    case Latest    extends Channel("latest stable release")

  /** The policy: the support channel each tool is held to. */
  val policy: Map[Tool, Channel] =
    Map(Tool.Jdk -> Channel.LatestLts, Tool.Scala -> Channel.LatestLts, Tool.Sbt -> Channel.Latest)

  final case class Version(major: Int, minor: Int, patch: Int) extends Ordered[Version]:

    def line: (Int, Int) = (major, minor)

    def compare(other: Version): Int =
      val byMajor = major.compare(other.major)
      val byMinor = minor.compare(other.minor)
      if byMajor != 0 then byMajor
      else if byMinor != 0 then byMinor
      else patch.compare(other.patch)

    override def toString: String = s"$major.$minor.$patch"

  object Version:

    /**
     * Reads digits and dots from the start of `raw`, dropping any further components, so a JDK
     * string such as `25.0.5.1+2` reads as `25.0.5` and `25` reads as `25.0.0`.
     */
    def parse(raw: String): Either[String, Version] =
      val parts = raw.trim.takeWhile(c => c.isDigit || c == '.').split('.').filter(_.nonEmpty)
      parts.headOption match
        case None    => Left(s"'$raw' is not a version")
        case Some(_) =>
          val numbers = parts.take(3).map(_.toInt)
          Right(Version(numbers(0), numbers.lift(1).getOrElse(0), numbers.lift(2).getOrElse(0)))

  /**
   * What upstream publishes for one tool: its newest release overall, and the newest release on
   * each LTS line that is still supported.
   */
  final case class Upstream(latest: Version, lts: List[Version])

  final case class Snapshot(entries: Map[Tool, Upstream]):
    def forTool(tool: Tool): Option[Upstream] = entries.get(tool)

  object Snapshot:

    /**
     * Parses the line-oriented snapshot format:
     * {{{
     * jdk   latest=27.0.1 lts=21.0.13,25.0.5
     * scala latest=3.10.1 lts=3.3.9,3.9.0
     * sbt   latest=2.1.1
     * }}}
     * Blank lines and lines starting with `#` are ignored.
     */
    def parse(text: String): Either[String, Snapshot] =
      val lines =
        text.linesIterator.map(_.trim).filter(line => line.nonEmpty && !line.startsWith("#"))
      lines
        .foldLeft[Either[String, Map[Tool, Upstream]]](Right(Map.empty)) { (acc, line) =>
          acc.flatMap(entries =>
            parseLine(line).map((tool, upstream) => entries.updated(tool, upstream))
          )
        }
        .map(Snapshot.apply)

    private val byLabel: Map[String, Tool] = Tool.values.map(tool => tool.label -> tool).toMap

    private def parseLine(line: String): Either[String, (Tool, Upstream)] =
      line.split("\\s+").toList match
        case label :: fields =>
          for
            tool        <- byLabel.get(label).toRight(s"unknown tool '$label' in '$line'")
            latest      <- field(fields, "latest").toRight(s"$label: missing latest= in '$line'")
            published   <- Version.parse(latest).left.map(error => s"$label: $error")
            ltsVersions <- parseVersions(label, field(fields, "lts").toList.flatMap(_.split(',')))
          yield (tool, Upstream(published, ltsVersions))
        case Nil => Left("empty snapshot line")

    private def field(fields: List[String], name: String): Option[String] =
      fields.collectFirst {
        case field if field.startsWith(s"$name=") => field.drop(name.length + 1)
      }

    private def parseVersions(label: String, raw: List[String]): Either[String, List[Version]] =
      raw
        .filter(_.nonEmpty)
        .foldLeft[Either[String, List[Version]]](Right(Nil)) { (acc, value) =>
          for
            versions <- acc
            version  <- Version.parse(value).left.map(error => s"$label lts: $error")
          yield versions :+ version
        }

  enum Reason(val description: String):
    case NotLts          extends Reason("not on a supported LTS line")
    case AheadOfUpstream extends Reason("newer than upstream reports")
    case NoUpstreamData  extends Reason("absent from the upstream snapshot")

  enum Verdict:
    case Current
    case Behind(expected: Version, major: Boolean)
    case OutsidePolicy(reason: Reason, expected: Option[Version])

  final case class Declared(tool: Tool, version: Version)

  final case class Finding(tool: Tool, declared: Version, verdict: Verdict):

    def channel: Channel = policy(tool)

    def isDrift: Boolean = verdict != Verdict.Current

    def isFailure: Boolean = verdict match
      case Verdict.OutsidePolicy(_, _) => true
      case _                           => false

    /**
     * Names the tool, the declared version, the expected version and the policy, so the reader does
     * not have to re-derive the reasoning.
     */
    def message: String =
      val label = tool.label
      val rule  = channel.label
      verdict match
        case Verdict.Current =>
          s"$label: declared $declared is current (policy: $rule)"
        case Verdict.Behind(expected, major) =>
          val kind = if major then "major update available" else "newer version available"
          s"$label: declared $declared, expected $expected (policy: $rule) - $kind"
        case Verdict.OutsidePolicy(reason, expected) =>
          val goal = expected.fold("")(version => s", expected $version")
          s"$label: declared $declared is outside policy (policy: $rule) - ${reason.description}$goal"

  /**
   * Compares declared versions against upstream and reports one finding per tool, in the order the
   * tools were declared.
   */
  def check(declared: List[Declared], snapshot: Snapshot): List[Finding] =
    declared.map { entry =>
      val verdict = verdictFor(policy(entry.tool), entry, snapshot.forTool(entry.tool))
      Finding(entry.tool, entry.version, verdict)
    }

  def report(findings: List[Finding]): String = findings.map(_.message).mkString("\n")

  def failed(findings: List[Finding]): Boolean = findings.exists(_.isFailure)

  /**
   * The JDK declaration names a feature release, so only its major number is meaningful: asking for
   * `25` always resolves the newest `25.x` patch.
   */
  private def comparable(tool: Tool, version: Version): Version =
    if tool == Tool.Jdk then Version(version.major, 0, 0) else version

  private def verdictFor(
      channel: Channel,
      declared: Declared,
      upstream: Option[Upstream]
  ): Verdict =
    upstream match
      case None            => Verdict.OutsidePolicy(Reason.NoUpstreamData, None)
      case Some(published) =>
        val mine = comparable(declared.tool, declared.version)
        channel match
          case Channel.LatestLts =>
            val target = published.lts.map(comparable(declared.tool, _)).maxOption
            val lines  = published.lts.map(version => comparable(declared.tool, version).line).toSet
            if !lines.contains(mine.line) then Verdict.OutsidePolicy(Reason.NotLts, target)
            else
              target match
                case None => Verdict.OutsidePolicy(Reason.NoUpstreamData, None)
                case Some(newest) if mine == newest => Verdict.Current
                case Some(newest) if mine < newest  => Verdict.Behind(newest, major = false)
                case Some(newest) => Verdict.OutsidePolicy(Reason.AheadOfUpstream, Some(newest))
          case Channel.Latest =>
            val newest = published.latest
            if mine == newest then Verdict.Current
            else if mine < newest then Verdict.Behind(newest, major = mine.major != newest.major)
            else Verdict.OutsidePolicy(Reason.AheadOfUpstream, Some(newest))
