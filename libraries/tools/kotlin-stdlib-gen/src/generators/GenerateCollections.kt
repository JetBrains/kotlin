package generators

import java.io.*
import templates.Family.*
import templates.*
import templates.PrimitiveType.*

fun generateCollectionsAPI(outDir: File) {
    elements().writeTo(File(outDir, "_Elements.kt")) { build() }
    filtering().writeTo(File(outDir, "_Filtering.kt")) { build() }
    ordering().writeTo(File(outDir, "_Ordering.kt")) { build() }
    arrays().writeTo(File(outDir, "_Arrays.kt")) { build() }
    snapshots().writeTo(File(outDir, "_Snapshots.kt")) { build() }
    mapping().writeTo(File(outDir, "_Mapping.kt")) { build() }
    sets().writeTo(File(outDir, "_Sets.kt")) { build() }
    aggregates().writeTo(File(outDir, "_Aggregates.kt")) { build() }
    guards().writeTo(File(outDir, "_Guards.kt")) { build() }
    generators().writeTo(File(outDir, "_Generators.kt")) { build() }
    strings().writeTo(File(outDir, "_Strings.kt")) { build() }
    streams().writeTo(File(outDir, "_Streams.kt")) { build() }
    specialJVM().writeTo(File(outDir, "_SpecialJVM.kt")) { build() }

    numeric().writeTo(File(outDir, "_Numeric.kt")) {
        val builder = StringBuilder()
        // TODO: decide if sum for byte and short is needed and how to make it work
        for (numeric in listOf(Int, Long, /*Byte, Short, */ Double, Float)) {
            build(builder, Iterables, numeric)
            build(builder, Streams, numeric)
        }

        for (numeric in listOf(Int, Long, Byte, Short, Double, Float)) {
            build(builder, ArraysOfObjects, numeric)
            build(builder, ArraysOfPrimitives, numeric)
        }
        builder.toString()
    }

}
