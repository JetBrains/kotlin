/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators

import templates.*
import java.io.File
import kotlin.system.exitProcess

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

    val targetBaseDirs = mutableMapOf<KotlinTarget, File>()

    when (args.size) {
        1 -> {
            val baseDir = File(args.first())
            targetBaseDirs[KotlinTarget.Common] = baseDir.resolveExistingDir("libraries/stdlib/common/src/generated")
            targetBaseDirs[KotlinTarget.JVM] = baseDir.resolveExistingDir("libraries/stdlib/jvm/src/generated")
            targetBaseDirs[KotlinTarget.JS] = baseDir.resolveExistingDir("libraries/stdlib/js/src/generated")
            targetBaseDirs[KotlinTarget.WASM] = baseDir.resolveExistingDir("libraries/stdlib/wasm/src/generated")
            targetBaseDirs[KotlinTarget.Native] = baseDir.resolveExistingDir("kotlin-native/runtime/src/main/kotlin/generated")
        }
        else -> {
            println("""Parameters:
    <kotlin-base-dir> - generates sources for common, jvm, js, ir-js, native targets using paths derived from specified base path
""")
            exitProcess(1)
        }
    }

    templateGroups.groupByFileAndWrite(targetsToGenerate = targetBaseDirs.keys) { (target, source) ->
        val targetDir = targetBaseDirs[target] ?: error("Target $target directory is not configured")
        val platformSuffix = when (val platform = target.platform) {
            Platform.Common -> ""
            Platform.Native -> if (target.backend == Backend.Wasm) "Wasm" else "Native"
            else -> platform.name.lowercase().capitalize()
        }
        targetDir.resolve("_${source.name.capitalize()}$platformSuffix.kt")
    }
}

fun File.resolveExistingDir(subpath: String) = resolve(subpath).also { it.requireExistingDir() }

fun File.requireExistingDir() {
    require(isDirectory) { "Directory $this doesn't exist" }
}
