/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package generators

import java.io.*
import templates.*
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

    COPYRIGHT_NOTICE = readCopyrightNoticeFromProfile { Thread.currentThread().contextClassLoader.getResourceAsStream("apache.xml").reader() }

    val targetBaseDirs = mutableMapOf<KotlinTarget, File>()

    when (args.size) {
        1 -> {
            val baseDir = File(args.first())
            targetBaseDirs[KotlinTarget.Common] =  baseDir.resolveExistingDir("libraries/stdlib/common/src/generated")
            targetBaseDirs[KotlinTarget.JVM] = baseDir.resolveExistingDir("libraries/stdlib/jvm/src/generated")
            targetBaseDirs[KotlinTarget.JS] = baseDir.resolveExistingDir("libraries/stdlib/js/src/generated")
            targetBaseDirs[KotlinTarget.JS_IR] = baseDir.resolveExistingDir("libraries/stdlib/js/irRuntime/generated")
        }
        2 -> {
            val (targetName, targetDir) = args
            val target = KotlinTarget.values.singleOrNull { it.name.equals(targetName, ignoreCase = true) } ?: error("Invalid target: $targetName")
            targetBaseDirs[target] = File(targetDir).also { it.requireExistingDir() }
        }
        else -> {
            println("""Parameters:
    <kotlin-base-dir> - generates sources for common, jvm, js, ir-js targets using paths derived from specified base path
    <target> <target-dir> - generates source for the specified target in the specified target directory
""")
            exitProcess(1)
        }
    }

    templateGroups.groupByFileAndWrite(targetsToGenerate = targetBaseDirs.keys) { (target, source) ->
        val targetDir = targetBaseDirs[target] ?: error("Target $target directory is not configured")
        val platformSuffix = when (val platform = target.platform) {
            Platform.Common -> ""
            else -> platform.name.toLowerCase().capitalize()
        }
        targetDir.resolve("_${source.name.capitalize()}$platformSuffix.kt")
    }
}

fun File.resolveExistingDir(subpath: String) = resolve(subpath).also { it.requireExistingDir() }

fun File.requireExistingDir() {
    require(isDirectory) { "Directory $this doesn't exist"}
}