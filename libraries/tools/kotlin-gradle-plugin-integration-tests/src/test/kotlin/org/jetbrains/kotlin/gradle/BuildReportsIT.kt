/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.statistics.formatSize
import org.jetbrains.kotlin.gradle.internal.build.metrics.GradleBuildMetricsData
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.ObjectInputStream
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertTrue
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.TestVersions.ThirdPartyDependencies.GRADLE_ENTERPRISE_PLUGIN_VERSION
import org.jetbrains.kotlin.gradle.util.BuildOperationRecordImpl
import org.jetbrains.kotlin.gradle.util.readJsonReport
import java.nio.file.Files
import kotlin.streams.asSequence
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        testBuildReportInFile(
            "kotlin-js-plugin-project",
            "compileKotlinJs",
            gradleVersion,
            languageVersion = KotlinVersion.KOTLIN_1_7.version
        )
    }

    private fun testBuildReportInFile(
        project: String,
        task: String,
        gradleVersion: GradleVersion,
        languageVersion: String = KotlinVersion.KOTLIN_2_0.version,
    ) {
        project(project, gradleVersion) {
            build(task) {
                assertBuildReportPathIsPrinted()
            }
            //Should contain build metrics for all compile kotlin tasks
            validateBuildReportFile(KotlinVersion.DEFAULT.version)
        }

        project(project, gradleVersion, buildOptions = defaultBuildOptions.copy(languageVersion = languageVersion)) {
            build(task, buildOptions = buildOptions.copy(languageVersion = languageVersion)) {
                assertBuildReportPathIsPrinted()
            }
            //Should contain build metrics for all compile kotlin tasks
            validateBuildReportFile(languageVersion)
        }
    }

    private fun TestProject.validateBuildReportFile(kotlinLanguageVersion: String) {
        val fileContents = assertFileContains(
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

        fun validateTotalCachesSizeMetric() {
            val cachesDirectories = Files.walk(projectPath).use { files ->
                val knownCachesDirectories = setOf("caches-jvm", "caches-js")
                files.asSequence().filter { Files.isDirectory(it) && it.name in knownCachesDirectories }.toList()
            }
            val actualCacheDirectoriesSize = cachesDirectories.sumOf { files ->
                Files.walk(files).use { cacheFiles ->
                    cacheFiles.filter { Files.isRegularFile(it) }.mapToLong { Files.size(it) }.sum()
                }
            }
            // the first found line of the report should contain a sum of the metric per all the tasks
            val reportedCacheDirectoriesSize = fileContents.lineSequence().find { "Total size of the cache directory:" in it }
                ?.replace("Total size of the cache directory:", "")?.trim()
            assertEquals(formatSize(actualCacheDirectoriesSize), reportedCacheDirectoriesSize)
        }

        fun validateSnapshotSizeMetric() {
            // traverse only the `build` directory files, because Gradle also contains a file with the name `last-build.bin`
            val actualSnapshotSize = Files.walk(projectPath.resolve("build")).use { files ->
                val knownSnapshotFiles = setOf("last-build.bin", "build-history.bin", "abi-snapshot.bin")
                files.asSequence().filter { Files.isRegularFile(it) && it.name in knownSnapshotFiles }.map { Files.size(it) }.sum()
            }
            // the first found line of the report should contain a sum of the metric per all the tasks
            val reportedSnapshotSize = fileContents.lineSequence().find { "ABI snapshot size:" in it }
                ?.replace("ABI snapshot size:", "")?.trim()
            assertEquals(formatSize(actualSnapshotSize), reportedSnapshotSize)
        }

        validateTotalCachesSizeMetric()
        validateSnapshotSizeMetric()
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
                "Compiler IR translation line number:",
                "Compiler IR lowering line number:",
                "Compiler IR generation line number:",
                "Compiler IR translation:",
                "Compiler IR lowering:",
                "Compiler IR generation:",
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
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.SINGLE_FILE))
        ) {
            buildAndFail("compileKotlin") {
                assertOutputContains("Can't configure single file report: 'kotlin.build.report.single_file' property is mandatory")
            }
        }
    }

    @DisplayName("single build report output")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testSingleBuildMetricsFile(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.SINGLE_FILE))
        ) {
            val newMetricsPath = projectPath.resolve("metrics.bin")
            build(
                "compileKotlin", "-Pkotlin.build.report.single_file=${newMetricsPath.pathString}",
            )
            assertTrue { newMetricsPath.exists() }
        }
    }

    @DisplayName("deprecated properties")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testDeprecatedReportProperties(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val deprecatedMetricsPath = projectPath.resolve("deprecated_metrics.bin")
            build(
                "compileKotlin", "-Pkotlin.build.report.dir=${projectPath.resolve("reports").pathString}",
                "-Pkotlin.internal.single.build.metrics.file=${projectPath.resolve("deprecated_metrics.bin").pathString}"
            ) {
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedGradleProperties, "kotlin.internal.single.build.metrics.file")
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedGradleProperties, "kotlin.build.report.dir")
            }
        }
    }

    @DisplayName("smoke")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testSingleBuildMetricsFileSmoke(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.SINGLE_FILE))
        ) {
            val metricsFile = projectPath.resolve("metrics.bin").toFile()
            build(
                "compileKotlin",
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
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG, buildReport = listOf(BuildReportType.BUILD_SCAN))
        ) {
            build(
                "compileKotlin",
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
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG, buildReport = listOf(BuildReportType.BUILD_SCAN))
        ) {
            build(
                "compileKotlin",
                "-Pkotlin.build.report.build_scan.custom_values_limit=0",
            ) {
                assertOutputDoesNotContain("Can't add any more custom values into build scan")
            }
        }
    }

    @DisplayName("build scan with project isolation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildReportWithProjectIsolation(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
                projectIsolation = true,
                configurationCache = null,
                buildReport = listOf(BuildReportType.FILE, BuildReportType.JSON)
            )
        ) {
            build(
                "compileKotlin", "-Pkotlin.build.report.json.directory=${projectPath.resolve("report").pathString}"
            ) {
                val jsonReportFile = projectPath.getSingleFileInDir("report")
                assertTrue { jsonReportFile.exists() }
                val jsonReport = readJsonReport(jsonReportFile)
                assertNotNull(jsonReport)
            }
        }
    }

    @DisplayName("Error file is created")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testErrorsFileSmokeTest(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
        ) {

            val lookupsTab = projectPath.resolve("build/kotlin/compileKotlin/cacheable/caches-jvm/lookups/lookups.tab")
            val kotlinErrorPaths = setOf(
                projectPersistentCache.resolve("errors"),
                projectPath.resolve(".gradle/kotlin/errors")
            )

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
                for (kotlinErrorPath in kotlinErrorPaths) {
                    assertDirectoryDoesNotExist(kotlinErrorPath)
                }
                assertOutputDoesNotContain("errors were stored into file")
            }
            val kotlinFile = kotlinSourcesDir().resolve("helloWorld.kt")
            kotlinFile.modify { it.replace("ArrayList", "skjfghsjk") }
            buildAndFail("compileKotlin", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContains("errors were stored into file")
                kotlinErrorPaths.forEach { kotlinErrorPath ->
                    val files = kotlinErrorPath.listDirectoryEntries()
                    assertFileExists(files.single())
                    files.single().bufferedReader().use { reader ->
                        val kotlinVersion = reader.readLine()
                        assertTrue("kotlin version should be in the error file") {
                            kotlinVersion != null && kotlinVersion.trim() == "kotlin version: ${buildOptions.kotlinVersion}"
                        }
                        val errorMessage = reader.readLine()
                        assertTrue("Error message should start with 'error message: ' to parse it on IDEA side") {
                            errorMessage != null && errorMessage.trim().startsWith("error message:")
                        }
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
    fun testErrorsFileWithCompilationError(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
        ) {
            val kotlinErrorPaths = setOf(
                projectPersistentCache.resolve("errors"),
                projectPath.resolve(".gradle/kotlin/errors")
            )

            build("compileKotlin", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputDoesNotContain("errors were stored into file")
                for (kotlinErrorPath in kotlinErrorPaths) {
                    assertDirectoryDoesNotExist(kotlinErrorPath)
                }
            }
            val kotlinFile = kotlinSourcesDir().resolve("helloWorld.kt")
            kotlinFile.modify { it.replace("ArrayList", "skjfghsjk") }
            buildAndFail("compileKotlin", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputDoesNotContain("errors were stored into file")
                for (kotlinErrorPath in kotlinErrorPaths) {
                    assertDirectoryDoesNotExist(kotlinErrorPath)
                }
            }
        }
    }

    @DisplayName("Error file is not written into .gradle/kotlin/errors")
    @GradleTest
    fun testDisableWritingErrorsIntoGradleProjectDir(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
        ) {
            val kotlinErrorPath = projectPersistentCache.resolve("errors")
            val gradleErrorPath = projectPath.resolve(".gradle/kotlin/errors")
            gradleProperties.appendText(
                """
                |
                |kotlin.project.persistent.dir.gradle.disableWrite=true
                """.trimMargin()
            )

            val lookupsTab = projectPath.resolve("build/kotlin/compileKotlin/cacheable/caches-jvm/lookups/lookups.tab")
            buildGradle.appendText(
                //language=groovy
                """
                |tasks.named("compileKotlin") {
                |    doLast {
                |       new File("${lookupsTab.toUri().path}").write("Invalid contents")
                |   }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertDirectoryDoesNotExist(kotlinErrorPath.toAbsolutePath())
                assertDirectoryDoesNotExist(gradleErrorPath.toAbsolutePath())
            }

            val kotlinFile = kotlinSourcesDir().resolve("helloWorld.kt")
            kotlinFile.modify { it.replace("ArrayList", "skjfghsjk") }
            buildAndFail("compileKotlin", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContains("errors were stored into file")
                assertDirectoryExists(kotlinErrorPath.toAbsolutePath())
                val errorFiles = kotlinErrorPath.listDirectoryEntries()
                assertFileExists(errorFiles.single())

                assertDirectoryDoesNotExist(gradleErrorPath.toAbsolutePath())
            }
        }
    }

    @DisplayName("build scan metrics validation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testBuildScanMetricsValidation(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.BUILD_SCAN))
        ) {
            buildAndFail(
                "compileKotlin", "-Pkotlin.build.report.build_scan.metrics=unknown_prop"
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

    @DisplayName("json validation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testJsonBuildMetricsFileValidation(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildAndFail(
                "compileKotlin",
                buildOptions = defaultBuildOptions.copy(
                    buildReport = listOf(BuildReportType.JSON)
                )
            ) {
                assertOutputContains("Can't configure json report: 'kotlin.build.report.json.directory' property is mandatory")
            }
        }
    }

    @DisplayName("json report")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_7_6, TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    fun testJsonBuildReport(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build(
                "compileKotlin",
                "-Pkotlin.build.report.json.directory=${projectPath.resolve("report").pathString}",
                buildOptions = defaultBuildOptions.copy(
                    buildReport = listOf(BuildReportType.JSON)
                )
            ) {
                val jsonReport = projectPath.getSingleFileInDir("report")
                val buildExecutionData = readJsonReport(jsonReport)
                val buildOperationRecords =
                    buildExecutionData.buildOperationRecord.first { it.path == ":compileKotlin" } as BuildOperationRecordImpl
                assertEquals(KotlinVersion.DEFAULT, buildOperationRecords.kotlinLanguageVersion)
                jsonReport.deleteExisting()
            }

            projectPath.resolve("src/main/kotlin/helloWorld.kt").modify {
                it.replace("internal fun getNames(): List<String?> = names.toList()", "")
            }

            build(
                "compileKotlin",
                "-Pkotlin.build.report.json.directory=${projectPath.resolve("report").pathString}",
                buildOptions = defaultBuildOptions.copy(
                    buildReport = listOf(BuildReportType.JSON),
                    incremental = true,
                    logLevel = LogLevel.DEBUG,
                )
            ) {
                val jsonReport = projectPath.getSingleFileInDir("report")
                val buildExecutionData = readJsonReport(jsonReport)
                val buildOperationRecords =
                    buildExecutionData.buildOperationRecord.first { it.path == ":compileKotlin" } as BuildOperationRecordImpl
                assertEquals(KotlinVersion.DEFAULT, buildOperationRecords.kotlinLanguageVersion)
            }
        }
    }

    @DisplayName("build report should not be overridden")
    @GradleTest
    fun testMultipleRuns(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion, buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
                buildReport = listOf(BuildReportType.FILE)
            )
        ) {
            val reportFolder = projectPath.resolve("build/reports/kotlin-build").toFile()
            reportFolder.mkdirs()
            assertEquals(0, reportFolder.listFiles()?.size)
            for (i in 1..10) {
                build("assemble") {
                    assertOutputContains("Kotlin build report is written to")
                }
            }
            assertEquals(10, reportFolder.listFiles()?.size)
        }
    }

}
