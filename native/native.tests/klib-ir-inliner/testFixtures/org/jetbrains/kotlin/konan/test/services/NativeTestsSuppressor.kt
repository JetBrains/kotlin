/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.suppressors

import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.model.SimpleTestFailureSuppressor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class NativeTestsSuppressor(
    testServices: TestServices,
) : SimpleTestFailureSuppressor(testServices) {
    override fun testIsMuted(): Boolean {
        return testServices.testRunSettings.isIgnoredTarget(testServices.moduleStructure.allDirectives)
    }

    override fun checkIfTestShouldBeUnmuted() {
        if (testIsMuted()) {
            throw AssertionError("Looks like this test can be unmuted. Adjust/remove a relevant test directive IGNORE_BACKEND or IGNORE_NATIVE")
        }
    }
}
