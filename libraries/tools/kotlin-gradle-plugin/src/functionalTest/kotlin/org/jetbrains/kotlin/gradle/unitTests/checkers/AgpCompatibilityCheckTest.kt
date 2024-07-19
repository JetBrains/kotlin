/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpCompatibilityCheck.AndroidGradlePluginVersionProvider
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpCompatibilityCheck.minimalSupportedAgpVersion
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpCompatibilityCheck.runAgpCompatibilityCheckIfAgpIsApplied
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.registerMinimalVariantImplementationFactoriesForTests
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AgpCompatibilityCheckTest {
    private val projectWithAgpApplied = buildProject() {
        gradle.registerMinimalVariantImplementationFactoriesForTests()
        plugins.apply("com.android.library")
    }

    private val unsupportedAgpVersion = FixedAndroidGradlePluginVersionProvider("7.2.0")
    private val unknownAgpVersion = FixedAndroidGradlePluginVersionProvider(null)
    private val supportedAgpVersion = FixedAndroidGradlePluginVersionProvider(minimalSupportedAgpVersion.toString())

    @Test
    fun testVersionNotSupported() {
        val error = assertFails {
            projectWithAgpApplied.runAgpCompatibilityCheckIfAgpIsApplied(unsupportedAgpVersion)
        }

        assertTrue(error is InvalidUserCodeException)
        projectWithAgpApplied.checkDiagnostics("checkers/agpCompatibilityCheck/versionTooLow")
    }

    @Test
    fun testVersionNotDetected() {
        projectWithAgpApplied.runAgpCompatibilityCheckIfAgpIsApplied(unknownAgpVersion)
        projectWithAgpApplied.checkDiagnostics("checkers/agpCompatibilityCheck/versionUnknown")
    }

    @Test
    fun testVersionIsCompatible() {
        projectWithAgpApplied.runAgpCompatibilityCheckIfAgpIsApplied(supportedAgpVersion)
        projectWithAgpApplied.assertNoDiagnostics()
    }

    @Test
    fun testDiagnosticNotTriggeredOnNoAgpPluginApplied() {
        val project = buildProject()

        project.runAgpCompatibilityCheckIfAgpIsApplied(unsupportedAgpVersion)

        project.assertNoDiagnostics()
    }

    internal class FixedAndroidGradlePluginVersionProvider(
        private val version: String?
    ) : AndroidGradlePluginVersionProvider {
        override fun get(): AndroidGradlePluginVersion? {
            if (version == null) return null
            return AndroidGradlePluginVersion(version)
        }
    }
}