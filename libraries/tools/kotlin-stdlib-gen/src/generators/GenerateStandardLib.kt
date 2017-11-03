package generators

import java.io.*
import templates.*

/**
 * Generates methods in the standard library which are mostly identical
 * but just using a different input kind.
 *
 * Kinda like mimicking source macros here, but this avoids the inefficiency of type conversions
 * at runtime.
 */
fun main(args: Array<String>) {
    val templateGroups = sequenceOf<TemplateGroup>(
        Elements,
        Filtering,
//        Ordering,
//        ArrayOps,
//        Snapshots,
//        Mapping,
//        SetOps,
        Aggregates,
//        Guards,
//        Generators,
//        StringJoinOps,
//        SequenceOps,
//        RangeOps,
//        Numeric,
//        ComparableOps,
//        CommonArrays,
//        PlatformSpecialized,
//        PlatformSpecializedJS,
        { emptySequence() }
    )

    require(args.size == 1) { "Expecting Kotlin project home path as an argument" }
    val baseDir = File(args.first())

    fun File.resolveExistingDir(subpath: String) = resolve(subpath).also {
        require(it.isDirectory) { "Directory $it doesn't exist"}
    }

    val commonDir = baseDir.resolveExistingDir("libraries/stdlib/common/src/generated")
    val jvmDir = baseDir.resolveExistingDir("libraries/stdlib/src/generated")
    val jsDir = baseDir.resolveExistingDir("js/js.libraries/src/core/generated")

    templateGroups.groupByFileAndWrite { (platform, source) ->
        //        File("build/out/$platform/$source.kt")
        when (platform) {
            Platform.Common -> commonDir.resolve("_${source.name.capitalize()}.kt")
            Platform.JVM -> jvmDir.resolve("_${source.name.capitalize()}.kt")
            Platform.JS -> jsDir.resolve("_${source.name.capitalize()}Js.kt")
            Platform.Native -> error("Native is unsupported yet")
        }
    }
}
