/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.enableKmpProjectIsolationSupport
import kotlin.test.Test

class GradleDeprecatedPropertyChecker {

    @Test
    fun `KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT reports deprecation warning`() {
        val project = buildProjectWithMPP(
            preApplyCode = { enableKmpProjectIsolationSupport(enabled = true) },
        )
        project.checkDiagnostics("KmpIsolatedProjectsSupportDeprecated")
    }
}