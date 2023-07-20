/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import java.io.File
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@MppGradlePluginTests
class MppDiagnosticsIt : KGPBaseTest() {
    @GradleTest
    fun testDiagnosticsRenderingSmoke(gradleVersion: GradleVersion) {
        project("diagnosticsRenderingSmoke", gradleVersion) {
            build {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testDeprecatedMppProperties(gradleVersion: GradleVersion) {
        project("mppDeprecatedProperties", gradleVersion) {
            checkDeprecatedProperties(isDeprecationExpected = false)

            this.gradleProperties.appendText(
                defaultFlags.entries.joinToString(
                    prefix = System.lineSeparator(),
                    postfix = System.lineSeparator(),
                    separator = System.lineSeparator(),
                ) { (prop, value) -> "$prop=$value" }
            )
            checkDeprecatedProperties(isDeprecationExpected = true)

            // remove the MPP plugin from the top-level project and check the warnings are still reported in subproject
            this.buildGradleKts.writeText("")
            checkDeprecatedProperties(isDeprecationExpected = true)

            this.gradleProperties.appendText("kotlin.mpp.deprecatedProperties.nowarn=true${System.lineSeparator()}")
            checkDeprecatedProperties(isDeprecationExpected = false)
        }
    }

    @GradleTest
    fun testErrorDiagnosticBuildFails(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildFails", gradleVersion) {
            // 'assemble' (triggers compileKotlin-tasks indirectly): fail
            buildAndFail("assemble") {
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // 'clean', not directly relevant to Kotlin tasks: build is OK
            build("clean") {
                assertEqualsToFile(expectedOutputFile("clean"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // Custom task, irrelevant to Kotlin tasks: build is OK
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(expectedOutputFile("customTask"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // commonizer task: build is OK (otherwise IDE will be bricked)
            build("commonize") {
                assertEqualsToFile(expectedOutputFile("commonize"), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_6)
    @TestMetadata("errorDiagnosticBuildFails")
    fun testErrorDiagnosticBuildFailsWithConfigurationCache(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildFails", gradleVersion) {
            buildAndFail("assemble", buildOptions = buildOptions.copy(configurationCache = true)) {
                assertConfigurationCacheStored()
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // fails again
            buildAndFail("assemble", buildOptions = buildOptions.copy(configurationCache = true)) {
                assertConfigurationCacheReused()
                assertEqualsToFile(expectedOutputFile("assemble-cache-reused"), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testErrorDiagnosticBuildSucceeds(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildSucceeds", gradleVersion) {
            build("assemble") {
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirVerboseDiagnostics())
            }
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(expectedOutputFile("customTask"), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testSuppressGradlePluginErrors(gradleVersion: GradleVersion) {
        project("suppressGradlePluginErrors", gradleVersion) {
            // build succeeds
            build("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testSuppressGradlePluginWarnings(gradleVersion: GradleVersion) {
        project("suppressGradlePluginWarnings", gradleVersion) {
            build("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testSuppressGradlePluginFatals(gradleVersion: GradleVersion) {
        project("suppressGradlePluginFatals", gradleVersion) {
            buildAndFail("assemble") {
                // Gradle 8.0+ for some reason renders exception twice in the build log
                val testDataSuffixIfAny = if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) "gradle-6.8.3" else null

                assertEqualsToFile(expectedOutputFile(testDataSuffixIfAny), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testErrorsFailOnlyRelevantProjects(gradleVersion: GradleVersion) {
        project("errorsFailOnlyRelevantProjects", gradleVersion) {
            buildAndFail("brokenProjectA:assemble") {
                assertEqualsToFile(expectedOutputFile("brokenA"), extractProjectsAndTheirVerboseDiagnostics())
            }

            buildAndFail("brokenProjectB:assemble") {
                assertEqualsToFile(expectedOutputFile("brokenB"), extractProjectsAndTheirVerboseDiagnostics())
            }

            build("healthyProject:assemble") {
                assertEqualsToFile(expectedOutputFile("healthy"), extractProjectsAndTheirVerboseDiagnostics())
            }

            // Turn off parallel execution so that order of execution (and therefore the testdata) is stable
            buildAndFail("assemble", buildOptions = buildOptions.copy(parallel = false)) {
                assertEqualsToFile(expectedOutputFile("root"), extractProjectsAndTheirVerboseDiagnostics())
            }

            buildAndFail("assemble", "--continue", buildOptions = buildOptions.copy(parallel = false)) {
                assertEqualsToFile(expectedOutputFile("root-with-continue"), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    @GradleTest
    fun testEarlyTasksMaterializationDoesntBreakReports(gradleVersion: GradleVersion) {
        project("earlyTasksMaterializationDoesntBreakReports", gradleVersion) {
            buildAndFail("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirVerboseDiagnostics())
            }
        }
    }

    private fun TestProject.expectedOutputFile(suffix: String? = null): File {
        val suffixIfAny = if (suffix != null) "-$suffix" else ""
        return projectPath.resolve("expectedOutput$suffixIfAny.txt").toFile()
    }

    private fun TestProject.checkDeprecatedProperties(isDeprecationExpected: Boolean) {
        build {
            if (isDeprecationExpected)
                output.assertHasDiagnostic(KotlinToolingDiagnostics.HierarchicalMultiplatformFlagsWarning)
            else
                output.assertNoDiagnostic(KotlinToolingDiagnostics.HierarchicalMultiplatformFlagsWarning)
        }
    }

    private val defaultFlags: Map<String, String>
        get() = mapOf(
            "kotlin.mpp.enableGranularSourceSetsMetadata" to "true",
            "kotlin.mpp.enableCompatibilityMetadataVariant" to "false",
            "kotlin.internal.mpp.hierarchicalStructureByDefault" to "true",
            "kotlin.mpp.hierarchicalStructureSupport" to "true",
            "kotlin.native.enableDependencyPropagation" to "false",
        )
}
