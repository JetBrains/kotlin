/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.test.assertTrue

@DisplayName("FUS statistic")
class FusStatisticsIT : KGPBaseTest() {
    private val expectedMetrics = arrayOf(
        "OS_TYPE",
        "BUILD_FAILED=false",
        "EXECUTED_FROM_IDEA=false",
        "BUILD_FINISH_TIME",
        "GRADLE_VERSION",
        "KOTLIN_STDLIB_VERSION",
        "KOTLIN_COMPILER_VERSION",
        "USE_CLASSPATH_SNAPSHOT=true"
    )

    private val GradleProject.fusStatisticsPath: Path
        get() = baseFusStatisticsDirectory.getSingleFileInDir()

    private val GradleProject.baseFusStatisticsDirectory: Path
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
            applyDokka(TestVersions.ThirdPartyDependencies.DOKKA)
            build("compileKotlin", "dokkaHtml", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(
                    fusStatisticsPath,
                    "ENABLED_DOKKA",
                    "ENABLED_DOKKA_HTML"
                )
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
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED)
        ) {
            settingsGradle.replaceText(
                "repositories {",
                """
                    repositories {
                         maven { url "https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/" }
                """.trimIndent()
            )

            //for templating-plugin and dokka-base plugins
            buildGradle.replaceText(
                "repositories {",
                """
                    repositories {
                         maven { url "https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/" }
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
                assertFileContains(
                    fusStatisticsPath,
                    *expectedDokkaFusMetrics
                )
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
                assertFileContains(
                    fusStatisticsPath,
                    *expectedDokkaFusMetrics
                )
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
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(fusStatisticsPath, "COCOAPODS_PLUGIN_ENABLED=true", "ENABLED_HMPP=true", "MPP_PLATFORMS")
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
            build("linkDebugExecutableHost", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(fusStatisticsPath, "KOTLIN_INCREMENTAL_NATIVE_ENABLED=true")
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
        project("simple-js-library", gradleVersion) {
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(fusStatisticsPath, "KOTLIN_JS_PLUGIN_ENABLED=true")
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
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                val fusStatisticsPath = fusStatisticsPath
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                )
                assertFileDoesNotContain(
                    fusStatisticsPath,
                    "ENABLED_DOKKA_HTML"
                ) // asserts that we do not put DOKKA metrics everywhere just in case
                assertFileDoesNotContain(fusStatisticsPath, "KOTLIN_JS_PLUGIN_ENABLED")
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

        project(
            "instantExecutionWithIncludedBuildPlugin",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED)
        ) {
            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                projectPath.resolve("kotlin-profile").listDirectoryEntries().forEach {
                    assertFileContains(it, *expectedMetrics)
                }
            }
            projectPath.resolve("kotlin-profile").listDirectoryEntries().forEach {
                assertTrue(it.deleteIfExists())
            }

            build("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                projectPath.resolve("kotlin-profile").listDirectoryEntries().forEach {
                    assertFileContains(it, *expectedMetrics)
                }
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
            projectPath.resolve("src/main/kotlin/helloWorld.kt").modify {
                it.replace("java.util.ArrayList", "")
            }
            buildAndFail("compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(
                    fusStatisticsPath,
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

    @DisplayName("fus metric for multiproject")
    @GradleTest
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    @JvmGradlePluginTests
    fun testFusStatisticsForMultiproject(gradleVersion: GradleVersion) {
        project(
            "incrementalMultiproject", gradleVersion,
        ) {
            //Collect metrics from BuildMetricsService also
            build(
                "compileKotlin", "-Pkotlin.session.logger.root.path=$projectPath",
                buildOptions = defaultBuildOptions.copy(buildReport = listOf(BuildReportType.FILE))
            ) {
                assertFileContains(
                    fusStatisticsPath,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=2",
                    "NUMBER_OF_SUBPROJECTS=2",
                    "COMPILATIONS_COUNT=2"
                )
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
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = isProjectIsolationEnabled,
                buildReport = listOf(BuildReportType.FILE)
            ),
        ) {
            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertConfigurationCacheStored()
                assertFileContains(
                    fusStatisticsPath,
                    *expectedMetrics,
                    "CONFIGURATION_IMPLEMENTATION_COUNT=1",
                    "NUMBER_OF_SUBPROJECTS=1",
                    "COMPILATIONS_COUNT=1",
                    "GRADLE_CONFIGURATION_CACHE_ENABLED=true",
                    "GRADLE_PROJECT_ISOLATION_ENABLED=${isProjectIsolationEnabled.toBooleanFlag(gradleVersion)}",
                )
            }

            assertTrue(fusStatisticsPath.deleteIfExists())
            build("clean", buildOptions = buildOptions)

            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertConfigurationCacheReused()
                assertFileContains(
                    fusStatisticsPath,
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
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_0, TestVersions.Gradle.G_8_2],
    )
    fun testConfigurationTypeFusMetrics(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build(
                "compileKotlin",
                "-Pkotlin.session.logger.root.path=$projectPath",
            ) {
                assertFileContains(
                    fusStatisticsPath,
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
            build("assemble", "-Pkotlin.internal.collectFUSMetrics=false") {
                val fusStatisticsPath = baseFusStatisticsDirectory
                assertFileNotExists(fusStatisticsPath)
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    @GradleTestVersions(additionalVersions = [TestVersions.Gradle.G_8_1, TestVersions.Gradle.G_8_2])
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
            build("assemble", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(
                    fusStatisticsPath,
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
    @GradleTestVersions(
        additionalVersions = [TestVersions.Gradle.G_8_2],
    )
    fun testWasmIncrementalStatisticCollection(gradleVersion: GradleVersion) {
        project(
            "new-mpp-wasm-test", gradleVersion
        ) {
            gradleProperties.writeText("kotlin.incremental.wasm=true")

            buildGradleKts.modify {
                it
                    .replace("wasmJs {", "wasmJs {\nbinaries.executable()")
                    .replace("<JsEngine>", "nodejs")
            }

            build("compileDevelopmentExecutableKotlinWasmJs", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinWasmJs")
                assertFileContains(
                    fusStatisticsPath,
                    "WASM_IR_INCREMENTAL=true",
                )
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
                |        kotlinOptions {
                |            freeCompilerArgs += listOf("-Xbinary=gc=noop")
                |        }
                |    }
                |}
                """.trimMargin())

            build("linkDebugExecutableHost", "-Pkotlin.session.logger.root.path=$projectPath") {
                assertFileContains(
                    fusStatisticsPath,
                    "ENABLED_NOOP_GC=true",
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
        //Test uses deprecated Gradle features
        project("multiplatformFlowAction", gradleVersion, buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)) {
            buildScriptInjection {
                project.tasks.register("doNothing"){}
            }

            build("doNothing")
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