/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.interop

import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import java.io.File

private fun assertFilesEqual(expected: File, actual: File, message: () -> String) {
    try {
        JUnit5Assertions.assertEqualsToFile(
                expected,
                if (actual.isFile) actual.readText() else "",
                sanitizer = { it }
        ) {
            """|${message()}
               |processing file
            """.trimMargin()
        }
    } catch (e: AssertionFailedError) {
        if (JUnit5Assertions.isTeamCityBuild) {
            if (actual.isFile) {
                println(actual.readText())
            }
        }
        throw e
    }
}

private fun assertDirectoriesEqual(expected: File, actual: File, message: () -> String) {
    val expectedListing = expected.list().orEmpty().toSet()
    val actualListing = actual.list().orEmpty().toSet()
    (actualListing + expectedListing).forEach { path ->
        val expectedFile = File(expected, path)
        val actualFile = File(actual, path)
        when {
            expectedFile.isDirectory -> assertDirectoriesEqual(expectedFile, actualFile, message)
            else -> assertFilesEqual(expectedFile, actualFile, message)
        }
    }
}

open class AbstractConsistencyCheckTest {
    @Test
    fun checkConsistency() {
        val usePrebuiltSources = System.getProperty("usePrebuiltSources").toBooleanStrict()
        val bindingsRoot = File(System.getProperty("bindingsRoot"))
        val generatedRoot = File(System.getProperty("generatedRoot"))
        val projectName = System.getProperty("projectName")
        val regenerateTaskName = System.getProperty("regenerateTaskName")
        val hostName = System.getProperty("hostName")
        if (!usePrebuiltSources) {
            assertEquals(generatedRoot.absolutePath, bindingsRoot.absolutePath) {
                """|At $projectName
                   |on $hostName
                   |Mismatched generation root and bindings root when prebuilt sources are unused
                """.trimMargin()
            }
            abort<Unit>("Prebuilt sources are not used for $projectName")
        }

        assertDirectoriesEqual(bindingsRoot, generatedRoot) {
            """|At $projectName
               |on $hostName
               |Generated bindings in ${generatedRoot.absolutePath}
               |Do not match prebuilt in ${bindingsRoot.absolutePath}
               |To regenerate the bindings, run ./gradlew $projectName:$regenerateTaskName
            """.trimMargin()
        }
    }
}