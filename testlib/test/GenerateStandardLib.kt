namespace kotlin.tools

import std.*
import std.io.*
import std.util.*
import java.io.*
import java.util.*

fun generateFile(outFile: File, header: String, typeName: String, inputFile: File) {
  println("Parsing $inputFile and writing $outFile")

  outFile.getParentFile()?.mkdirs()
  val writer = PrintWriter(FileWriter(outFile))
  try {
    writer.println("// NOTE this file is auto-generated from $inputFile")
    writer.println(header)

    val reader = FileReader(inputFile).buffered()
    try {
      // TODO ideally we'd use a filterNot() here :)
      val iter = reader.lineIterator()
      while (iter.hasNext) {
        val line = iter.next()

        if (line.startsWith("package")) continue
        val xform = line.replaceAll("java.lang.Iterable<T>", typeName).replaceAll("java.util.Collection<T>", typeName)
        writer.println(xform)
      }
    } finally {
      reader.close()
      reader.close()
    }
  } finally {
    writer.close()
  }
}


/**
 * Generates methods in the standard library which are mostly identical
 * but just using a different input kind.
 *
 * Kinda like mimicking source macros here, but this avoids the inefficiency of type conversions
 * at runtime.
 */
fun main(args: Array<String>) {
  var stdlib = File("stdlib")
  if (!stdlib.exists()) {
    stdlib = File("../stdlib")
    if (!stdlib.exists()) {
      println("Cannot find stdlib!")
      return
    }
  }
  val srcDir = File(stdlib, "ktSrc")
  val input = File(srcDir, "JavaIterables.kt")
  val outDir = File(srcDir, "generated")
  generateFile(File(outDir, "ArraysGenerated.kt"), "package std", "Array<T>", input)
}