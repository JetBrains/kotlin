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
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(compilerArgumentsLogLevel = null)

    @GradleTest
    fun testDiagnosticsRenderingSmoke(gradleVersion: GradleVersion) {
        project("diagnosticsRenderingSmoke", gradleVersion) {
            build {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testDeprecatedMppProperties(gradleVersion: GradleVersion) {
        for (deprecatedProperty in this.deprecatedFlags) {
            project("mppDeprecatedProperties", gradleVersion) {
                checkDeprecatedProperties(isDeprecationExpected = false)

                this.gradleProperties.appendText(
                    "${deprecatedProperty.key}=${deprecatedProperty.value}${System.lineSeparator()}"
                )
                checkDeprecatedProperties(isDeprecationExpected = true)

                // remove the MPP plugin from the top-level project and check the warnings are still reported in subproject
                this.buildGradleKts.writeText("")
                checkDeprecatedProperties(isDeprecationExpected = true)

                this.gradleProperties.appendText("kotlin.internal.suppressGradlePluginErrors=PreHMPPFlagsError${System.lineSeparator()}")
                checkDeprecatedProperties(isDeprecationExpected = false)
            }
        }
    }

    @GradleTest
    fun testErrorDiagnosticBuildFails(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildFails", gradleVersion) {
            // 'assemble' (triggers compileKotlin-tasks indirectly): fail
            buildAndFail("assemble") {
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirDiagnostics())
            }

            // 'clean', not directly relevant to Kotlin tasks: build is OK
            build("clean") {
                assertEqualsToFile(expectedOutputFile("clean"), extractProjectsAndTheirDiagnostics())
            }

            // Custom task, irrelevant to Kotlin tasks: build is OK
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(expectedOutputFile("customTask"), extractProjectsAndTheirDiagnostics())
            }

            // commonizer task: build is OK (otherwise IDE will be bricked)
            build("commonize") {
                assertEqualsToFile(expectedOutputFile("commonize"), extractProjectsAndTheirDiagnostics())
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
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirDiagnostics())
            }

            // fails again
            buildAndFail("assemble", buildOptions = buildOptions.copy(configurationCache = true)) {
                assertConfigurationCacheReused()
                assertEqualsToFile(expectedOutputFile("assemble-cache-reused"), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testErrorDiagnosticBuildSucceeds(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildSucceeds", gradleVersion) {
            build("assemble") {
                assertEqualsToFile(expectedOutputFile("assemble"), extractProjectsAndTheirDiagnostics())
            }
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(expectedOutputFile("customTask"), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testSuppressGradlePluginErrors(gradleVersion: GradleVersion) {
        project("suppressGradlePluginErrors", gradleVersion) {
            // build succeeds
            build("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testKt64121(gradleVersion: GradleVersion) {
        project("kt64121", gradleVersion) {
            build("assemble")
        }
    }

    @GradleTest
    fun testSuppressGradlePluginWarnings(gradleVersion: GradleVersion) {
        project("suppressGradlePluginWarnings", gradleVersion) {
            build("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testSuppressGradlePluginFatals(gradleVersion: GradleVersion) {
        project("suppressGradlePluginFatals", gradleVersion) {
            buildAndFail("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testErrorsFailOnlyRelevantProjects(gradleVersion: GradleVersion) {
        project("errorsFailOnlyRelevantProjects", gradleVersion) {
            buildAndFail("brokenProjectA:assemble") {
                assertEqualsToFile(expectedOutputFile("brokenA"), extractProjectsAndTheirDiagnostics())
            }

            buildAndFail("brokenProjectB:assemble") {
                assertEqualsToFile(expectedOutputFile("brokenB"), extractProjectsAndTheirDiagnostics())
            }

            build("healthyProject:assemble") {
                assertEqualsToFile(expectedOutputFile("healthy"), extractProjectsAndTheirDiagnostics())
            }

            // Turn off parallel execution so that order of execution (and therefore the testdata) is stable
            buildAndFail("assemble", buildOptions = buildOptions.copy(parallel = false)) {
                assertEqualsToFile(expectedOutputFile("root"), extractProjectsAndTheirDiagnostics())
            }

            buildAndFail("assemble", "--continue", buildOptions = buildOptions.copy(parallel = false)) {
                assertEqualsToFile(expectedOutputFile("root-with-continue"), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testEarlyTasksMaterializationDoesntBreakReports(gradleVersion: GradleVersion) {
        project("earlyTasksMaterializationDoesntBreakReports", gradleVersion) {
            buildAndFail("assemble") {
                assertEqualsToFile(expectedOutputFile(), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testDiagnosticsRenderingWithStacktraceOption(gradleVersion: GradleVersion) {
        project("diagnosticsRenderingWithStacktraceOption", gradleVersion) {
            // KGP sets showDiagnosticsStacktrace=false and --full-stacktrace by default in tests,
            // need to override that to mimic real-life scenarios
            val options = buildOptions.copy(showDiagnosticsStacktrace = null, stacktraceMode = null)
            build("help", buildOptions = options) {
                assertEqualsToFile(expectedOutputFile("without-stacktrace"), extractProjectsAndTheirDiagnostics())
            }

            build("help", "--stacktrace", buildOptions = options) {
                assertEqualsToFile(expectedOutputFile("with-stacktrace"), extractProjectsAndTheirDiagnostics())
            }

            build("help", "--full-stacktrace", buildOptions = options) {
                assertEqualsToFile(expectedOutputFile("with-full-stacktrace"), extractProjectsAndTheirDiagnostics())
            }
        }
    }

    @GradleTest
    fun testErrorDiagnosticUpToDateIfNoErrors(gradleVersion: GradleVersion) {
        project("errorDiagnosticUpToDateIfNoErrors", gradleVersion) {
            build("assemble") {
                assertTasksSkipped(":checkKotlinGradlePluginConfigurationErrors")
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
                output.assertHasDiagnostic(KotlinToolingDiagnostics.PreHMPPFlagsError)
            else
                output.assertNoDiagnostic(KotlinToolingDiagnostics.PreHMPPFlagsError)
        }
    }

    private val deprecatedFlags: Map<String, String>
        get() = mapOf(
            "kotlin.mpp.enableGranularSourceSetsMetadata" to "true",
            "kotlin.mpp.enableCompatibilityMetadataVariant" to "false",
            "kotlin.internal.mpp.hierarchicalStructureByDefault" to "true",
            "kotlin.mpp.hierarchicalStructureSupport" to "true",
            "kotlin.native.enableDependencyPropagation" to "false",
        )
}
