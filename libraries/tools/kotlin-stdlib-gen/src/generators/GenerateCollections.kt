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
    sequences().writeTo(File(outDir, "_Sequences.kt")) { build() }
    specialJVM().writeTo(File(outDir, "_SpecialJVM.kt")) { build() }
    ranges().writeTo(File(outDir, "_Ranges.kt")) { build() }

    numeric().writeTo(File(outDir, "_Numeric.kt")) {
        val builder = StringBuilder()
        for (numeric in numericPrimitives)
            for (family in buildFamilies)
                build(builder, family, numeric)

        builder.toString()
    }

    comparables().writeTo(File(outDir, "_Comparables.kt")) { build() }

}

fun generateCollectionsJsAPI(outDir: File) {
    specialJS().writeTo(File(outDir, "kotlin_special.kt")) { build() }
}
