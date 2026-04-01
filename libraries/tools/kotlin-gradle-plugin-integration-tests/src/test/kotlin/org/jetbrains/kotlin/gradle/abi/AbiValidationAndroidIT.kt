/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertTrue

@AndroidGradlePluginTests
class AbiValidationAndroidIT : KGPBaseTest() {
    @DisplayName("KT-78525 (Android)")
    @GradleAndroidTest
    fun testAndroidCompatibilityWithBcv(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidProject(gradleVersion, agpVersion, jdkVersion, applyBcvPlugin = true) {
            abiValidation { }

            build("updateKotlinAbi")
            assertFileExists(referenceJvmDumpFile())
            assertTrue(referenceJvmDumpFile().length() > 0)

            build("checkKotlinAbi")
            build("apiCheck")
        }
    }

    @DisplayName("KT-78525 (KMP)")
    @GradleAndroidTest
    fun testKmpCompatibilityWithBcv(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        kmpWithAndroidProject(gradleVersion, agpVersion, jdkVersion, applyBcvPlugin = true) {
            abiValidation()

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    sourceSets.commonMain.get().compileSource("""class SimpleClass { fun method(): String = "Hello, world!" }""")
                }
            }

            build("updateKotlinAbi")

            val referenceMixedJvmDumpFile = referenceMixedJvmDumpFile()
            assertFileExists(referenceMixedJvmDumpFile)
            assertFileContains(referenceMixedJvmDumpFile.toPath(), "class SimpleClass")

            val referenceMixedAndroidDumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(referenceMixedAndroidDumpFile)
            assertFileContains(referenceMixedAndroidDumpFile.toPath(), "class SimpleClass")

            build("checkKotlinAbi")
            build("apiCheck")
        }
    }
}