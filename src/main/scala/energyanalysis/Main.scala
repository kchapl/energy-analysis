package energyanalysis

import java.time.YearMonth

@main
def main(electricityPath: String, gasPath: String, year: Int = YearMonth.now().getYear): Unit =
  def abort(message: String): Nothing =
    Console.err.println(message)
    sys.exit(1)

  def lines(path: String): List[String] =
    File.fetch(path) match
      case Right(lines) => lines
      case Left(error)  => abort(s"Error reading $path: $error")

  MonthlyUsage.validateYear(year) match
    case Left(error) => abort(s"Error processing year $year: $error")
    case Right(_)    => ()

  val report =
    MonthlyUsage.report(lines(electricityPath), lines(gasPath), year) match
      case Right(report) => report
      case Left(error)   => abort(s"Error processing year $year: $error")

  report.problems.foreach { problem =>
    Console.err.println(s"Error processing ${problem.source}: ${problem.error}")
  }

  for (monthUsage <- report.months)
    val start = monthUsage.month.atDay(1)
    val end   = monthUsage.month.atEndOfMonth
    println(s"$start: $end: ${monthUsage.electricity.round}: ${monthUsage.gas.round}")
