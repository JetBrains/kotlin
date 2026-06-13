/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.abiValidation
import org.jetbrains.kotlin.gradle.abi.utils.referenceJvmDumpFile
import org.jetbrains.kotlin.gradle.abi.utils.referenceKlibDumpFile
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.test.assertContains
import kotlin.test.assertEquals

@MppGradlePluginTests
class AbiValidationKmpMavenPublicationsIT : KGPBaseTest() {
    @GradleTest
    fun testNoPublishPluginInKmp(
        gradleVersion: GradleVersion,
    ) {
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion,
        ) {
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                    linuxX64()
                }
            }
            abiValidation {
                binariesSource.set(BinariesSource.MAVEN_PUBLICATIONS)
            }
            buildAndFail("updateKotlinAbi")
        }
    }

    @GradleTest
    fun testSame(
        gradleVersion: GradleVersion,
    ) {
        val compilationsKlibDump: String
        val compilationsJvmDump: String
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion
        ).run {
            addSampleSource()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                    linuxX64()
                    // check support of custom target name
                    mingwX64("MINGW_CUSTOM_NAME")
                }
            }
            abiValidation()

            build("updateKotlinAbi")
            compilationsKlibDump = referenceKlibDumpFile().readText()
            compilationsJvmDump = referenceJvmDumpFile().readText()
        }

        val klibDumpFromPublication: String
        val jvmDumpFromPublication: String
        project(
            "base-kotlin-multiplatform-library",
            gradleVersion
        ).run {
            plugins {
                id("org.gradle.maven-publish")
            }

            addSampleSource()
            buildScriptInjection {
                with(kotlinMultiplatform) {
                    jvm()
                    linuxX64()
                    // check support of custom target name
                    mingwX64("MINGW_CUSTOM_NAME")
                }
            }
            abiValidation {
                binariesSource.set(BinariesSource.MAVEN_PUBLICATIONS)
            }

            build("updateKotlinAbi")
            klibDumpFromPublication = referenceKlibDumpFile().readText()
            jvmDumpFromPublication = referenceJvmDumpFile().readText()
        }


        assertEquals(compilationsKlibDump, klibDumpFromPublication)
        assertEquals(compilationsJvmDump, jvmDumpFromPublication)
    }

    @GradleAndroidTest
    fun testAndroidNotSupported(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "base-kotlin-multiplatform-android-library",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ).run {
            plugins {
                id("org.gradle.maven-publish")
            }

            addSampleSource()
            buildScriptInjection {
                applyDefaultAndroidLibraryConfiguration()

                with(kotlinMultiplatform) {
                    jvm()
                    linuxX64()
                    androidTarget()
                }
            }
            abiValidation {
                binariesSource.set(BinariesSource.MAVEN_PUBLICATIONS)
            }

            buildAndFail("updateKotlinAbi") {
                assertContains(output, "Android targets are not supported by ABI validation when Maven binary sources mode is enabled")
            }
        }
    }
}


private fun GradleProject.addSampleSource() {
    kotlinSourcesDir("commonMain").source("org/jetbrains/tests") { SOURCE_FILE }
}

private val SOURCE_FILE = """
    fun function() {
        println("Hello, world!")
    }
    
    class Foo {
        fun bar() {
        }
    }
""".trimIndent()

