/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.AbiValidationTestDumps.assertDumpsEqual
import org.jetbrains.kotlin.gradle.abi.utils.abiValidation
import org.jetbrains.kotlin.gradle.abi.utils.androidKmpLibraryProject
import org.jetbrains.kotlin.gradle.abi.utils.referenceMixedAndroidDumpFile
import org.jetbrains.kotlin.gradle.abi.utils.referenceMixedJvmDumpFile
import org.jetbrains.kotlin.gradle.testbase.AndroidGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.AndroidTestVersions
import org.jetbrains.kotlin.gradle.testbase.GradleAndroidTest
import org.jetbrains.kotlin.gradle.testbase.JdkVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.source

@AndroidGradlePluginTests
class AbiValidationAndroidKmpIT : KGPBaseTest() {

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    fun testAndroidOnly(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }

            build("updateKotlinAbi")

            val dumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(dumpFile)

            val tab = "\t"
            val expectedDump = """
                public final class AndroidClass {
                ${tab}public fun <init> ()V
                }

                public final class CommonClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedDump, dumpFile)
        }
    }

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88, additionalVersions = [TestVersions.AGP.AGP_811])
    @GradleAndroidTest
    fun testMixed(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidKmpLibraryProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                }
            }

            kotlinSourcesDir("commonMain").source("CommonClass.kt") { "class CommonClass" }
            kotlinSourcesDir("androidMain").source("AndroidClass.kt") { "class AndroidClass" }
            kotlinSourcesDir("jvmMain").source("JvmClass.kt") { "class JvmClass" }

            build("updateKotlinAbi")

            val tab = "\t"

            val androidDumpFile = referenceMixedAndroidDumpFile()
            assertFileExists(androidDumpFile)
            val expectedAndroidDump = """
                public final class AndroidClass {
                ${tab}public fun <init> ()V
                }

                public final class CommonClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedAndroidDump, androidDumpFile)

            val jvmDumpFile = referenceMixedJvmDumpFile()
            assertFileExists(jvmDumpFile)
            val expectedJvmDump = """
                public final class CommonClass {
                ${tab}public fun <init> ()V
                }

                public final class JvmClass {
                ${tab}public fun <init> ()V
                }


            """.trimIndent()
            assertDumpsEqual(expectedJvmDump, jvmDumpFile)
        }
    }

}
