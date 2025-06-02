/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.version
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.filterBackwardCompatibilityKotlinFusFiles
import org.jetbrains.kotlin.gradle.util.filterKotlinFusFiles
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
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
        "KOTLIN_COMPILER_VERSION",
    )

    private val GradleProject.fusStatisticsDirectory: Path
        get() = projectPath.resolve("kotlin-profile")

    @JvmGradlePluginTests
    @DisplayName("for dokka")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testDokka(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            // TODO: KT-70336 dokka doesn't support Configuration Cache
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED)
        ) {
            assertNoErrorFilesCreated {
                applyDokka(TestVersions.ThirdPartyDependencies.DOKKA)
                build("compileKotlin", "dokkaHtml", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        "ENABLED_DOKKA",
                        "ENABLED_DOKKA_HTML"
                    )
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("for dokka v2 html doc")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
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
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
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
            assertNoErrorFilesCreated {
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

                build(
                    "compileKotlin",
                    "dokkaGenerate",
                    "-Porg.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled",
                    "-Pkotlin.session.logger.root.path=$projectPath",
                ) {
                    assertConfigurationCacheStored()
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(*expectedDokkaFusMetrics)
                }

                projectPath.resolve("kotlin-profile").deleteRecursively()
                build("clean")

                build(
                    "compileKotlin",
                    "dokkaGenerate",
                    "-Porg.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled",
                    "-Pkotlin.session.logger.root.path=$projectPath"
                ) {
                    assertConfigurationCacheReused()
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(*expectedDokkaFusMetrics)
                }
            }
        }
    }

    @NativeGradlePluginTests
    @DisplayName("Verify that the metric for applying the Cocoapods plugin is being collected")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testMetricCollectingOfApplyingCocoapodsPlugin(gradleVersion: GradleVersion) {
        project("native-cocoapods-template", gradleVersion) {
            assertNoErrorFilesCreated {
                build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains("COCOAPODS_PLUGIN_ENABLED=true", "ENABLED_HMPP=true", "MPP_PLATFORMS")
                }
            }
        }
    }

    @NativeGradlePluginTests
    @DisplayName("Verify that the metric for native incremental compilation")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testMetricCollectingForNative(gradleVersion: GradleVersion) {
        nativeProject(
            "native-incremental-simple", gradleVersion, buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    incremental = true
                )
            )
        ) {
            assertNoErrorFilesCreated {
                build("linkDebugExecutableHost", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains("KOTLIN_INCREMENTAL_NATIVE_ENABLED=true")
                }
            }
        }
    }

    @JsGradlePluginTests
    @DisplayName("Verify that the metric for applying the Kotlin JS plugin is being collected")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testMetricCollectingOfApplyingKotlinJsPlugin(gradleVersion: GradleVersion) {
        project(
            "simple-js-library",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = IsolatedProjectsMode.DISABLED),
        ) {
            assertNoErrorFilesCreated {
                build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains("KOTLIN_JS_PLUGIN_ENABLED=true")
                }
            }
        }
    }


    @JvmGradlePluginTests
    @DisplayName("Ensure that the metric are not collected if plugins were not applied to simple project")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testAppliedPluginsMetricsAreNotCollectedInSimpleProject(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            assertNoErrorFilesCreated {
                build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(*expectedMetrics)
                    // asserts that we do not put DOKKA metrics everywhere just in case
                    fusStatisticsDirectory.assertFusReportDoesNotContain("ENABLED_DOKKA_HTML", "KOTLIN_JS_PLUGIN_ENABLED")
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("for project with buildSrc")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2]
    )
    fun testProjectWithBuildSrcForGradleVersion7(gradleVersion: GradleVersion) {
        //KT-64022 there are a different build instances in buildSrc and rest project:
        project(
            "instantExecutionWithBuildSrc",
            gradleVersion,
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFilesCombinedContains(
                    projectPath.resolve("kotlin-profile").listDirectoryEntries(),
                    *expectedMetrics,
                    "BUILD_SRC_EXISTS=true"
                )
            }
        }
    }

    @DisplayName("for project with included build")
    @GradleTest
    @JvmGradlePluginTests
    @GradleTestVersions(
        maxVersion = TestVersions.Gradle.G_8_0
    )
    fun testProjectWithIncludedBuild(gradleVersion: GradleVersion) {
        //KT-64022
        //there are a different build instances in buildSrc and rest project

        project("instantExecutionWithIncludedBuildPlugin", gradleVersion) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                fusStatisticsDirectory.assertAllFusReportContains(*expectedMetrics)
            }
            fusStatisticsDirectory.listDirectoryEntries().forEach {
                assertTrue(it.deleteIfExists())
            }

            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                fusStatisticsDirectory.assertAllFusReportContains(*expectedMetrics)
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("for failed build")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testFusStatisticsForFailedBuild(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
        ) {
            assertNoErrorFilesCreated {
                projectPath.resolve("src/main/kotlin/helloWorld.kt").modify {
                    it.replace("java.util.ArrayList", "")
                }
                buildAndFail("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
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
    }

    @JvmGradlePluginTests
    @DisplayName("fus metric for multiproject")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testFusStatisticsForMultiproject(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
        ) {
            assertNoErrorFilesCreated {
                //Collect metrics from BuildMetricsService also
                build(
                    "compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath",
                    buildOptions = defaultBuildOptions
                        .copy(buildReport = listOf(BuildReportType.FILE))
                        // With isolated projects enabled, it creates 2 profile files,
                        // this behavior is tested in [org.jetbrains.kotlin.gradle.FusPluginIT.withConfigurationCacheAndProjectIsolation]
                        .disableIsolatedProjects(),
                ) {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        "CONFIGURATION_IMPLEMENTATION_COUNT=2",
                        "NUMBER_OF_SUBPROJECTS=2",
                        "COMPILATIONS_COUNT=2"
                    )
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("test configuration time ksp metrics")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testFusStatisticsForKsp(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
                id("com.google.devtools.ksp") version (TestVersions.ThirdPartyDependencies.KSP)
            }
            build("help", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertOutputDoesNotContainFusErrors()
                fusStatisticsDirectory.assertFusReportContains(
                    "KSP_GRADLE_PLUGIN_VERSION=1.9.22"
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("fus metric for jvm feature flags")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testFusStatisticsForJvmMultiprojectWithFeatureFlags(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
        ) {
            assertNoErrorFilesCreated {
                //Collect metrics from BuildMetricsService also
                build(
                    "compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath",
                    buildOptions = defaultBuildOptions
                        .copy(
                            buildReport = listOf(BuildReportType.FILE),
                            useFirJvmRunner = true,
                        ).disableIsolatedProjects(),
                ) {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        "CONFIGURATION_IMPLEMENTATION_COUNT=2",
                        "NUMBER_OF_SUBPROJECTS=2",
                        "COMPILATIONS_COUNT=2",
                        "KOTLIN_INCREMENTAL_FIR_RUNNER_ENABLED=true"
                    )
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("general fields with configuration cache")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testFusStatisticsWithConfigurationCache(gradleVersion: GradleVersion) {
        testFusStatisticsWithConfigurationCache(gradleVersion, IsolatedProjectsMode.DISABLED)
    }

    @JvmGradlePluginTests
    @DisplayName("general fields with configuration cache and project isolation")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
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
            assertNoErrorFilesCreated {
                build(
                    "compileKotlin",
                    "-Pkotlin.session.logger.root.path=$projectPath",
                ) {
                    assertConfigurationCacheStored()
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        *expectedMetrics,
                        "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                        "NUMBER_OF_SUBPROJECTS=1",
                        "COMPILATIONS_COUNT=1",
                        "GRADLE_CONFIGURATION_CACHE_ENABLED=true",
                        "GRADLE_PROJECT_ISOLATION_ENABLED=${isProjectIsolationEnabled.toBooleanFlag(gradleVersion)}",
                    )
                }

                fusStatisticsDirectory.listDirectoryEntries()
                    .forEach { assertTrue(it.deleteIfExists(), "Can't delete file ${it.absolutePathString()}") }

                build("clean", buildOptions = buildOptions)

                build(
                    "compileKotlin",
                    "-Pkotlin.session.logger.root.path=$projectPath",
                ) {
                    assertConfigurationCacheReused()
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        *expectedMetrics,
                        "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                        "NUMBER_OF_SUBPROJECTS=1",
                        "COMPILATIONS_COUNT=1"
                    )
                }
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("configuration type metrics")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testConfigurationTypeFusMetrics(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            assertNoErrorFilesCreated {
                build(
                    "compileKotlin",
                    "-Pkotlin.session.logger.root.path=$projectPath",
                ) {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        "CONFIGURATION_COMPILE_ONLY_COUNT=1",
                        "CONFIGURATION_API_COUNT=1",
                        "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                        "CONFIGURATION_RUNTIME_ONLY_COUNT=1",
                    )
                }
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    fun testFusMetricsCanBeDisabled(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("assemble", "-Pkotlin.internal.collectFUSMetrics=false") {
                assertFileNotExists(fusStatisticsDirectory)
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    @GradleTestVersions(additionalVersions = [TestVersions.Gradle.G_8_1, TestVersions.Gradle.G_8_2])
    @Disabled("KT-78390: Requires an updated AtomicFU that would use a newer kotlin-metadata-jvm")
    fun testKotlinxPlugins(gradleVersion: GradleVersion) {
        project(
            "simpleProject", gradleVersion,
            buildOptions = defaultBuildOptions.suppressDeprecationWarningsSinceGradleVersion(
                TestVersions.Gradle.G_8_2,
                gradleVersion,
                "Kover produces Gradle deprecation"
            )
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
            assertNoErrorFilesCreated {
                build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains(
                        "KOTLINX_KOVER_GRADLE_PLUGIN_ENABLED=true",
                        "KOTLINX_SERIALIZATION_GRADLE_PLUGIN_ENABLED=true",
                        "KOTLINX_ATOMICFU_GRADLE_PLUGIN_ENABLED=true",
                        "KOTLINX_BINARY_COMPATIBILITY_GRADLE_PLUGIN_ENABLED=true",
                    )
                }
            }
        }
    }

    @MppGradlePluginTests
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testWasmIncrementalStatisticCollection(gradleVersion: GradleVersion) {
        project(
            "new-mpp-wasm-test",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = IsolatedProjectsMode.DISABLED),
        ) {
            gradleProperties.writeText("kotlin.incremental.wasm=true")

            buildGradleKts.modify {
                it
                    .replace("wasmJs {", "wasmJs {\nbinaries.executable()")
                    .replace("<JsEngine>", "nodejs")
            }

            assertNoErrorFilesCreated {
                build("compileDevelopmentExecutableKotlinWasmJs", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertTasksExecuted(":compileDevelopmentExecutableKotlinWasmJs")
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains("WASM_IR_INCREMENTAL=true")
                }
            }
        }
    }

    @DisplayName("native compiler arguments")
    @GradleTest
    @NativeGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
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

            assertNoErrorFilesCreated {
                build("linkDebugExecutableHost", "-Pkotlin.session.logger.root.path=$projectPath") {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains("ENABLED_NOOP_GC=true")
                }
            }
        }
    }

    // Swift export enabled only on macOS.
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("native swift export - happy path")
    @GradleTest
    @NativeGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testSwiftExportIsReported(gradleVersion: GradleVersion, @TempDir testBuildDir: Path) {
        project("empty", gradleVersion) {
            assertNoErrorFilesCreated {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                    }
                }

                // Check that we generate ENABLED_SWIFT_EXPORT=true when building Swift export.
                build(
                    ":embedSwiftExportForXcode",
                    "-Pkotlin.session.logger.root.path=$projectPath",
                    environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
                ) {
                    assertOutputDoesNotContainFusErrors()
                    fusStatisticsDirectory.assertFusReportContains("ENABLED_SWIFT_EXPORT=true")
                }
            }
        }
    }

    // Swift export enabled only on macOS.
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("native swift export - unhappy path")
    @GradleTest
    @NativeGradlePluginTests
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
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
            build(":linkDebugFrameworkIosArm64", "-Pkotlin.session.logger.root.path=$projectPath") {
                fusStatisticsDirectory.assertFusReportDoesNotContain(
                    "ENABLED_SWIFT_EXPORT=true",
                )
            }
        }
    }

    @DisplayName("add configuration metrics after build was finish")
    @GradleTest
    @MppGradlePluginTests
    @GradleTestVersions(
        //test uses internal internal method `org.gradle.internal.extensions.core.serviceOf`
        minVersion = TestVersions.Gradle.G_8_11,
    )
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
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_2,
    )
    fun concurrencyModificationExceptionTest(gradleVersion: GradleVersion) {
        val rounds = 100
        //TODO KT-79408 fix finish file already exists error
        project(
            "multiClassloaderProject", gradleVersion,
        ) {
            repeat(rounds) {
                build(
                    "compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath", "-Dorg.gradle.parallel=true",
                    buildOptions = defaultBuildOptions.copy(
                        buildReport = listOf(BuildReportType.FILE),
                        isolatedProjects = IsolatedProjectsMode.ENABLED,
                    ),
                ) {
                    assertOutputDoesNotContain("BuildFusService was not registered")
                    assertOutputDoesNotContainFusErrors()
                }

                build("clean", buildOptions = buildOptions)
            }

            assertEquals(getExpectedFusFilesCount(gradleVersion, rounds), fusStatisticsDirectory.filterKotlinFusFiles().size)

            fusStatisticsDirectory.assertFusReportContains(
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
            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContains("Fus metrics won't be collected: CI build is detected via environment variable TEAMCITY_VERSION")
            }
        }
    }

    private fun getExpectedFusFilesCount(gradleVersion: GradleVersion, rounds: Int): Int {
        val expectedFiles = if (gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_9)) {
            //every submodule will create a separate file. There are two modules in the project
            rounds * 2
        } else {
            rounds
        }
        return expectedFiles
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

private fun BuildResult.assertOutputDoesNotContainFusErrors() {
    assertOutputDoesNotContain("finish-profile already exists")
    assertOutputDoesNotContain("Unable to collect finish file for build")
}
