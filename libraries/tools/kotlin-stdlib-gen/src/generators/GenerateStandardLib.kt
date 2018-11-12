/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

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
        Ordering,
        ArrayOps,
        Snapshots,
        Mapping,
        SetOps,
        Aggregates,
        Guards,
        Generators,
        StringJoinOps,
        SequenceOps,
        RangeOps,
        Numeric,
        ComparableOps
    )

    require(args.size == 1) { "Expecting Kotlin project home path as an argument" }
    val baseDir = File(args.first())

    COPYRIGHT_NOTICE = readCopyrightNoticeFromProfile(baseDir.resolve(".idea/copyright/apache.xml"))

    fun File.resolveExistingDir(subpath: String) = resolve(subpath).also {
        require(it.isDirectory) { "Directory $it doesn't exist"}
    }

    val commonDir = baseDir.resolveExistingDir("libraries/stdlib/common/src/generated")
    val jvmDir = baseDir.resolveExistingDir("libraries/stdlib/jvm/src/generated")
    val jsDir = baseDir.resolveExistingDir("libraries/stdlib/js/src/generated")
    val jsIrDir = baseDir.resolveExistingDir("libraries/stdlib/js/irRuntime/generated")

    templateGroups.groupByFileAndWrite { (target, source) ->
        //        File("build/out/$platform/$source.kt")
        when (target) {
            KotlinTarget.Common -> commonDir.resolve("_${source.name.capitalize()}.kt")
            KotlinTarget.JVM -> jvmDir.resolve("_${source.name.capitalize()}Jvm.kt")
            KotlinTarget.JS -> jsDir.resolve("_${source.name.capitalize()}Js.kt")
            KotlinTarget.JS_IR -> jsIrDir.resolve("_${source.name.capitalize()}Js.kt")
            KotlinTarget.Native -> error("Native is unsupported yet")
        }
    }
}
