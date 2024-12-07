/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppOptionalExpectationIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-with-tests")
    fun testOptionalExpectations(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-with-tests",
            gradleVersion = gradleVersion,
        ) {
            kotlinSourcesDir("commonMain").resolve("Optional.kt").writeText(
                """
                @file:Suppress("OPT_IN_USAGE_ERROR", "EXPERIMENTAL_API_USAGE_ERROR")
                @OptionalExpectation
                expect annotation class Optional(val value: String)
    
                @Optional("optionalAnnotationValue")
                class OptionalCommonUsage
                """.trimIndent()
            )

            build("compileCommonMainKotlinMetadata") {
                val args = extractTaskCompilerArguments(":compileCommonMainKotlinMetadata").split(" ")

                val xCommonSourcesArg = args.singleOrNull { it.startsWith("-Xcommon-sources=") }
                assertNotNull(xCommonSourcesArg, "The compiler args for K2Metadata should contain the -Xcommon-sources argument")

                val xCommonSourcesFiles = xCommonSourcesArg.substringAfter("-Xcommon-sources=").split(",")
                assertTrue("`-Xcommon-sources=` should contain Optional.kt. Actual: $xCommonSourcesFiles") {
                    xCommonSourcesFiles.any { it.endsWith("Optional.kt") }
                }
            }

            build("compileKotlinJvmWithoutJava", "compileKotlinLinux64") {
                assertFileInProjectExists("build/classes/kotlin/jvmWithoutJava/main/OptionalCommonUsage.class")
            }

            val optionalImplText = """
                |@Optional("should fail, see KT-25196")
                |class OptionalPlatformUsage
                |
                """.trimMargin()

            projectPath.resolve("src/jvmWithoutJavaMain/kotlin/OptionalImpl.kt").writeText(optionalImplText)

            buildAndFail("compileKotlinJvmWithoutJava") {
                assertOutputContains("Declaration annotated with '@OptionalExpectation' can only be used in common module sources")
            }

            projectPath.resolve("src/linux64Main/kotlin/OptionalImpl.kt").apply {
                if (!exists()) {
                    createParentDirectories()
                    createFile()
                }
                writeText(optionalImplText)
            }

            buildAndFail("compileKotlinLinux64") {
                assertOutputContains("Declaration annotated with '@OptionalExpectation' can only be used in common module sources")
            }
        }
    }
}
