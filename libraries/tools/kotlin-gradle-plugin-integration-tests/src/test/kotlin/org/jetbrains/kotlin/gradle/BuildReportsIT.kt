/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.DynamicBuildTimeKey
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.statistics.formatSize
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.internal.build.metrics.GradleBuildMetricsData
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.TestVersions.ThirdPartyDependencies.GRADLE_ENTERPRISE_PLUGIN_VERSION
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.BuildOperationRecordImpl
import org.jetbrains.kotlin.gradle.util.readJsonReport
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.ObjectInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.asSequence
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("Build reports")
class BuildReportsIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            buildReport = listOf(BuildReportType.FILE)
        )

    private val GradleProject.reportFile: Path
        get() = projectPath.getSingleFileInDir("build/reports/kotlin-build")

    @DisplayName("Build report is created")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testBuildReportOutputProperty(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildAndFail("assemble", "-Pkotlin.build.report.output=file,invalid") {
                assertOutputContains("Unknown output type:")
            }
        }
    }

    @DisplayName("Build metrics produces valid report")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testBuildMetricsSmokeTest(gradleVersion: GradleVersion) {
        testBuildReportInFile("simpleProject", "assemble", gradleVersion)
    }

    @DisplayName("Build metrics produces valid report for mpp-jvm")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testBuildMetricsForMppJvm(gradleVersion: GradleVersion) {
        testBuildReportInFile("mppJvmWithJava", "assemble", gradleVersion)
    }

    @DisplayName("Build metrics produces valid report for mpp-js")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testBuildMetricsForMppJs(gradleVersion: GradleVersion) {
        testBuildReportInFile(
            "kotlin-js-package-module-name",
            "assemble",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            disableIsolatedProjects = true,
        )
    }

    @DisplayName("Build metrics produces valid report for JS project")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @TestMetadata("kotlin-js-plugin-project")
    @JvmGradlePluginTests
    fun testBuildMetricsForJsProject(gradleVersion: GradleVersion) {
        testBuildReportInFile(
            "kotlin-js-plugin-project",
            "compileKotlinJs",
            gradleVersion,
            languageVersion = KotlinVersion.DEFAULT.version,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            disableIsolatedProjects = true,
        )
    }

    @DisplayName("Build metrics produces valid report for lowerings in JS project")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @TestMetadata("kotlin-js-plugin-project")
    @JvmGradlePluginTests
    fun testLoweringsBuildMetricsForJsProject(gradleVersion: GradleVersion) {
        testBuildReportInFile(
            "kotlin-js-plugin-project",
            "compileKotlinJs",
            gradleVersion,
            languageVersion = KotlinVersion.DEFAULT.version,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            disableIsolatedProjects = true,
            freeCompilerArgs = listOf("-XXLanguage:+IrInlinerBeforeKlibSerialization"),
            expectedReportLines = listOf(
                "Compiler IR pre-lowering",
                "JsCodeOutliningLoweringOnFirstStage",
            ),
        )
    }

    private fun testBuildReportInFile(
        project: String,
        task: String,
        gradleVersion: GradleVersion,
        languageVersion: String = KotlinVersion.KOTLIN_2_0.version,
        disableIsolatedProjects: Boolean = false,
        freeCompilerArgs: List<String> = listOf(),
        expectedReportLines: List<String> = listOf(),
    ) {
        val buildOptions = if (disableIsolatedProjects) defaultBuildOptions.copy(
            isolatedProjects = IsolatedProjectsMode.DISABLED
        ) else defaultBuildOptions

        project(project, gradleVersion, buildOptions = buildOptions) {
            if (!isWithJavaSupported && project == "mppJvmWithJava") buildGradle.replaceText("withJava()", "")
            addCompilerArgs(freeCompilerArgs)
            build(task) {
                assertBuildReportPathIsPrinted()
            }
            //Should contain build metrics for all compile kotlin tasks
            validateBuildReportFile(nonIncrementalBuildFileExpectedContents(KotlinVersion.DEFAULT.version), expectedReportLines)
        }

        project(project, gradleVersion, buildOptions = buildOptions.copy(languageVersion = languageVersion)) {
            if (!isWithJavaSupported && project == "mppJvmWithJava") buildGradle.replaceText("withJava()", "")
            addCompilerArgs(freeCompilerArgs)
            build(task, buildOptions = buildOptions.copy(languageVersion = languageVersion)) {
                assertBuildReportPathIsPrinted()
            }
            //Should contain build metrics for all compile kotlin tasks
            validateBuildReportFile(nonIncrementalBuildFileExpectedContents(languageVersion), expectedReportLines)
        }
    }

    private fun TestProject.addCompilerArgs(args: List<String>) {
        if (args.isNotEmpty()) {
            buildGradleKts.appendText(
                """
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
                    compilerOptions {
                        freeCompilerArgs.add(${args.joinToString { "\"$it\"" }})
                    }
                }
                """.trimIndent()
            )
        }
    }

    @DisplayName("Build metrics produces valid report for lowerings in Native project")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @TestMetadata("native-incremental-simple")
    @NativeGradlePluginTests
    fun testLoweringsBuildMetricsForNativeProject(gradleVersion: GradleVersion) {
        testNativeBuildReportInFile(
            "native-incremental-simple",
            "build",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            disableIsolatedProjects = true,
            freeCompilerArgs = listOf("-XXLanguage:+IrInlinerBeforeKlibSerialization"),
            additionalReportLines = listOf(
                "InlineFunctionSerializationPreProcessing",
            ),
        )
    }

    private fun testNativeBuildReportInFile(
        project: String,
        task: String,
        gradleVersion: GradleVersion,
        disableIsolatedProjects: Boolean = false,
        freeCompilerArgs: List<String> = listOf(),
        additionalReportLines: List<String> = listOf(),
    ) {
        val buildOptions = if (disableIsolatedProjects) defaultBuildOptions.copy(
            isolatedProjects = IsolatedProjectsMode.DISABLED
        ) else defaultBuildOptions

        nativeProject(project, gradleVersion, buildOptions = buildOptions) {
            addNativeCompilerArgs(freeCompilerArgs)
            build(task) {
                assertBuildReportPathIsPrinted()
            }
            //Should contain build metrics for all compile kotlin tasks
            validateBuildReportFile(nativeBuildFileExpectedContents, additionalReportLines, doValidateSizeMetrics = false)
        }
    }

    private fun TestProject.addNativeCompilerArgs(args: List<String>) {
        if (args.isNotEmpty()) {
            buildGradleKts.appendText(
                """
                    
                kotlin {
                    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
                        compilerOptions {
                            freeCompilerArgs.add(${args.joinToString { "\"$it\"" }})
                        }
                    }
                }
                """.trimIndent()
            )
        }
    }

    val nativeBuildFileExpectedContents = listOf(
        "Time metrics:",
        "Run compilation",
        "Run native in process:",
        "Compiler IR pre-lowering:",
        "InlineFunctionSerializationPreProcessing",
        "Compiler IR Serialization:",
        "Compiler IR lowering:",
        "ValidateIrBeforeLowering:",
        "ValidateIrAfterLowering:",
        "Compiler backend:",
        "llvm-default.AlwaysInlinerPass:",
        "Size metrics:",
    )

    private fun nonIncrementalBuildFileExpectedContents(kotlinLanguageVersion: String) = listOf(
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

    private fun TestProject.validateBuildReportFile(expectedReportLines: List<String>, additionalReportLines: List<String>, doValidateSizeMetrics: Boolean = true) {
        val fileContents = assertFileContains(
            reportFile,
            *expectedReportLines.toTypedArray(),
            *additionalReportLines.toTypedArray()
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

        if (doValidateSizeMetrics) {
            validateTotalCachesSizeMetric()
            validateSnapshotSizeMetric()
        }
    }

    @DisplayName("Compiler build metrics report is produced")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
                "Number of lines analyzed:",
                "Compiler translation to IR:",
                "Compiler IR lowering:",
                "Compiler backend:",
            )
        }
    }

    @DisplayName("with no kotlin task executed")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testDeprecatedReportProperties(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val deprecatedMetricsPath = projectPath.resolve("deprecated_metrics.bin")
            build(
                "compileKotlin", "-Pkotlin.build.report.dir=${projectPath.resolve("reports").pathString}",
                "-Pkotlin.internal.single.build.metrics.file=${deprecatedMetricsPath.pathString}"
            ) {
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties, "kotlin.internal.single.build.metrics.file")
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties, "kotlin.build.report.dir")
            }
        }
    }

    @DisplayName("smoke")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
                assertOutputContains(CAN_NOT_ADD_CUSTOM_VALUES_TO_BUILD_SCAN_MESSAGE)
            }
        }
    }

    @DisplayName("build scan listener lazy initialisation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
                assertOutputDoesNotContain(CAN_NOT_ADD_CUSTOM_VALUES_TO_BUILD_SCAN_MESSAGE)
            }
        }
    }

    @DisplayName("build scan with project isolation")
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testBuildReportWithProjectIsolation(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
                isolatedProjects = IsolatedProjectsMode.ENABLED,
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
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
        additionalVersions = [TestVersions.Gradle.G_8_0],
    )
    @GradleTest
    @JvmGradlePluginTests
    fun testErrorsFileWithCompilationError(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
        ) {
            assertNoErrorFilesCreated {
                build("compileKotlin", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                    assertNoErrorFileCreatedInOutput()
                }
                val kotlinFile = kotlinSourcesDir().resolve("helloWorld.kt")
                kotlinFile.modify { it.replace("ArrayList", "skjfghsjk") }
                buildAndFail("compileKotlin", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                    assertNoErrorFileCreatedInOutput()
                }
            }
        }
    }

    @JvmGradlePluginTests
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
    @JvmGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
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
    @JvmGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
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
    @JvmGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
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
    @JvmGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0],
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
    @JvmGradlePluginTests
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

    @DisplayName("build scan with project isolation")
    @JvmGradlePluginTests
    @GradleTestVersions(
        //There is an exception for gradle 7.6 with project isolation:
        //Plugin 'com.gradle.enterprise': Cannot access project ':app' from project ':'
        minVersion = TestVersions.Gradle.G_8_0,
        // https://youtrack.jetbrains.com/issue/KT-68847
        maxVersion = TestVersions.Gradle.G_8_14,
    )
    @GradleTest
    fun testBuildScanReportWithProjectIsolation(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = IsolatedProjectsMode.ENABLED,
                buildReport = listOf(BuildReportType.BUILD_SCAN)
            )
        ) {
            build(
                "compileKotlin", "--scan"
            ) {
                assertOutputContains("Build report creation in the build scan format is not yet supported when the isolated projects feature is enabled.")
            }
        }
    }

    @DisplayName("for build scan with develocity plugin")
    @JvmGradlePluginTests
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_4
    )
    @GradleTest
    fun testBuildScanReportWithDevelocityPlugin(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
                configurationCache = BuildOptions.ConfigurationCacheValue.UNSPECIFIED,
                buildReport = listOf(BuildReportType.BUILD_SCAN),
                // KT-68847 Support build reports for build scan with project isolation
                isolatedProjects = IsolatedProjectsMode.DISABLED,
            )
        ) {
            settingsGradle.modify {
                """
                ${
                    it.replace(
                        "id(\"org.jetbrains.kotlin.test.gradle-warnings-detector\")",
                        """
                               id("org.jetbrains.kotlin.test.gradle-warnings-detector")
                               id "com.gradle.develocity" version "${TestVersions.ThirdPartyDependencies.GRADLE_DEVELOCITY_PLUGIN_VERSION}"
                        """.trimIndent()
                    )
                }
                        
                develocity {
                    buildScan {
                        termsOfUseAgree = "yes"
                        termsOfUseUrl = "https://gradle.com/terms-of-service"

                        tag "test"
                    }
                }
                """.trimIndent()
            }
            build(
                "compileKotlin", "--scan"
            ) {
                assertOutputDoesNotContain("The build scan was not published due to a configuration problem.")
                assertOutputDoesNotContain("The following functionality has been deprecated and will be removed in the next major release of the Develocity Gradle plugin.")
                assertOutputContains("Build metrics are stored into build scan for")
                assertOutputContains("[com.gradle.develocity.agent.gradle.DevelocityPlugin] Publishing build scan...")
            }
        }
    }

    @DisplayName("Verify metrics for for 2nd phase native in-process compilation")
    @NativeGradlePluginTests
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testMetricFor2ndPhaseNativeProjectInProcess(gradleVersion: GradleVersion) {
        nativeProject(
            "native-incremental-simple", gradleVersion, buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                ),
                buildReport = listOf(BuildReportType.JSON)
            )
        ) {
            build("linkDebugExecutableHost", "-Pkotlin.build.report.json.directory=${projectPath.resolve("report").pathString}") {
                val jsonReportFile = projectPath.getSingleFileInDir("report")
                assertTrue { jsonReportFile.exists() }
                val jsonReport = readJsonReport(jsonReportFile)
                val bulidTimesKeys = jsonReport.aggregatedMetrics.buildTimes.buildTimesMapMs().keys
                assertContains(bulidTimesKeys, GradleBuildTime.NATIVE_IN_PROCESS)

                val dynamicBuildTimesKeys = jsonReport.aggregatedMetrics.buildTimes.dynamicBuildTimesMapMs().keys
                    .filter { it.parent == GradleBuildTime.IR_LOWERING }
                    .map { it.name }
                val expectedDynamicBuildTimesNames = listOf(
                    "ValidateIrBeforeLowering",
                    "TestProcessor",
                    "UpgradeCallableReferences",
                    "Autobox",
                    "ConstructorsLowering",
                    "ValidateIrAfterLowering",
                )
                expectedDynamicBuildTimesNames.forEach {
                    assertContains(dynamicBuildTimesKeys, it)
                }
            }
        }
    }

    @DisplayName("Verify that the metric for native in-process compilation")
    @NativeGradlePluginTests
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testMetricForNativeProjectInProcess(gradleVersion: GradleVersion) {
        nativeProject(
            "native-incremental-simple", gradleVersion, buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                ),
                buildReport = listOf(BuildReportType.JSON)
            )
        ) {
            build("linkDebugExecutableHost", "-Pkotlin.build.report.json.directory=${projectPath.resolve("report").pathString}") {
                val jsonReportFile = projectPath.getSingleFileInDir("report")
                assertTrue { jsonReportFile.exists() }
                val jsonReport = readJsonReport(jsonReportFile)
                assertContains(jsonReport.aggregatedMetrics.buildTimes.buildTimesMapMs().keys, GradleBuildTime.NATIVE_IN_PROCESS)

                val compilerMetrics = GradleBuildTime.COMPILER_PERFORMANCE.allChildrenMetrics()
                val reportedCompilerMetrics =
                    jsonReport.aggregatedMetrics.buildTimes.buildTimesMapMs().keys.filter { it in compilerMetrics }

                // Recursively (only two levels) gather leaves of subtree under COMPILER_PERFORMANCE, excluding nodes like CODE_GENERATION
                val expected = GradleBuildTime.COMPILER_PERFORMANCE.children()?.flatMap { it.children() ?: listOf(it) }
                assertEquals(
                    expected,
                    reportedCompilerMetrics.sorted()
                )

                assertTrue {
                    jsonReport.aggregatedMetrics.buildTimes.dynamicBuildTimesMapMs().keys.contains(
                        DynamicBuildTimeKey("AvoidLocalFOsInInlineFunctionsLowering", GradleBuildTime.IR_PRE_LOWERING)
                    )
                }
                assertTrue {
                    // LLVM passes must have been reported
                    jsonReport.aggregatedMetrics.buildTimes.dynamicBuildTimesMapMs().keys.contains(
                        DynamicBuildTimeKey("llvm-default.AlwaysInlinerPass", GradleBuildTime.BACKEND)
                    )
                }
            }
        }
    }

    @DisplayName("Verify that the metric for native in-process compilation with IR Inliner on 1st phase")
    @NativeGradlePluginTests
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testMetricForNativeProjectWithInilnedFunInKlibInProcess(gradleVersion: GradleVersion) {
        nativeProject(
            "native-incremental-simple", gradleVersion, buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                ),
                buildReport = listOf(BuildReportType.JSON)
            )
        ) {
            buildScriptInjection {
                project.applyMultiplatform {
                    compilerOptions.freeCompilerArgs.add("-XXLanguage:+IrInlinerBeforeKlibSerialization")
                }
            }
            build("linkDebugExecutableHost", "-Pkotlin.build.report.json.directory=${projectPath.resolve("report").pathString}") {
                val jsonReportFile = projectPath.getSingleFileInDir("report")
                assertTrue { jsonReportFile.exists() }
                val jsonReport = readJsonReport(jsonReportFile)
                val bulidTimesKeys = jsonReport.aggregatedMetrics.buildTimes.buildTimesMapMs().keys
                assertContains(bulidTimesKeys, GradleBuildTime.NATIVE_IN_PROCESS)

                val compilerMetrics = GradleBuildTime.COMPILER_PERFORMANCE.allChildrenMetrics()
                val reportedCompilerMetrics = bulidTimesKeys.filter { it in compilerMetrics }

                // Recursively (only two levels) gather leaves of subtree under COMPILER_PERFORMANCE, excluding nodes like CODE_GENERATION
                val expected = GradleBuildTime.COMPILER_PERFORMANCE.children()?.flatMap { it.children() ?: listOf(it) }
                assertEquals(
                    expected,
                    reportedCompilerMetrics.sorted()
                )

                assertEquals(
                    listOf(
                        "UpgradeCallableReferences",
                        "AssertionWrapperLowering",
                        "AvoidLocalFOsInInlineFunctionsLowering",
                        "LateinitLowering", // first lowering in K/N 1st phase lowerings, specific for `+IrInlinerBeforeKlibSerialization` feature
                    ),
                    jsonReport.aggregatedMetrics.buildTimes.dynamicBuildTimesMapMs().keys
                        .filter { it.parent == GradleBuildTime.IR_PRE_LOWERING }
                        .map { it.name }
                        .take(4)
                )
            }
        }
    }

    companion object {
        private const val CAN_NOT_ADD_CUSTOM_VALUES_TO_BUILD_SCAN_MESSAGE = "Can't add any more custom values into build scan"
    }
}
