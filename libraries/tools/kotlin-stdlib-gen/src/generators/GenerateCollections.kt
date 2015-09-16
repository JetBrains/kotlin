package generators

import java.io.*
import templates.Family.*
import templates.*
import templates.PrimitiveType.*

fun generateCollectionsAPI(outDir: File) {
    val templates = sequenceOf(
            ::elements,
            ::filtering,
            ::ordering,
            ::arrays,
            ::snapshots,
            ::mapping,
            ::sets,
            ::aggregates,
            ::guards,
            ::generators,
            ::strings,
            ::sequences,
            ::specialJVM,
            ::ranges,
            ::numeric,
            ::comparables
    ).flatMap { it().sortedBy { it.signature }.asSequence() }

    val groupedConcreteFunctions = templates.flatMap { it.instantiate().asSequence() }.groupBy { it.sourceFile }

    for ((sourceFile, functions) in groupedConcreteFunctions) {
        functions.writeTo(outDir, sourceFile)
    }
}

fun generateCollectionsJsAPI(outDir: File) {
    specialJS().writeTo(File(outDir, "kotlin_special.kt")) { build() }
}
