/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.STRONG_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.test.TestDataAssertions.assertEqualsToFile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@MppGradlePluginTests
class MppDiagnosticsIt : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            compilerArgumentsLogLevel = null,
            ignoreWarningModeSeverityOverride = true
        )

    @GradleTest
    fun testDiagnosticsRenderingSmoke(gradleVersion: GradleVersion) {
        project("diagnosticsRenderingSmoke", gradleVersion) {
            build {
                // with isolated projects enabled, the order of diagnostics blocks is non-deterministic
                // and depends on the subproject evaluation order, so the easiest way to assert that all diagnostics blocks are present
                // is to compare blocks ignoring the order
                assertBlocksEqual(
                    expectedOutputFile().extractBlocksFromExpectedOutput(),
                    extractProjectsAndTheirDiagnosticsInBlocks(),
                )
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
                assertEqualsToFile(
                    expectedOutputFile("assemble"),
                    filteredErrorDiagnosticBuildFailsOutput()
                )
            }

            // 'clean', not directly relevant to Kotlin tasks: build is OK
            build("clean") {
                assertEqualsToFile(
                    expectedOutputFile("clean"),
                    filteredErrorDiagnosticBuildFailsOutput()
                )
            }

            // Custom task, irrelevant to Kotlin tasks: build is OK
            build("myTask", "--rerun-tasks") {
                assertEqualsToFile(
                    expectedOutputFile("customTask"),
                    filteredErrorDiagnosticBuildFailsOutput()
                )
            }

            // commonizer task: build is OK (otherwise IDE will be bricked)
            build("commonize") {
                assertEqualsToFile(
                    expectedOutputFile("commonize"),
                    filteredErrorDiagnosticBuildFailsOutput()
                )
            }
        }
    }

    @GradleTest
    @TestMetadata("errorDiagnosticBuildFails")
    fun testErrorDiagnosticBuildFailsWithConfigurationCache(gradleVersion: GradleVersion) {
        project("errorDiagnosticBuildFails", gradleVersion) {
            buildAndFail("assemble") {
                assertConfigurationCacheStored()
                assertEqualsToFile(
                    expectedOutputFile("assemble"),
                    filteredErrorDiagnosticBuildFailsOutput()
                )
            }

            // fails again
            buildAndFail("assemble") {
                assertConfigurationCacheReused()
                assertEqualsToFile(
                    expectedOutputFile("assemble-cache-reused"),
                    filteredErrorDiagnosticBuildFailsOutput()
                )
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
        project(
            "kt64121",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
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
        project(
            "errorsFailOnlyRelevantProjects",
            gradleVersion,
            // CC should be explicitly disabled because it hides the warning on subsequent builds: KT-75750
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED),
        ) {
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
        project(
            "diagnosticsRenderingWithStacktraceOption",
            gradleVersion,
            // CC should be explicitly disabled because it hides the warning on subsequent builds: KT-75750
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED),
        ) {
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

    internal object StrongWarningDiagnostic : ToolingDiagnosticFactory(STRONG_WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() =
            build {
                title("Foo")
                    .descriptionBuilder {
                        if (org.slf4j.LoggerFactory.getLogger(StrongWarningDiagnostic::class.java).isInfoEnabled) {
                            info
                        } else {
                            standard
                        }
                    }
                    .solution("baz")
            }

        val standard = "StrongWarningDiagnostic_STANDARD"
        val info = "StrongWarningDiagnostic_INFO"
    }
    @GradleTest
    fun testStrongWarningDiagnostic(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                }
                project.reportDiagnostic(StrongWarningDiagnostic())
            }
            build(
                ":checkKotlinGradlePluginConfigurationErrors",
                buildOptions = defaultBuildOptions.copy(logLevel = org.gradle.api.logging.LogLevel.LIFECYCLE),
            ) {
                assertHasDiagnostic(StrongWarningDiagnostic)
                assertTasksExecuted(":checkKotlinGradlePluginConfigurationErrors")
                assertOutputContains(StrongWarningDiagnostic.standard)
            }
            build(
                ":checkKotlinGradlePluginConfigurationErrors",
                buildOptions = defaultBuildOptions.copy(logLevel = org.gradle.api.logging.LogLevel.INFO),
            ) {
                assertConfigurationCacheReused()
                assertHasDiagnostic(StrongWarningDiagnostic)
                assertTasksExecuted(":checkKotlinGradlePluginConfigurationErrors")
                assertOutputContains(StrongWarningDiagnostic.info)
            }
        }
    }

    @DisplayName("checkKotlinGradlePluginConfigurationErrors does not cause a false positive configuration cache warning")
    @GradleTest
    fun testKt63165(gradleVersion: GradleVersion) {
        // the false positive warning is https://github.com/gradle/gradle/issues/22481
        project("errorDiagnosticUpToDateIfNoErrors", gradleVersion) {
            //language=Gradle
            settingsGradleKts.appendText(
                """

                enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
                """.trimIndent()
            )
            settingsGradleKts.modify {
                val pluginApplyString = "id(\"org.jetbrains.kotlin.test.gradle-warnings-detector\")"
                val startingIndex = it.lastIndexOf(pluginApplyString)
                // workaround for a Gradle bug: https://github.com/gradle/gradle/issues/28533
                it.replaceRange(startingIndex, startingIndex + pluginApplyString.length, "")
            }
            build("assemble") {
                // expect no deprecation warnings failing the build
                assertTasksSkipped(":checkKotlinGradlePluginConfigurationErrors")
            }
        }
    }

    private fun TestProject.expectedOutputFile(suffix: String? = null): File {
        val suffixIfAny = if (suffix != null) "-$suffix" else ""
        return projectPath.resolve("expectedOutput$suffixIfAny.txt").toFile()
    }

    /**
     * Filters out DisabledNativeTargetTaskWarning from diagnostics output.
     * This warning is host-dependent and is already tested in GeneralNativeIT.
     */
    private fun BuildResult.filteredErrorDiagnosticBuildFailsOutput(): String {
        val filteredBlocks = extractProjectsAndTheirDiagnosticsInBlocks().filterNot {
            KotlinToolingDiagnostics.DisabledNativeTargetTaskWarning.id in it
        }
        return filteredBlocks.joinToString(separator = "\n").trim()
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
