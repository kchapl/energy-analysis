package energyanalysis

import org.scalatest.funsuite.AnyFunSuite
import java.io.{File => JFile}
import java.io.PrintWriter

class FileTest extends AnyFunSuite {
  test("fetch should successfully read lines from file") {
    val tempFile = JFile.createTempFile("test", ".txt")
    val writer = new PrintWriter(tempFile)
    writer.write("line1\nline2\nline3")
    writer.close()
    
    val result = File.fetch(tempFile.getPath)
    assert(result.isRight)
    assert(result.toOption.get == List("line1", "line2", "line3"))
    
    tempFile.delete()
  }

  test("fetch should return FileNotFound for non-existent file") {
    val result = File.fetch("nonexistent.txt")
    assert(result.isLeft)
    assert(result.left.toOption.get == FileNotFound("nonexistent.txt"))
  }
}
