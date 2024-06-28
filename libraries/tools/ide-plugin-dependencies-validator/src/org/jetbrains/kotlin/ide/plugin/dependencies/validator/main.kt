/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.relativeTo

fun main(args: Array<String>) {
    check(args.isNotEmpty())

    val experimentalAnnotationUsages =
        ExperimentalOptInUsageInSourceChecker.checkExperimentalOptInUsage(args.map { Paths.get(it) })
            .filterNot { usage -> allowedUsages.any { it.isAllowed(usage) } }

    if (experimentalAnnotationUsages.isNotEmpty()) {
        val errorMessage = buildString {
            appendLine(
                """
                Experimental Kotlin StdLib API cannot be used in the modules that are used in the IDE. 
                See KT-62510 for more details.
                The following files use deprecated APIs:
                """.trimIndent()
            )
            for (annotationUsage in experimentalAnnotationUsages) {
                appendLine(
                    " - " + annotationUsage.file.relativeTo(Paths.get(".").toAbsolutePath()).toString() +
                            ":" + annotationUsage.lineNumber +
                            " has an opt-in for experimental declaration: @OptIn(${annotationUsage.usedExperimentalAnnotation}::class)"
                )
            }
        }
        error(errorMessage)
    }
}


//  Please do not add new usages here, it may break IntelliJ Kotlin Plugin. See KT-62510 for more details
private val allowedUsages = listOf(
    // TODO should be removed as a part of KTIJ-27368
    AllowedUsage(
        Paths.get("compiler/ir/serialization.common/src/org/jetbrains/kotlin/backend/common/serialization/CityHash.kt"),
        "ExperimentalUnsignedTypes"
    )
)

private data class AllowedUsage(
    val file: Path,
    val usedExperimentalAnnotation: String,
) {
    fun isAllowed(usage: ExperimentalAnnotationUsage) =
        usage.file.absolute() == file.absolute()
                && usedExperimentalAnnotation == usage.usedExperimentalAnnotation
}

