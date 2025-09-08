/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpWithBuiltInKotlinAppliedCheck
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpWithBuiltInKotlinAppliedCheck.minimalBuiltInKotlinSupportedAgpVersion
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpWithBuiltInKotlinAppliedCheck.runAgpWithBuiltInKotlinIfAppliedCheck
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.registerMinimalVariantImplementationFactoriesForTests
import org.junit.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AgpWithBuiltInKotlinAppliedCheckTest {

    private val projectWithKotlinAndroidExtensionApplied = buildProject {
        gradle.registerMinimalVariantImplementationFactoriesForTests()
        project.createKotlinExtension(KotlinAndroidProjectExtension::class)
    }

    private val unaffectedAgpVersion = FixedAndroidGradlePluginVersionProvider("8.12.0")
    private val unknownAgpVersion = FixedAndroidGradlePluginVersionProvider(null)
    private val affectedAgpVersion = FixedAndroidGradlePluginVersionProvider(minimalBuiltInKotlinSupportedAgpVersion.toString())

    @Test
    fun testNotAffectedVersion() {
        projectWithKotlinAndroidExtensionApplied.runAgpWithBuiltInKotlinIfAppliedCheck(unaffectedAgpVersion)
        projectWithKotlinAndroidExtensionApplied.assertNoDiagnostics()
    }

    @Test
    fun testAffectedVersion() {
        val error = assertFails {
            projectWithKotlinAndroidExtensionApplied.runAgpWithBuiltInKotlinIfAppliedCheck(affectedAgpVersion)
        }

        assertTrue(error is InvalidUserCodeException)
        projectWithKotlinAndroidExtensionApplied.checkDiagnostics("checkers/agpBuiltInKotlinCheck/affectedVersion")
    }

    @Test
    fun testNoAgpApplied() {
        projectWithKotlinAndroidExtensionApplied.runAgpWithBuiltInKotlinIfAppliedCheck(unknownAgpVersion)
        projectWithKotlinAndroidExtensionApplied.assertNoDiagnostics()
    }

    internal class FixedAndroidGradlePluginVersionProvider(
        private val version: String?
    ) : AgpWithBuiltInKotlinAppliedCheck.AndroidGradlePluginVersionProvider {
        override fun get(): AndroidGradlePluginVersion? {
            if (version == null) return null
            return AndroidGradlePluginVersion(version)
        }
    }
}
