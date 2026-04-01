/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.pathString
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("Problems API tests")
@GradleTestVersions(
    minVersion = TestVersions.Gradle.G_8_6 // Problems Api available only from Gradle 8.6
)
@OtherGradlePluginTests
class ProblemsApiIT : KGPBaseTest() {
    @GradleTest
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_11 // html reports available only from Gradle 8.11
    )
    @DisplayName("Test problems emitted")
    fun testProblemsEmitted(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val deprecatedMetricsPath = projectPath.resolve("deprecated_metrics.bin")
            build(
                "compileKotlin",
                "-Pkotlin.build.report.dir=${projectPath.resolve("reports").pathString}",
                "-Pkotlin.internal.single.build.metrics.file=${deprecatedMetricsPath.pathString}"
            ) {

                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties, "kotlin.internal.single.build.metrics.file")
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties, "kotlin.build.report.dir")
                assertHasDiagnostic(KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed)

                val problemReportUrl = ProblemsApiTestUtils.extractProblemReportUrl(output)
                assertNotNull(problemReportUrl, "Problem report URL not found in the output")
                val scriptContent = ProblemsApiTestUtils.readProblemReportContent(problemReportUrl)
                val problemReport = ProblemsApiTestUtils.parseProblemReportFromScript(scriptContent, gradleVersion)

                val expectedProblems = if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_4)) {
                    buildList {
                        add(
                            ProblemsApiDiagnosticG811(
                                locations = listOf(ProblemsApiLocation("org.jetbrains.kotlin.jvm")),
                                problem = listOf(TextWrapper("Deprecated Gradle Property 'kotlin.internal.single.build.metrics.file' Used")),
                                severity = "ERROR",
                                problemDetails = listOf(TextWrapper("The `kotlin.internal.single.build.metrics.file` deprecated property is used in your build.")),
                                contextualLabel = "Deprecated Gradle Property 'kotlin.internal.single.build.metrics.file' Used",
                                problemId = listOf(
                                    ProblemIdentifier("kgp:deprecation", "Kotlin Gradle Plugin Deprecation"),
                                    ProblemIdentifier("kotlin", "Kotlin"),
                                    ProblemIdentifier(
                                        "deprecated-warning-gradle-properties",
                                        "Deprecated Gradle Property 'kotlin.internal.single.build.metrics.file' Used"
                                    )
                                ),
                                solutions = listOf(listOf(TextWrapper("It is unsupported, please stop using it.")))
                            )
                        )

                        add(
                            ProblemsApiDiagnosticG811(
                                locations = listOf(ProblemsApiLocation("org.jetbrains.kotlin.jvm")),
                                problem = listOf(TextWrapper("Deprecated Gradle Property 'kotlin.build.report.dir' Used")),
                                severity = "ERROR",
                                problemDetails = listOf(TextWrapper("The `kotlin.build.report.dir` deprecated property is used in your build.")),
                                contextualLabel = "Deprecated Gradle Property 'kotlin.build.report.dir' Used",
                                problemId = listOf(
                                    ProblemIdentifier("kgp:deprecation", "Kotlin Gradle Plugin Deprecation"),
                                    ProblemIdentifier("kotlin", "Kotlin"),
                                    ProblemIdentifier(
                                        "deprecated-warning-gradle-properties",
                                        "Deprecated Gradle Property 'kotlin.build.report.dir' Used"
                                    )
                                ),
                                solutions = listOf(listOf(TextWrapper("It is unsupported, please stop using it.")))
                            )
                        )

                        add(
                            ProblemsApiDiagnosticG811(
                                locations = listOf(ProblemsApiLocation("org.jetbrains.kotlin.jvm")),
                                problem = listOf(TextWrapper("Usage of Internal Kotlin Gradle Plugin Properties Detected")),
                                severity = "ERROR",
                                problemDetails = listOf(TextWrapper("ATTENTION! This build uses the following Kotlin Gradle Plugin properties:\n\nkotlin.internal.compiler.arguments.log.level\nkotlin.internal.diagnostics.showStacktrace\nkotlin.internal.diagnostics.useParsableFormatting\n\nInternal properties are not recommended for production use.\nStability and future compatibility of the build is not guaranteed.")),
                                contextualLabel = "Usage of Internal Kotlin Gradle Plugin Properties Detected",
                                problemId = listOf(
                                    ProblemIdentifier("kgp:misconfiguration", "Kotlin Gradle Plugin Misconfiguration"),
                                    ProblemIdentifier("kotlin", "Kotlin"),
                                    ProblemIdentifier(
                                        "internal-kotlin-gradle-plugin-properties-used",
                                        "Usage of Internal Kotlin Gradle Plugin Properties Detected"
                                    )
                                ),
                                solutions = listOf(listOf(TextWrapper("Please consider using the public API instead of internal properties.")))
                            )
                        )
                    }
                } else {
                    buildList {
                        add(
                            ProblemsApiDiagnosticG94(
                                locations = listOf(ProblemsApiLocation("org.jetbrains.kotlin.jvm")),
                                problem = emptyList(),
                                severity = "ERROR",
                                problemDetails = "The `kotlin.internal.single.build.metrics.file` deprecated property is used in your build.",
                                contextualLabel = "Deprecated Gradle Property 'kotlin.internal.single.build.metrics.file' Used",
                                problemId = listOf(
                                    ProblemIdentifier("kotlin", "Kotlin"),
                                    ProblemIdentifier("kgp:deprecation", "Kotlin Gradle Plugin Deprecation"),
                                    ProblemIdentifier(
                                        "deprecated-warning-gradle-properties",
                                        "Deprecated Gradle Property 'kotlin.internal.single.build.metrics.file' Used"
                                    )
                                ),
                                solutions = listOf("It is unsupported, please stop using it.")
                            )
                        )

                        add(
                            ProblemsApiDiagnosticG94(
                                locations = listOf(ProblemsApiLocation("org.jetbrains.kotlin.jvm")),
                                problem = emptyList(),
                                severity = "ERROR",
                                problemDetails = "The `kotlin.build.report.dir` deprecated property is used in your build.",
                                contextualLabel = "Deprecated Gradle Property 'kotlin.build.report.dir' Used",
                                problemId = listOf(
                                    ProblemIdentifier("kotlin", "Kotlin"),
                                    ProblemIdentifier("kgp:deprecation", "Kotlin Gradle Plugin Deprecation"),
                                    ProblemIdentifier(
                                        "deprecated-warning-gradle-properties",
                                        "Deprecated Gradle Property 'kotlin.build.report.dir' Used"
                                    )
                                ),
                                solutions = listOf("It is unsupported, please stop using it.")
                            )
                        )

                        add(
                            ProblemsApiDiagnosticG94(
                                locations = listOf(ProblemsApiLocation("org.jetbrains.kotlin.jvm")),
                                problem = emptyList(),
                                severity = "ERROR",
                                problemDetails = "ATTENTION! This build uses the following Kotlin Gradle Plugin properties:\n\nkotlin.internal.compiler.arguments.log.level\nkotlin.internal.diagnostics.showStacktrace\nkotlin.internal.diagnostics.useParsableFormatting\n\nInternal properties are not recommended for production use.\nStability and future compatibility of the build is not guaranteed.",
                                contextualLabel = "Usage of Internal Kotlin Gradle Plugin Properties Detected",
                                problemId = listOf(
                                    ProblemIdentifier("kotlin", "Kotlin"),
                                    ProblemIdentifier("kgp:misconfiguration", "Kotlin Gradle Plugin Misconfiguration"),
                                    ProblemIdentifier(
                                        "internal-kotlin-gradle-plugin-properties-used",
                                        "Usage of Internal Kotlin Gradle Plugin Properties Detected"
                                    )
                                ),
                                solutions = listOf("Please consider using the public API instead of internal properties.")
                            )
                        )
                    }
                }

                val actualProblems = problemReport.diagnostics.filter {
                    it.problemId.all { id ->
                        id.name != "OldNativeVersionDiagnostic" && id.name != "deprecated-gradle-version-warning"
                    }
                }

                assertEquals(
                    expectedProblems,
                    actualProblems
                )
            }
        }
    }
}
