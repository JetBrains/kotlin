/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.test.services.sourceProviders.AbstractLauncherAdditionalSourceProvider.Companion.groupedLauncherFileName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Checks that [PrettyResultsHandler] attributes the failures reported by a batch executable to the tests of the batch.
 */
class PrettyResultsHandlerTest {
    @Test
    fun attributesFailureOfIsolatedTest() {
        assertEquals(listOf(""), PrettyResultsHandler.findFailedTestsWithTCLogger(failureReason("__launcher__Kt.runTest")))
    }

    @Test
    fun attributesFailuresOfGroupedTests() {
        // The hash of the first module name is negative, and the one of the second is positive.
        val launcherA = launcherFacadeClassName("NativeCodegenBoxTestGenerated.testWhen7.main")
        val launcherB = launcherFacadeClassName("main")
        val reason = failureReason(
            "box.NativeCodegenBoxTestGenerated.testWhen7.$launcherA.runTest",
            "box.NativeCodegenBoxTestGenerated.testMain.$launcherB.runTest",
        )
        assertEquals(
            listOf("box.NativeCodegenBoxTestGenerated.testWhen7", "box.NativeCodegenBoxTestGenerated.testMain"),
            PrettyResultsHandler.findFailedTestsWithTCLogger(reason),
        )
    }

    @Test
    fun ignoresOtherFailedFunctions() {
        assertEquals(emptyList<String>(), PrettyResultsHandler.findFailedTestsWithTCLogger(failureReason("box.TestKt.runTest")))
    }

    private fun launcherFacadeClassName(moduleName: String): String =
        groupedLauncherFileName(moduleName).removeSuffix(".kt") + "Kt"

    // As `TestRunCheck.TestFiltering` lists the failed tests of the test report.
    private fun failureReason(vararg failedTests: String): String = buildString {
        append("Failed tests found in the test report:")
        failedTests.forEach { appendLine().append(" - ").append(it) }
    }
}
