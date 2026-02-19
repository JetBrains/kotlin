/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolute

class ExperimentalOptInUsageInSourceCheckerTest {
    @Test
    fun test() {
        println(Paths.get(".").toAbsolutePath())
        val sourcePath = basePath.resolve("testData/source")
        val usages = ExperimentalOptInUsageInSourceChecker.checkExperimentalOptInUsage(listOf(sourcePath))
        Assertions.assertEquals(
            listOf(
                ExperimentalAnnotationUsage(Paths.get("pckg/experimentalPathApi.kt"), lineNumber = 1, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("pckg/experimentalPathApi.kt"), lineNumber = 6, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("pckg/experimentalPathApi.kt"), lineNumber = 12, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("pckg/experimentalPathApi.kt"), lineNumber = 21, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("pckg/experimentalPathApi.kt"), lineNumber = 27, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("experimentalStdlibApi.kt"), lineNumber = 1, "ExperimentalStdlibApi"),
                ExperimentalAnnotationUsage(Paths.get("experimentalStdlibApi.kt"), lineNumber = 6, "ExperimentalStdlibApi"),
                ExperimentalAnnotationUsage(Paths.get("experimentalStdlibApi.kt"), lineNumber = 12, "ExperimentalStdlibApi"),
                ExperimentalAnnotationUsage(Paths.get("experimentalStdlibApi.kt"), lineNumber = 21, "ExperimentalStdlibApi"),
                ExperimentalAnnotationUsage(Paths.get("experimentalStdlibApi.kt"), lineNumber = 26, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("multiple.kt"), lineNumber = 2, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("multiple.kt"), lineNumber = 2, "ExperimentalStdlibApi"),
                ExperimentalAnnotationUsage(Paths.get("multiple.kt"), lineNumber = 7, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("multiple.kt"), lineNumber = 8, "ExperimentalStdlibApi"),
                ExperimentalAnnotationUsage(Paths.get("multiple.kt"), lineNumber = 14, "ExperimentalPathApi"),
                ExperimentalAnnotationUsage(Paths.get("multiple.kt"), lineNumber = 15, "ExperimentalPathApi")
            ).map { it.copy(file = sourcePath.resolve(it.file)) }.sortedWith(experimentalAnnotationUsageComparator),
            usages.sortedWith(experimentalAnnotationUsageComparator),
        )
    }

    companion object {
        private val experimentalAnnotationUsageComparator =
            compareBy<ExperimentalAnnotationUsage>(
                { it.file.absolute().toString() },
                { it.lineNumber },
                { it.usedExperimentalAnnotation },
            )
    }
}