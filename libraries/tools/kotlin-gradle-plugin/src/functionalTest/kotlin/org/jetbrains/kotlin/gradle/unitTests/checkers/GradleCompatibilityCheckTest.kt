/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.registerMinimalVariantImplementationFactoriesForTests
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internal.diagnostics.GradleCompatibilityCheck
import org.jetbrains.kotlin.gradle.internal.diagnostics.GradleCompatibilityCheck.runGradleCompatibilityCheck
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class GradleCompatibilityCheckTest {
    private val genericProject = buildProject {
        gradle.registerMinimalVariantImplementationFactoriesForTests()
    }

    private val unsupportedGradleVersion = FixedAndroidGradlePluginVersionProvider("7.5.1")
    private val supportedGradleVersion = FixedAndroidGradlePluginVersionProvider(
        GradleCompatibilityCheck.minSupportedGradleVersionString
    )

    @Test
    fun testVersionNotSupported() {
        val error = assertFails {
            genericProject.runGradleCompatibilityCheck(unsupportedGradleVersion)
        }

        assertTrue(error is InvalidUserCodeException)
        genericProject.checkDiagnostics("checkers/gradleCompatibilityCheck/versionTooLow")
    }

    @Test
    fun testVersionIsCompatible() {
        genericProject.runGradleCompatibilityCheck(supportedGradleVersion)
        genericProject.assertNoDiagnostics()
    }

    internal class FixedAndroidGradlePluginVersionProvider(
        private val version: String
    ) : GradleCompatibilityCheck.CurrentGradleVersionProvider {
        override fun get(): GradleVersion = GradleVersion.version(version)
    }
}