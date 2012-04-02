package org.jetbrains.kotlin.tools

import java.io.*

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
    var srcDir = File("src/kotlin")
    if (!srcDir.exists()) {
        srcDir = File("stdlib/src/kotlin")
        require(srcDir.exists(), "Could not find the src/kotlin directory!")
    }
    val outDir = File(srcDir, "../generated")

    // JLangIterables - Generic iterable stuff
    generateFile(File(outDir, "ArraysFromJLangIterables.kt"), "package kotlin\n\nimport kotlin.util.*", File(srcDir, "JLangIterables.kt")) {
        it.replaceAll("java.lang.Iterable<T", "Array<T").replaceAll("java.lang.Iterable<T", "Array<T")
    }
    generateFile(File(outDir, "ArraysFromJLangIterablesLazy.kt"), "package kotlin\n\nimport kotlin.util.*", File(srcDir, "JLangIterablesLazy.kt")) {
        it.replaceAll("java.lang.Iterable<T", "Array<T").replaceAll("java.lang.Iterable<T", "Array<T")
    }

    generateFile(File(outDir, "StandardFromJLangIterables.kt"), "package kotlin\n\nimport kotlin.util.*", File(srcDir, "JLangIterables.kt")) {
        it.replaceAll("java.lang.Iterable<T", "Iterable<T")
    }
    generateFile(File(outDir, "StandardFromJLangIterablesLazy.kt"), "package kotlin\n\nimport kotlin.util.*", File(srcDir, "JLangIterablesLazy.kt")) {
        it.replaceAll("java.lang.Iterable<T", "Iterable<T")
    }

    generateFile(File(outDir, "JUnilIteratorsFromJLangIterables.kt"), "package kotlin", File(srcDir, "JLangIterables.kt")) {
        it.replaceAll("java.lang.Iterable<T", "java.util.Iterator<T")
    }


    // JUtilCollections - methods returning a collection of the same input size (if its a collection)

    generateFile(File(outDir, "ArraysFromJUtilCollections.kt"), "package kotlin", File(srcDir, "JUtilCollections.kt")) {
        it.replaceAll("java.util.Collection<T", "Array<T")
    }

    generateFile(File(outDir, "JUnilIterablesFromJUtilCollections.kt"), "package kotlin", File(srcDir, "JUtilCollections.kt")) {
        it.replaceAll("java.util.Collection<T", "java.lang.Iterable<T").replaceAll("(this.size)", "")
    }

    generateFile(File(outDir, "StandardFromJUtilCollections.kt"), "package kotlin", File(srcDir, "JUtilCollections.kt")) {
        it.replaceAll("java.util.Collection<T", "Iterable<T").replaceAll("(this.size)", "")
    }
}
