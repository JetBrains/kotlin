/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.enableKmpProjectIsolationSupport
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.test.Test

class GradleDeprecatedPropertyChecker {

    @Test
    fun `KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT reports deprecation warning`() {
        val project = buildProjectWithMPP(
            preApplyCode = { enableKmpProjectIsolationSupport(enabled = true) },
        )
        project.checkDiagnostics("KmpIsolatedProjectsSupportDeprecated")
    }

    @Test
    fun `KT-83254 - diagnostic with filtering - emits only when filter passes the property`() {
        val propertiesWithDeprecatedFalseValue = listOf(
            "kotlin.mpp.import.enableKgpDependencyResolution" to "EnableKgpDependencyResolutionDeprecation",
            "kotlin.publishJvmEnvironmentAttribute" to "EnablePublishJvmEnvironmentAttributeDeprecation",
        )
        propertiesWithDeprecatedFalseValue.forEach {
            buildProjectWithMPP(
                preApplyCode = { project.propertiesExtension.set(it.first, false.toString()) },
            ).checkDiagnostics(it.second)
            buildProjectWithMPP(
                preApplyCode = { project.propertiesExtension.set(it.first, true.toString()) },
            ).assertNoDiagnostics()
        }
    }
}