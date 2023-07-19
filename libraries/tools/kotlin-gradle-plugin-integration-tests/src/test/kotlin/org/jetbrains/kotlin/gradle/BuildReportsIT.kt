/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internal.build.metrics.GradleBuildMetricsData
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.ObjectInputStream
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertTrue
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.testbase.TestVersions.ThirdPartyDependencies.GRADLE_ENTERPRISE_PLUGIN_VERSION

@DisplayName("Build reports")
@JvmGradlePluginTests
class BuildReportsIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            buildReport = listOf(BuildReportType.FILE)
        )

    private val GradleProject.reportFile: Path
        get() = projectPath.getSingleFileInDir("build/reports/kotlin-build")

    @DisplayName("Build report is created")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildReportSmokeTest(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble") {
                assertBuildReportPathIsPrinted()
            }

            build("clean", "assemble") {
                assertBuildReportPathIsPrinted()
            }
        }
    }

    @DisplayName("Build report output property accepts only certain values")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildReportOutputProperty(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildAndFail("assemble", "-Pkotlin.build.report.output=file,invalid") {
                assertOutputContains("Unknown output type:")
            }
        }
    }

    @DisplayName("Build metrics produces valid report")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildMetricsSmokeTest(gradleVersion: GradleVersion) {
        testBuildReportInFile("simpleProject", "assemble", gradleVersion)
    }

    @DisplayName("Build metrics produces valid report for mpp-jvm")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildMetricsForMppJvm(gradleVersion: GradleVersion) {
        testBuildReportInFile("mppJvmWithJava", "assemble", gradleVersion)
    }

    @DisplayName("Build metrics produces valid report for mpp-js")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildMetricsForMppJs(gradleVersion: GradleVersion) {
        testBuildReportInFile("kotlin-js-package-module-name", "assemble", gradleVersion)
    }

    @DisplayName("Build metrics produces valid report for JS project")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildMetricsForJsProject(gradleVersion: GradleVersion) {
        testBuildReportInFile("kotlin-js-plugin-project", "compileKotlinJs", gradleVersion,
                              languageVersion = KotlinVersion.KOTLIN_1_7.version)
    }

    private fun testBuildReportInFile(project: String, task: String, gradleVersion: GradleVersion,
                                      languageVersion: String = KotlinVersion.KOTLIN_2_0.version) {
        project(project, gradleVersion) {
            build(task) {
                assertBuildReportPathIsPrinted()
            }
            //Should contains build metrics for all compile kotlin tasks
            validateBuildReportFile(KotlinVersion.DEFAULT.version)
        }

        project(project, gradleVersion, buildOptions = defaultBuildOptions.copy(languageVersion = languageVersion)) {
            build(task, buildOptions = buildOptions.copy(languageVersion = languageVersion)) {
                assertBuildReportPathIsPrinted()
            }
            //Should contains build metrics for all compile kotlin tasks
            validateBuildReportFile(languageVersion)
        }
    }

    private fun TestProject.validateBuildReportFile(kotlinLanguageVersion: String) {
        assertFileContains(
            reportFile,
            "Time metrics:",
            "Run compilation:",
            "Incremental compilation in daemon:",
            "Size metrics:",
            "Total size of the cache directory:",
            "Total compiler iteration:",
            "ABI snapshot size:",
            //for non-incremental builds
            "Build attributes:",
            "REBUILD_REASON:",
            //gc metrics
            "GC count:",
            "GC time:",
            //task info
            "Task info:",
            "Kotlin language version: $kotlinLanguageVersion",
        )
    }

    @DisplayName("Compiler build metrics report is produced")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testCompilerBuildMetricsSmokeTest(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble") {
                assertBuildReportPathIsPrinted()
            }
            assertFileContains(
                reportFile,
                "Compiler code analysis:",
                "Compiler code generation:",
                "Compiler initialization time:",
            )
        }
    }

    @DisplayName("with no kotlin task executed")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testFileReportWithoutKotlinTask(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble", "--dry-run") {
                assertBuildReportPathIsPrinted()
            }
            assertFileContains(
                reportFile,
                "No Kotlin task was run",
            )
        }
    }

    @DisplayName("validation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testSingleBuildMetricsFileValidation(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildAndFail(
                "compileKotlin", "-Pkotlin.build.report.output=SINGLE_FILE",
            ) {
                assertOutputContains("Can't configure single file report: 'kotlin.build.report.single_file' property is mandatory")
            }
        }
    }

    @DisplayName("deprecated property")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testDeprecatedAndNewSingleBuildMetricsFile(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val newMetricsPath = projectPath.resolve("metrics.bin")
            val deprecatedMetricsPath = projectPath.resolve("deprecated_metrics.bin")
            build(
                "compileKotlin", "-Pkotlin.build.report.single_file=${newMetricsPath.pathString}",
                "-Pkotlin.internal.single.build.metrics.file=${deprecatedMetricsPath.pathString}"
            )
            assertTrue { deprecatedMetricsPath.exists() }
            assertTrue { newMetricsPath.notExists() }
        }
    }

    @DisplayName("smoke")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testSingleBuildMetricsFileSmoke(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val metricsFile = projectPath.resolve("metrics.bin").toFile()
            build(
                "compileKotlin",
                "-Pkotlin.build.report.output=SINGLE_FILE",
                "-Pkotlin.build.report.single_file=${metricsFile.absolutePath}"
            )

            assertTrue { metricsFile.exists() }
            // test whether we can deserialize data from the file
            ObjectInputStream(metricsFile.inputStream().buffered()).use { input ->
                input.readObject() as GradleBuildMetricsData
            }
        }
    }

    @DisplayName("custom value limit")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testCustomValueLimitForBuildScan(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            build(
                "compileKotlin",
                "-Pkotlin.build.report.output=BUILD_SCAN",
                "-Pkotlin.build.report.build_scan.custom_values_limit=0",
                "--scan"
            ) {
                assertOutputContains("Can't add any more custom values into build scan")
            }
        }
    }

    @DisplayName("build scan listener lazy initialisation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildScanListenerLazyInitialisation(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            build(
                "compileKotlin",
                "-Pkotlin.build.report.output=BUILD_SCAN",
                "-Pkotlin.build.report.build_scan.custom_values_limit=0",
            ) {
                assertOutputDoesNotContain("Can't add any more custom values into build scan")
            }
        }
    }

    private val kotlinErrorPath = ".gradle/kotlin/errors"

    @DisplayName("Error file is created")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testErrorsFileSmokeTest(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {

            val lookupsTab = projectPath.resolve("build/kotlin/compileKotlin/cacheable/caches-jvm/lookups/lookups.tab")
            buildGradle.appendText(
                """
                    tasks.named("compileKotlin") {
                        doLast {
                            new File("${lookupsTab.toUri().path}").write("Invalid contents")
                        }
                    }
                """.trimIndent()
            )
            build("compileKotlin") {
                assertTrue { projectPath.resolve(kotlinErrorPath).listDirectoryEntries().isEmpty() }
            }
            val kotlinFile = kotlinSourcesDir().resolve("helloWorld.kt")
            kotlinFile.modify { it.replace("ArrayList", "skjfghsjk") }
            buildAndFail("compileKotlin") {
                val buildErrorDir = projectPath.resolve(kotlinErrorPath).toFile()
                val files = buildErrorDir.listFiles()
                assertTrue { files?.first()?.exists() ?: false }
                files?.first()?.bufferedReader().use { reader ->
                    val kotlinVersion = reader?.readLine()
                    assertTrue("kotlin version should be in the error file") {
                        kotlinVersion != null && kotlinVersion.trim().equals("kotlin version: ${buildOptions.kotlinVersion}")
                    }
                    val errorMessage = reader?.readLine()
                    assertTrue("Error message should start with 'error message: ' to parse it on IDEA side") {
                        errorMessage != null && errorMessage.trim().startsWith("error message:")
                    }
                }

            }
        }
    }

    @DisplayName("Error file should not contain compilation exceptions")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testErrorsFileWithCompilationError(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("compileKotlin") {
                assertTrue { projectPath.resolve(kotlinErrorPath).listDirectoryEntries().isEmpty() }
            }
            val kotlinFile = kotlinSourcesDir().resolve("helloWorld.kt")
            kotlinFile.modify { it.replace("ArrayList", "skjfghsjk") }
            buildAndFail("compileKotlin") {
                assertTrue { projectPath.resolve(kotlinErrorPath).listDirectoryEntries().isEmpty() }
            }
        }
    }

    @DisplayName("build scan metrics validation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildScanMetricsValidation(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildAndFail(
                "compileKotlin", "-Pkotlin.build.report.output=BUILD_SCAN", "-Pkotlin.build.report.build_scan.metrics=unknown_prop"
            ) {
                assertOutputContains("Unknown metric: 'unknown_prop', list of available metrics")
            }
        }
    }

    @DisplayName("build reports work with init script")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildReportsWithInitScript(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            gradleProperties.modify { "$it\nkotlin.build.report.output=BUILD_SCAN,FILE\n" }

            val initScript = projectPath.resolve("init.gradle").createFile()
            initScript.modify {
                """
                    initscript {
                        repositories {
                            maven { url = 'https://plugins.gradle.org/m2/' }
                        }

                        dependencies {
                            classpath 'com.gradle:gradle-enterprise-gradle-plugin:$GRADLE_ENTERPRISE_PLUGIN_VERSION'
                        }
                    }

                    beforeSettings {
                        it.pluginManager.apply(com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin)
                    }
                """.trimIndent()
            }

            build(
                "compileKotlin",
                "-I", "init.gradle",
            )

            build(
                "compileKotlin",
                "-I", "init.gradle",
                enableBuildScan = true,
            )
        }
    }

}
