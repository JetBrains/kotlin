/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.suppressors

import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.junit.jupiter.api.Assumptions

class NativeTestsSuppressor(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (failedAssertions.isEmpty()) {
            return buildList {
                addAll(testServices.createUnmutingErrorIfNeeded())
            }.map { it.wrap() }
        }

        val newFailedAssertions = failedAssertions.flatMap { wrappedException ->
            testServices.processException(wrappedException)
        }

        if (newFailedAssertions.isEmpty()) {
            // Explicitly mark the test as "ignored".
            throw Assumptions.abort<Nothing>()
        } else {
            return newFailedAssertions
        }
    }
}

private fun TestServices.processException(wrappedException: WrappedException): List<WrappedException> {
    return if (testRunSettings.isIgnoredTarget(moduleStructure.allDirectives))
        emptyList()
    else listOf(wrappedException)
}

private fun TestServices.createUnmutingErrorIfNeeded(): List<Throwable> {
    return if (testRunSettings.isIgnoredTarget(moduleStructure.allDirectives))
        listOf(AssertionError("Looks like this test can be unmuted. Adjust/remove a relevant test directive IGNORE_BACKEND or IGNORE_NATIVE"))
    else emptyList()
}
