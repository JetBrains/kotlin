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
            return listOfNotNull(testServices.createUnmutingErrorIfNeeded()?.wrap())
        }

        val newFailedAssertions = failedAssertions.mapNotNull { wrappedException ->
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

private fun TestServices.processException(wrappedException: WrappedException): WrappedException? {
    if (testRunSettings.isIgnoredTarget(moduleStructure.allDirectives))
        return null

    // TODO Remove this workaround for KT-73621, when tests having `EVALUATED{IR}` will be dropped in scope of KT-83514.
    if (wrappedException is WrappedException.FromMetaInfoHandler &&
        wrappedException.message?.startsWith("org.opentest4j.AssertionFailedError: Actual data differs from file content:") == true &&
        moduleStructure.modules.any {it.files.any { it.originalContent.contains("<!EVALUATED{IR}(")} }
    ) return null

    return wrappedException

}

private fun TestServices.createUnmutingErrorIfNeeded(): Throwable? =
    takeIf { testRunSettings.isIgnoredTarget(moduleStructure.allDirectives) }
        ?.run { AssertionError("Looks like this test can be unmuted. Adjust/remove a relevant test directive IGNORE_BACKEND or IGNORE_NATIVE") }
