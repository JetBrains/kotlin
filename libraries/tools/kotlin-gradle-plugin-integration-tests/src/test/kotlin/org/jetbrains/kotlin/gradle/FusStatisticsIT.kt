/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.file.Directory
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.version
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.includeBuild
import org.jetbrains.kotlin.gradle.util.filterBackwardCompatibilityKotlinFusFiles
import org.jetbrains.kotlin.gradle.util.filterKotlinFusFiles
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("FUS statistic")
class FusStatisticsIT : KGPBaseTest() {
    private val expectedMetrics = arrayOf(
        "OS_TYPE",
        "OS_VERSION",
        "BUILD_FAILED=false",
        "EXECUTED_FROM_IDEA=false",
        "BUILD_FINISH_TIME",
        "GRADLE_VERSION",
        "KOTLIN_STDLIB_VERSION",
        "KOTLIN_BTA_USED",
        "KOTLIN_COMPILER_VERSION",
        "KOTLIN_GRADLE_PLUGIN_VERSION",
        "KOTLIN_COMPILER_EXECUTION_POLICY",
    )

    @JvmGradlePluginTests
    @DisplayName("for dokka")
    @GradleTest
    fun testDokka(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            // TODO: KT-70336 dokka doesn't support Configuration Cache
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                isolatedProjects = IsolatedProjectsMode.DISABLED,
            )
        ) {
            applyDokka(TestVersions.ThirdPartyDependencies.DOKKA)
            validateFusFiles(
                "compileKotlin", "dokkaHtml",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "ENABLED_DOKKA",
                    "ENABLED_DOKKA_HTML"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("for dokka v2 html doc")
    @GradleTest
    fun testDokkaV2HtmlDoc(gradleVersion: GradleVersion) {
        val expectedDokkaFusMetrics = arrayOf(
            "ENABLED_DOKKA",
            "ENABLE_DOKKA_GENERATE_TASK",
            "ENABLE_DOKKA_GENERATE_PUBLICATION_HTML_TASK",
            "ENABLE_LINK_DOKKA_GENERATE_TASK"
        )
        testDokkaPlugin(gradleVersion, "org.jetbrains.dokka", expectedDokkaFusMetrics)
    }

    @JvmGradlePluginTests
    @DisplayName("for dokka v2 javadoc")
    @GradleTest
    fun testDokkaV2Javadoc(gradleVersion: GradleVersion) {
        val expectedDokkaFusMetrics = arrayOf(
            "ENABLED_DOKKA_JAVADOC",
            "ENABLE_DOKKA_GENERATE_TASK",
            "ENABLE_DOKKA_GENERATE_PUBLICATION_JAVADOC_TASK",
        )
        testDokkaPlugin(gradleVersion, "org.jetbrains.dokka-javadoc", expectedDokkaFusMetrics)
    }

    private fun testDokkaPlugin(gradleVersion: GradleVersion, pluginName: String, expectedDokkaFusMetrics: Array<String>) {
        project("simpleProject", gradleVersion) {
            settingsGradle.replaceText(
                "repositories {",
                """
                    repositories {
                         maven { url = "https://redirector.kotlinlang.org/maven/dokka-dev" }
                """.trimIndent()
            )

            //for templating-plugin and dokka-base plugins
            buildGradle.replaceText(
                "repositories {",
                """
                    repositories {
                         maven { url = "https://redirector.kotlinlang.org/maven/dokka-dev" }
                """.trimIndent()
            )

            //apply Dokka plugins
            buildGradle.replaceText(
                "plugins {",
                """
                plugins {
                    id("$pluginName") version "${TestVersions.ThirdPartyDependencies.DOKKA_V2}"
                """.trimIndent()
            )

            val fusReportRootDirectory = defaultFusReportRootDirectory()

            validateFusFiles(
                "compileKotlin",
                "dokkaGenerate",
                "-Porg.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled",
                buildAction = BuildActions.build,
                fusReportRootDirectory = fusReportRootDirectory,
                buildAssertions = { assertConfigurationCacheStored() }
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, *expectedDokkaFusMetrics)
            }

            fusReportRootDirectory.deleteRecursively()
            build("clean")

            validateFusFiles(
                "compileKotlin",
                "dokkaGenerate",
                "-Porg.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled",
                buildAction = BuildActions.build,
                fusReportRootDirectory = fusReportRootDirectory,
                buildAssertions = { assertConfigurationCacheReused() }
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, *expectedDokkaFusMetrics)
            }
        }
    }

    @NativeGradlePluginTests
    @DisplayName("Verify that the metric for applying the Cocoapods plugin is being collected")
    @GradleTest
    fun testMetricCollectingOfApplyingCocoapodsPlugin(gradleVersion: GradleVersion) {
        project("native-cocoapods-template", gradleVersion) {
            validateFusFiles(
                "assemble",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, "COCOAPODS_PLUGIN_ENABLED=true", "ENABLED_HMPP=true", "MPP_PLATFORMS")
            }
        }
    }

    @NativeGradlePluginTests
    @DisplayName("Verify that the metric for native incremental compilation")
    @GradleTest
    fun testMetricCollectingForNative(gradleVersion: GradleVersion) {
        nativeProject(
            "native-incremental-simple", gradleVersion, buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                )
            )
        ) {
            validateFusDirectory(
                "linkDebugExecutableHost",
                buildAction = BuildActions.build,
            ) { fusDirectory ->
                fusDirectory.assertFusReportContains("KOTLIN_INCREMENTAL_NATIVE_ENABLED=true")
                fusDirectory.assertFusReportContainsMetricWithValues("MPP_PLATFORMS", listOf("common", HostManager.host.name))
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("Ensure that the metric are not collected if plugins were not applied to simple project")
    @GradleTest
    fun testAppliedPluginsMetricsAreNotCollectedInSimpleProject(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            validateFusDirectory(
                "assemble",
                buildAction = BuildActions.build,
            ) { fusDirectory ->
                fusDirectory.assertFusReportContains(*expectedMetrics)
                // asserts that we do not put DOKKA metrics everywhere just in case
                fusDirectory.assertFusReportDoesNotContain("ENABLED_DOKKA_HTML", "KOTLIN_JS_PLUGIN_ENABLED")
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("for project with buildSrc")
    @GradleTest
    fun testProjectWithBuildSrcForGradleVersion7(gradleVersion: GradleVersion) {
        //KT-64022 there are different build instances in buildSrc and rest project:
        project(
            "instantExecutionWithBuildSrc",
            gradleVersion,
        ) {
            validateFusFiles(
                "compileKotlin",
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    *expectedMetrics,
                    "BUILD_SRC_EXISTS=true"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("for failed build")
    @GradleTest
    fun testFusStatisticsForFailedBuild(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
        ) {
            projectPath.resolve("src/main/kotlin/helloWorld.kt").modify {
                it.replace("java.util.ArrayList", "")
            }
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.buildAndFail,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "BUILD_FAILED=true",
                    "OS_TYPE",
                    "EXECUTED_FROM_IDEA=false",
                    "BUILD_FINISH_TIME",
                    "GRADLE_VERSION",
                    "KOTLIN_STDLIB_VERSION",
                    "KOTLIN_COMPILER_VERSION",
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("fus metric for multiproject")
    @GradleTest
    fun testFusStatisticsForMultiproject(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
        ) {
            //Collect metrics from BuildMetricsService also
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
                buildOptions = defaultBuildOptions
                    .copy(buildReport = listOf(BuildReportType.FILE))
                    // With isolated projects enabled, it creates 2 profile files,
                    // this behavior is tested in [org.jetbrains.kotlin.gradle.FusPluginIT.withConfigurationCacheAndProjectIsolation]
                    .disableIsolatedProjects(),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=2",
                    "NUMBER_OF_SUBPROJECTS=2",
                    "COMPILATIONS_COUNT=2"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("test configuration time ksp metrics")
    @GradleTest
    fun testFusStatisticsForKsp(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
                id("com.google.devtools.ksp") version (TestVersions.ThirdPartyDependencies.KSP)
            }
            validateFusFiles(
                "help",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KSP_GRADLE_PLUGIN_VERSION=1.9.22"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("fus metric for jvm feature flags")
    @GradleTest
    fun testFusStatisticsForJvmMultiprojectWithFeatureFlags(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
        ) {
            //Collect metrics from BuildMetricsService also
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
                buildOptions = defaultBuildOptions
                    .copy(
                        buildReport = listOf(BuildReportType.FILE),
                        useFirJvmRunner = true,
                    ).disableIsolatedProjects(),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=2",
                    "NUMBER_OF_SUBPROJECTS=2",
                    "COMPILATIONS_COUNT=2",
                    "KOTLIN_INCREMENTAL_FIR_RUNNER_ENABLED=true"
                )
            }
        }
    }

    @MppGradlePluginTests
    @DisplayName("fus metrics for KMP JVM incremental compilation flags")
    @GradleTest
    fun testKmpJvmIncrementalCompilationFlagsMetrics(gradleVersion: GradleVersion) {
        project("jvm-with-common", gradleVersion) {
            validateFusFiles(
                "compileKotlinJvm",
                buildAction = BuildActions.build,
                buildOptions = defaultBuildOptions.copy(
                    jvmClasspathMetadata = true,
                    enableJvmIncrementalCompilationOfCommonSources = true,
                ),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KMP_JVM_CLASSPATH_METADATA_ENABLED=true",
                    "KMP_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES_ENABLED=true",
                )
            }

            validateFusFiles(
                "clean", "compileKotlinJvm",
                buildAction = BuildActions.build,
                buildOptions = defaultBuildOptions.copy(
                    jvmClasspathMetadata = false,
                    enableJvmIncrementalCompilationOfCommonSources = false,
                ),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KMP_JVM_CLASSPATH_METADATA_ENABLED=false",
                    "KMP_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES_ENABLED=false",
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("general fields with configuration cache")
    @GradleTest
    fun testFusStatisticsWithConfigurationCache(gradleVersion: GradleVersion) {
        testFusStatisticsWithConfigurationCache(gradleVersion, IsolatedProjectsMode.DISABLED)
    }

    @JvmGradlePluginTests
    @DisplayName("general fields with configuration cache and project isolation")
    @GradleTest
    fun testFusStatisticsWithConfigurationCacheAndProjectIsolation(gradleVersion: GradleVersion) {
        testFusStatisticsWithConfigurationCache(gradleVersion, IsolatedProjectsMode.ENABLED)
    }

    fun testFusStatisticsWithConfigurationCache(gradleVersion: GradleVersion, isProjectIsolationEnabled: IsolatedProjectsMode) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = isProjectIsolationEnabled,
                buildReport = listOf(BuildReportType.FILE)
            ),
        ) {
            val fusReportRootDirectory = defaultFusReportRootDirectory()

            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
                fusReportRootDirectory = fusReportRootDirectory,
                buildAssertions = { assertConfigurationCacheStored() }
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    *expectedMetrics,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "NUMBER_OF_SUBPROJECTS=1",
                    "COMPILATIONS_COUNT=1",
                    "GRADLE_CONFIGURATION_CACHE_ENABLED=true",
                    "GRADLE_PROJECT_ISOLATION_ENABLED=${isProjectIsolationEnabled.toBooleanFlag(gradleVersion)}",
                )
            }

            fusReportRootDirectory.resolve("kotlin-profile").listDirectoryEntries()
                .forEach { assertTrue(it.deleteIfExists(), "Can't delete file ${it.absolutePathString()}") }

            build("clean", buildOptions = buildOptions)

            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
                fusReportRootDirectory = fusReportRootDirectory,
                buildAssertions = { assertConfigurationCacheReused() }
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    *expectedMetrics,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "NUMBER_OF_SUBPROJECTS=1",
                    "COMPILATIONS_COUNT=1"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("configuration type metrics")
    @GradleTest
    fun testConfigurationTypeFusMetrics(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "CONFIGURATION_COMPILE_ONLY_COUNT=1",
                    "CONFIGURATION_API_COUNT=1",
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "CONFIGURATION_RUNTIME_ONLY_COUNT=1",
                )
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    fun testFusMetricsCanBeDisabled(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build(
                "assemble",
                "-Pkotlin.internal.collectFUSMetrics=false",
            ) {
                assertFileNotExists(projectPath.resolve("kotlin-profile"))
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    @GradleTestVersions(
        // Kover triggers deprecation in Gradle 9.6.0 https://github.com/Kotlin/kotlinx-kover/issues/813
        maxVersion = TestVersions.Gradle.G_9_5,
    )
    fun testKotlinxPlugins(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
        ) {
            buildGradle.replaceText(
                "plugins {",
                """
                    plugins {
                        id("org.jetbrains.kotlinx.atomicfu") version "${TestVersions.ThirdPartyDependencies.KOTLINX_ATOMICFU}"
                        id("org.jetbrains.kotlinx.kover") version "${TestVersions.ThirdPartyDependencies.KOTLINX_KOVER}"
                        id("org.jetbrains.kotlinx.binary-compatibility-validator") version "${TestVersions.ThirdPartyDependencies.KOTLINX_BINARY_COMPATIBILITY_VALIDATOR}"
                        id("org.jetbrains.kotlin.plugin.serialization") version "${'$'}kotlin_version"
                    """.trimIndent()
            )
            validateFusFiles(
                "assemble",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KOTLINX_KOVER_GRADLE_PLUGIN_ENABLED=true",
                    "KOTLINX_SERIALIZATION_GRADLE_PLUGIN_ENABLED=true",
                    "KOTLINX_ATOMICFU_GRADLE_PLUGIN_ENABLED=true",
                    "KOTLINX_BINARY_COMPATIBILITY_GRADLE_PLUGIN_ENABLED=true",
                )
            }
        }
    }

    @MppGradlePluginTests
    @GradleTest
    fun testWasmIncrementalStatisticCollection(gradleVersion: GradleVersion) {
        project(
            "new-mpp-wasm-test",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            gradleProperties.writeText("kotlin.incremental.wasm=true")

            buildGradleKts.modify {
                it
                    .replace("wasmJs {", "wasmJs {\nbinaries.executable()")
                    .replace("<JsEngine>", "nodejs")
            }

            validateFusFiles(
                "compileDevelopmentExecutableKotlinWasmJs",
                buildAction = BuildActions.build,
                buildAssertions = { assertTasksExecuted(":compileDevelopmentExecutableKotlinWasmJs") }
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, "WASM_IR_INCREMENTAL=true")
            }
        }
    }

    @JsGradlePluginTests
    @DisplayName("browser test DSL configured with options changed from defaults")
    @GradleTest
    @OptIn(ExperimentalJsTestDsl::class)
    fun testJsBrowserTestDslWithChangedOptions(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(
                    configurationCache = if (gradleVersion == GradleVersion.version(TestVersions.Gradle.G_8_14)) {
                        // FIXME: KT-88448
                        BuildOptions.ConfigurationCacheValue.DISABLED
                    } else {
                        BuildOptions.ConfigurationCacheValue.ENABLED
                    }
                )
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js().browser {
                        test.apply {
                            headless.set(false)
                            @OptIn(DelicateKotlinGradlePluginApi::class)
                            testsLocation.set(
                                object : KotlinJsTestsLocation {
                                    override val bundleLocation: Provider<Directory> = project.layout.buildDirectory.dir("some_dir")
                                    override val testHtmlFileName: Provider<String> = project.provider { "test.html" }

                                    @OptIn(DelicateKotlinGradlePluginApi::class)
                                    @get:Internal
                                    override val url: Provider<URI> =
                                        bundleLocation.map { it.asFile.resolve(testHtmlFileName.get()).toURI() }
                                }
                            )
                            chromium {
                                it.launchArgs.set(listOf("--no-sandbox"))
                                it.launchEnvironmentVariables.put("KOTLIN_JS_TEST_VARIABLE", "42")
                            }
                            firefox()
                        }
                    }
                }
            }

            validateFusDirectory(
                "assemble",
                buildAction = BuildActions.build,
            ) { fusDirectory ->
                fusDirectory.assertFusReportContainsMetricWithValues(
                    StringListMetrics.JS_TEST_BROWSER_TYPE.name,
                    listOf("chromium", "firefox")
                )
                // the values are reported in the alphabetical order
                fusDirectory.assertFusReportContainsMetricWithValues(
                    StringListMetrics.JS_TEST_BROWSER_CHANGED_OPTION.name,
                    listOf("headless", "launchArgs", "launchEnvironmentVariables", "testsLocation")
                )
            }
        }
    }

    @DisplayName("native compiler arguments")
    @GradleTest
    @NativeGradlePluginTests
    fun testNativeCompilerArguments(gradleVersion: GradleVersion) {
        nativeProject("native-incremental-simple", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |kotlin {
                |    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
                |        compilerOptions {
                |            freeCompilerArgs.add("-Xbinary=gc=noop")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            validateFusFiles(
                "linkDebugExecutableHost",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, "ENABLED_NOOP_GC=true")
            }
        }
    }

    // Swift export enabled only on macOS.
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("native swift export - happy path")
    @GradleTest
    @NativeGradlePluginTests
    fun testSwiftExportIsReported(gradleVersion: GradleVersion, @TempDir testBuildDir: Path) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                }
            }

            // Check that we generate ENABLED_SWIFT_EXPORT=true when building Swift export.
            validateFusFiles(
                ":embedSwiftExportForXcode",
                buildAction = { buildArguments, options, assertions ->
                    build(
                        buildArguments = buildArguments,
                        buildOptions = options,
                        environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
                        assertions = assertions
                    )
                },
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, "ENABLED_SWIFT_EXPORT=true")
            }
        }
    }

    // Swift export enabled only on macOS.
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("native swift export - unhappy path")
    @GradleTest
    @NativeGradlePluginTests
    fun testSwiftExportIsNotReportedWithoutNeed(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64 {
                        binaries {
                            framework()
                        }
                    }
                }
            }

            // Check that we do not generate ENABLED_SWIFT_EXPORT=true when building other Native targets.
            validateFusDirectory(":linkDebugFrameworkIosArm64") { fusDirectory ->
                fusDirectory.assertFusReportDoesNotContain(
                    "ENABLED_SWIFT_EXPORT=true",
                )
            }
        }
    }

    @DisplayName("add configuration metrics after build was finish")
    @GradleTest
    @MppGradlePluginTests
    fun addConfigurationMetricsAfterFlowActionWasCalled(gradleVersion: GradleVersion) {
        project(
            "multiplatformFlowAction",
            gradleVersion,
            buildOptions = defaultBuildOptions.suppressDeprecationWarningsOn("Test uses deprecated Gradle features") {
                gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_0)
            }
        ) {
            buildScriptInjection {
                project.tasks.register("doNothing") {}
            }
            build("doNothing")
        }
    }

    @DisplayName("add configuration metrics after build was finish")
    @GradleTest
    @JvmGradlePluginTests
    fun concurrencyModificationExceptionTest(gradleVersion: GradleVersion) {
        val rounds = 100
        project(
            "multiClassloaderProject", gradleVersion,
        ) {
            val fusReportRootDirectory = defaultFusReportRootDirectory()

            repeat(rounds) {
                validateFusDirectory(
                    "compileKotlin", "-Dorg.gradle.parallel=true",
                    buildAction = BuildActions.build,
                    buildOptions = defaultBuildOptions.copy(
                        buildReport = listOf(BuildReportType.FILE),
                        isolatedProjects = IsolatedProjectsMode.ENABLED,
                    ),
                    fusReportRootDirectory = fusReportRootDirectory,
                    buildAssertions = { assertOutputDoesNotContain("BuildFusService was not registered") }
                )

                build("clean", buildOptions = buildOptions)
            }

            val fusDirectory = fusReportRootDirectory.resolve("kotlin-profile")
            //every submodule will create a separate file. There are two modules in the project
            assertEquals(rounds * 2, fusDirectory.filterKotlinFusFiles().size)

            fusDirectory.assertFusReportContains(
                "CONFIGURATION_IMPLEMENTATION_COUNT",
                "NUMBER_OF_SUBPROJECTS",
            )
        }
    }

    @DisplayName("disable FUS on TC")
    @GradleTest
    @JvmGradlePluginTests
    @OptIn(EnvironmentalVariablesOverride::class)
    //This test relies on the 'TEAMCITY_VERSION' environment variable being set on TeamCity agents.
    //To run locally, set the environment variable TEAMCITY_VERSION to any value:
    //environmentVariables = EnvironmentalVariables("TEAMCITY_VERSION" to "1.0.0")
    fun disableFusOnTeamCity(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
        ) {
            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG, pathToFusReportDirectory = { null })) {
                assertOutputContains("Fus metrics won't be collected: CI build is detected via environment variable TEAMCITY_VERSION")
            }
        }
    }

    @DisplayName("enabling/disabling BTA")
    @GradleTest
    @JvmGradlePluginTests
    fun testBtaEnabled(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins { kotlin("jvm") }
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, "KOTLIN_BTA_USED=true")
            }

            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
                buildOptions = buildOptions.copy(runViaBuildToolsApi = false),
            ) { fusFiles ->
                assertFilesCombinedContains(fusFiles, "KOTLIN_BTA_USED=false")
            }
        }
    }

    @DisplayName("various compiler execution settings")
    @GradleTest
    @JvmGradlePluginTests
    fun testCompilerExecutionSettings(gradleVersion: GradleVersion) {
        val kotlinVersion = StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize(KOTLIN_VERSION, ";")
        project("simpleProject", gradleVersion) {
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KOTLIN_GRADLE_PLUGIN_VERSION=$kotlinVersion",
                    "KOTLIN_COMPILER_VERSION=$kotlinVersion",
                    "KOTLIN_COMPILER_EXECUTION_POLICY=daemon",
                )
            }

            validateFusFiles(
                "clean",
                "compileKotlin",
                "-Pkotlin.compiler.execution.strategy=in-process",
                buildAction = BuildActions.build,
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KOTLIN_GRADLE_PLUGIN_VERSION=$kotlinVersion",
                    "KOTLIN_COMPILER_VERSION=$kotlinVersion",
                    "KOTLIN_COMPILER_EXECUTION_POLICY=in-process",
                )
            }
            buildScriptInjection {
                @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
                kotlinJvm.compilerVersion.set("2.2.20")
                kotlinJvm.coreLibrariesVersion = "2.2.20"
            }
            validateFusFiles(
                "compileKotlin",
                buildAction = BuildActions.build,
                buildOptions = buildOptions.copy(runViaBuildToolsApi = true),
            ) { fusFiles ->
                assertFilesCombinedContains(
                    fusFiles,
                    "KOTLIN_GRADLE_PLUGIN_VERSION=$kotlinVersion",
                    "KOTLIN_COMPILER_VERSION=2.2.20",
                    "KOTLIN_COMPILER_EXECUTION_POLICY=daemon",
                )
            }
        }
    }

    @DisplayName("FUS should not break project configuration for included build")
    @GradleTest
    @MppGradlePluginTests
    fun testProjectConfiguration(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            val included = project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        iosSimulatorArm64()
                    }
                }
            }
            includeBuild(included)

            validateFusDirectory(
                "help",
                buildAction = BuildActions.build,
            )
        }
    }

    private fun TestProject.applyDokka(version: String) {
        buildGradle.replaceText(
            "plugins {",
            """
            plugins {
                id("org.jetbrains.dokka") version "$version"
            """.trimIndent()
        )
    }

}

private fun Path.assertFusReportContains(vararg expectedMetrics: String) {
    assertFilesCombinedContains(filterKotlinFusFiles(), *expectedMetrics)
    assertFilesCombinedContains(filterBackwardCompatibilityKotlinFusFiles(), *expectedMetrics)
}

private fun Path.assertAllFusReportContains(vararg expectedMetrics: String) {
    listDirectoryEntries().filter { it.endsWith(".finish-profile") }.forEach {
        assertFileContains(it, *expectedMetrics)
    }
}

private fun Path.assertFusReportDoesNotContain(vararg expectedMetrics: String) {
    listDirectoryEntries().forEach {
        assertFileDoesNotContain(it, *expectedMetrics)
    }
}

private fun Path.assertFusReportContainsMetricWithValues(metricName: String, expectedValues: List<String>) {
    assertFilesCombinedContains(filterKotlinFusFiles(), "$metricName=${expectedValues.joinToString(",")}")
    assertFilesCombinedContains(filterBackwardCompatibilityKotlinFusFiles(), "$metricName=${expectedValues.joinToString(";")}")
}
