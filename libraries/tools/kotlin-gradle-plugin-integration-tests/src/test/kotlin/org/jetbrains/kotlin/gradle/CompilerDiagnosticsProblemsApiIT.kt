/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.jetbrains.kotlin.gradle.testbase.assertProblemsReportContainsDiagnostic

@DisplayName("Compiler Diagnostics Problems API tests")
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_11, // This test asserts via the HTML problems report, which is available from Gradle 8.11+
    additionalVersions = [
        TestVersions.Gradle.G_8_13
    ]
)
@OtherGradlePluginTests
class CompilerDiagnosticsProblemsApiIT : KGPBaseTest() {

    @GradleTest
    @DisplayName("Test compiler warning appears in build output via Problems API renderer")
    fun testCompilerWarningAppearsInProblemsReport(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
            }

            kotlinSourcesDir().source("deprecatedUsage.kt") {
                """
                @Deprecated("Use newFunction instead")
                fun oldFunction(): String = "old"

                fun callerOfOldFunction(): String = oldFunction()
                """.trimIndent()
            }

            build("compileKotlin") {
                assertOutputContains(Regex("""file:///.*deprecatedUsage\.kt"""))
                assertOutputContainsAny("is deprecated", "Use newFunction instead")
                assertProblemsReportContainsDiagnostic(
                    "compiler-warning",
                    "Use newFunction instead",
                    gradleVersion,
                )
            }
        }
    }

    @GradleTest
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_11,
        additionalVersions = [TestVersions.Gradle.G_8_13, TestVersions.Gradle.G_9_3],
    )
    @DisplayName("Test compiler warning is not duplicated in build output by Problems API renderer")
    fun testCompilerWarningNotDuplicatedInOutput(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
            }

            kotlinSourcesDir().source("deprecatedUsage.kt") {
                """
                @Deprecated("Use newFunction instead")
                fun oldFunction(): String = "old"

                fun callerOfOldFunction(): String = oldFunction()
                """.trimIndent()
            }

            // --warning-mode=all causes Gradle to render Problems API entries as "Problem found:" in console.
            // With the fix, the standard "w:" log is suppressed to avoid duplication —
            // the warning should only appear once, via Gradle's Problems API rendering.
            //
            // Set warning mode via gradle.properties to avoid the test infra's
            // GradleWarningsDetectorPlugin assertion that requires Gradle deprecation warnings.
            gradleProperties.append("\norg.gradle.warning.mode=all\n")
            build("compileKotlin") {
                // The warning should appear in console via Problems API rendering
                assertOutputContainsAny("is deprecated", "Use newFunction instead")

                // The warning should be reported to the Problems API HTML report
                assertProblemsReportContainsDiagnostic(
                    "compiler-warning",
                    "Use newFunction instead",
                    gradleVersion,
                )

                // The warning message should appear exactly once — not duplicated
                assertOutputContainsExactlyTimes("Use newFunction instead", expectedCount = 1)
            }
        }
    }

    @GradleTest
    @DisplayName("Test compiler error appears in build output via Problems API renderer")
    fun testCompilerErrorAppearsInProblemsReport(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
            }

            kotlinSourcesDir().source("errorFile.kt") {
                """
                fun broken(): String = unresolvedReference()
                """.trimIndent()
            }

            buildAndFail("compileKotlin") {
                assertOutputContains(Regex("""file:///.*errorFile\.kt"""))
                assertOutputContains(Regex("""[Uu]nresolved reference"""))
                assertProblemsReportContainsDiagnostic(
                    "compiler-error",
                    "nresolved reference",
                    gradleVersion,
                )
            }
        }
    }
}
