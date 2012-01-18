package kotlin.tools

import std.*
import std.io.*
import std.util.*
import java.io.*
import java.util.*

fun generateFile(outFile: File, header: String, inputFile: File, f: (String)-> String) {
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

        val xform = f(line)
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
  val outDir = File(srcDir, "generated")


  // JavaIterables - Generic iterable stuff
  generateFile(File(outDir, "ArraysFromJavaIterables.kt"), "package std", File(srcDir, "JavaIterables.kt")) {
      it.replaceAll("java.lang.Iterable<T>", "Array<T>")
  }

  generateFile(File(outDir, "StandardFromJavaIterables.kt"), "package std", File(srcDir, "JavaIterables.kt")) {
    it.replaceAll("java.lang.Iterable<T>", "Iterable<T>")
  }


  // JavaCollections - methods returning a collection of the same input size (if its a collection)

  generateFile(File(outDir, "ArraysFromJavaCollections.kt"), "package std", File(srcDir, "JavaCollections.kt")) {
    it.replaceAll("java.util.Collection<T>", "Array<T>")
  }

  generateFile(File(outDir, "JavaUtilIterablesFromJavaCollections.kt"), "package std.util", File(srcDir, "JavaCollections.kt")) {
    it.replaceAll("java.util.Collection<T>", "java.lang.Iterable<T>").replaceAll("(this.size)", "")
  }

  generateFile(File(outDir, "StandardFromJavaCollections.kt"), "package std", File(srcDir, "JavaCollections.kt")) {
    it.replaceAll("java.util.Collection<T>", "Iterable<T>").replaceAll("(this.size)", "")
  }
}
