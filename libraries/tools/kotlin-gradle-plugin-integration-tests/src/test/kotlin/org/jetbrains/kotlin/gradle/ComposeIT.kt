/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.writeText

@DisplayName("Compose compiler Gradle plugin")
class ComposeIT : KGPBaseTest() {

    // AGP 8.6.0+ autoconfigures compose in the presence of Kotlin Compose plugin
    @DisplayName("Should not affect Android project where compose is not enabled")
    @AndroidTestVersions(maxVersion = TestVersions.AGP.AGP_85)
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleApp")
    fun testAndroidDisabledCompose(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.compose"
                |${originalBuildScript.substringAfter("plugins {")}
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:1.6.4"
                |}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            build("assembleDebug") {
                assertOutputDoesNotContain("Detected Android Gradle Plugin compose compiler configuration")
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
                assertCompilerArgument(
                    ":compileDebugKotlin",
                    "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=true," +
                            "plugin:androidx.compose.compiler.plugins.kotlin:traceMarkersEnabled=true",
                    LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("Should conditionally suggest to migrate to new compose plugin")
    @AndroidTestVersions(
        maxVersion = TestVersions.AGP.AGP_86,
        additionalVersions = [TestVersions.AGP.AGP_85]
    )
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleApp")
    fun testAndroidComposeSuggestion(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |$originalBuildScript
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:1.6.4"
                |}
                |
                |android.buildFeatures.compose = true
                |
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            buildAndFail("assembleDebug") {
                when (agpVersion) {
                    TestVersions.AgpCompatibilityMatrix.AGP_82.version,
                    TestVersions.AgpCompatibilityMatrix.AGP_83.version,
                    TestVersions.AgpCompatibilityMatrix.AGP_84.version,
                        -> {
                        assertOutputContains(APPLY_COMPOSE_SUGGESTION)
                    }
                    else -> {
                        // This error should come from AGP side
                        assertOutputContains(
                            "Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required\n" +
                                    "  when compose is enabled. See the following link for more information:\n" +
                                    "  https://d.android.com/r/studio-ui/compose-compiler"
                        )
                    }
                }
            }
        }
    }

    @DisplayName("Should work correctly when compose in Android is enabled")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleComposeApp")
    fun testAndroidWithCompose(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            build("assembleDebug") {
                assertOutputContains("Detected Android Gradle Plugin compose compiler configuration")
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
            }
        }
    }

    @DisplayName("Should not break build cache relocation")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("AndroidSimpleComposeApp")
    fun testAndroidBuildCacheRelocation(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        @TempDir localCacheDir: Path,
    ) {
        val project1 = androidComposeAppProjectWithLocalCacheEnabled(
            gradleVersion,
            agpVersion,
            providedJdk,
            localCacheDir
        )

        val project2 = androidComposeAppProjectWithLocalCacheEnabled(
            gradleVersion,
            agpVersion,
            providedJdk,
            localCacheDir
        )

        project1.build(
            "assembleDebug",
        ) {
            assertTasksExecuted(":compileDebugKotlin")
        }

        project2.build("assembleDebug") {
            assertTasksFromCache(":compileDebugKotlin")
        }
    }

    @DisplayName("Should work with JB Compose plugin")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("JBComposeApp")
    fun testJBCompose(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        var buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
            .suppressDeprecationWarningsOn(
                "JB Compose produces deprecation warning: CMP-3945"
            ) {
                gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_4) &&
                        gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_0)
            }
        if (OS.WINDOWS.isCurrentOs) {
            // CMP-8375 Compose Gradle Plugin is not compatible with Gradle isolated projects on Windows
            buildOptions = buildOptions.disableIsolatedProjects()
        }

        project(
            projectName = "JBComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = buildOptions,
        ) {
            val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(agpVersion)
            build(":composeApp:assembleDebug") {
                // AGP autoconfigures compose in the presence of Kotlin Compose plugin
                if (agpVersion <= TestVersions.AgpCompatibilityMatrix.AGP_85) {
                    assertOutputDoesNotContain("Detected Android Gradle Plugin compose compiler configuration")
                    assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
                }
            }

            build(":composeApp:desktopJar") {
                if (agpVersion <= TestVersions.AgpCompatibilityMatrix.AGP_85) {
                    assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
                }
            }
        }
    }

    @DisplayName("Should not suggest apply Kotlin compose plugin in JB Compose plugin")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    @TestMetadata("JBComposeApp")
    fun testAndroidJBComposeNoSuggestion(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
    ) {
        project(
            projectName = "JBComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            subProject("composeApp").buildGradleKts.modify {
                it.replace("kotlin(\"plugin.compose\")", "")
            }

            buildAndFail(":composeApp:assembleDebug") {
                assertOutputDoesNotContain(APPLY_COMPOSE_SUGGESTION)
            }
        }
    }

    private fun androidComposeAppProjectWithLocalCacheEnabled(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk,
        localCacheDir: Path,
    ): TestProject {
        return project(
            projectName = "AndroidSimpleComposeApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions
                .copy(
                    androidVersion = agpVersion,
                    buildCacheEnabled = true,
                )
        ) {
            projectPath.resolve("stability-configuration.conf").writeText(
                """
                |// Consider LocalDateTime stable
                |java.time.LocalDateTime
                |// Consider kotlin collections stable
                |kotlin.collections.*
                """.trimMargin()
            )
            buildGradleKts.appendText(
                """
                |
                |composeCompiler {
                |    metricsDestination.set(project.layout.buildDirectory.dir("metrics"))
                |    reportsDestination.set(project.layout.buildDirectory.dir("reports"))
                |    stabilityConfigurationFile.set(project.layout.projectDirectory.file("stability-configuration.conf"))
                |}
                """.trimMargin()
            )

            enableLocalBuildCache(localCacheDir)
        }
    }

    @DisplayName("Run Compose compiler with runtime v1.0")
    @GradleAndroidTest
    @OtherGradlePluginTests
    @TestMetadata("AndroidSimpleApp")
    fun testComposePluginWithRuntimeV1_0(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.compose"
                |${originalBuildScript.substringAfter("plugins {")}
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:1.0.0"
                |}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            val composableFile = projectPath.resolve("src/main/kotlin/com/example/Compose.kt").createFile()
            composableFile.appendText(
                """
                |package com.example
                |
                |import androidx.compose.runtime.Composable
                |
                |@Composable fun Test() { Test() }
            """.trimMargin())

            build("assembleDebug") {
                assertTasksExecuted(":compileDebugKotlin")
            }
        }
    }

    @DisplayName("Run Compose compiler with the latest runtime")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.MAX_SUPPORTED)
    @OtherGradlePluginTests
    @TestMetadata("AndroidSimpleApp")
    fun testComposePluginWithRuntimeLatest(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        val composeSnapshotId = TestVersions.Compose.composeSnapshotId
        val composeSnapshotVersion = TestVersions.Compose.composeSnapshotVersion
        project(
            projectName = "AndroidSimpleApp",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                additionalRepos = setOf("https://androidx.dev/snapshots/builds/${composeSnapshotId}/artifacts/repository")
            )
        ) {
            buildGradle.modify { originalBuildScript ->
                """
                |plugins {
                |    id "org.jetbrains.kotlin.plugin.compose"
                |${originalBuildScript.substringAfter("plugins {")}
                |
                |dependencies {
                |    implementation "androidx.compose.runtime:runtime:$composeSnapshotVersion"
                |}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                android.useAndroidX=true
                """.trimIndent()
            )

            val composableFile = projectPath.resolve("src/main/kotlin/com/example/Compose.kt").createFile()
            composableFile.appendText(
                """
                |package com.example
                |
                |import androidx.compose.runtime.Composable
                |
                |@Composable fun Test() { Test() }
            """.trimMargin())

            build("assembleDebug") {
                assertTasksExecuted(":compileDebugKotlin")
            }
        }
    }

    @DisplayName("Run test against older versions of open @Composable function")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.MAX_SUPPORTED)
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_14) // Kotlin 1.9.2x is not compatible with Gradle 9+
    @OtherGradlePluginTests
    @TestMetadata("composeMultiModule")
    fun testComposeDefaultParamsInOpenFunctionK1ToK2(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        val composeSnapshotId = TestVersions.Compose.composeSnapshotId
        val composeSnapshotVersion = TestVersions.Compose.composeSnapshotVersion
        project(
            projectName = "composeMultiModule/dep",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion, kotlinVersion = "1.9.21")
                .suppressDeprecationWarningsSinceGradleVersion(
                    TestVersions.Gradle.G_8_13,
                    gradleVersion,
                    "Old Kotlin release produces deprecation warning"
                )
        ) {
            val composeBase = projectPath.resolve("src/main/kotlin/com/example/Compose.kt").createFile()
            composeBase.writeText(
                //language=kotlin
                """
                |package com.example
                |
                |import androidx.compose.runtime.Composable
                |
                |open class TestComposable {
                |    @Composable
                |    open fun UnitFun(value: Int = 42) {
                |    }
                |
                |    @Composable
                |    open fun openFun(value: Int = 42): Int {
                |       return value
                |    }
                |}
                |
                |interface TestInterface {
                |    @Composable fun Content()
                |}
                |
                |class OtherModuleImpl : TestInterface {
                |    @Composable override fun Content() {}
                |}
                """.trimMargin()
            )
            build("publishToMavenLocal") {
                assertTasksExecuted(":compileReleaseKotlin")
            }
        }
        project(
            projectName = "composeMultiModule",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                additionalRepos = setOf("https://androidx.dev/snapshots/builds/${composeSnapshotId}/artifacts/repository")
            )
        ) {
            buildGradleKts.appendComposePlugin()
            buildGradleKts.appendText(
                """
                |
                |dependencies {
                |    implementation("androidx.compose.runtime:runtime:$composeSnapshotVersion")
                |    implementation("androidx.compose.runtime:runtime-test-utils:$composeSnapshotVersion")
                |    
                |    implementation("com.example:dep:1.0")
                |}
                """.trimMargin()
            )

            val testFile = projectPath.resolve("src/test/kotlin/com/example/ComposeTest.kt")
            testFile.writeText(
                //language=kotlin
                """
                |package com.example
                |
                |import androidx.compose.runtime.*
                |import androidx.compose.runtime.mock.*
                |import org.junit.Test
                |
                |class ComposeTest {
                |    @Test
                |    fun test() = compositionTest {
                |       val testImpl = TestImpl()
                |       val otherModuleImpl = OtherModuleImpl()
                |       compose {
                |           testImpl.UnitFun(1)
                |           testImpl.UnitFun()
                |           Text("${'$'}{testImpl.openFun(1)}")
                |           Text("${'$'}{testImpl.openFun()}")
                |           otherModuleImpl.Content() // Just executing this successfully is enough
                |       }
                |       
                |       validate {
                |           Text("1")
                |           Text("0") // Expected bug behavior
                |           Text("1")
                |           Text("0") // Expected bug behavior
                |       }
                |    }
                |}
                |
                |private class TestImpl : TestComposable() {
                |    @Composable
                |    override fun UnitFun(value: Int) {
                |       Text("${'$'}value")
                |    }
                |
                |    @Composable
                |    override fun openFun(value: Int): Int {
                |       return super.openFun(value)
                |    }
                |}
                """.trimMargin()
            )

            build("testReleaseUnitTest") {
                assertTasksExecuted(":compileReleaseUnitTestKotlin")
                assertOutputContainsExactlyTimes(LEGACY_OPEN_FUNCTION_WARNING, 2)
            }
        }
    }

    @DisplayName("Run source information test with older versions of Compose runtime")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.MAX_SUPPORTED)
    @OtherGradlePluginTests
    @TestMetadata("composeMultiModule")
    fun testComposeSourceInformationOldRuntime(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "composeMultiModule",
            gradleVersion = gradleVersion,
            buildJdk = providedJdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            dependencyManagement = DependencyManagement.DefaultDependencyManagement()
        ) {
            buildGradleKts.appendComposePlugin()
            buildScriptInjection {
                project.configurations.getByName("implementation").dependencies.add(
                    project.dependencies.create("androidx.compose.runtime:runtime:1.8.1")
                )
            }

            val testFile = projectPath.resolve("src/test/kotlin/com/example/ComposeTest.kt")
            testFile.writeText(
                //language=kotlin
                """
                package com.example
                
                import androidx.compose.runtime.*
                import androidx.compose.runtime.tooling.*
                import kotlinx.coroutines.test.runTest
                import kotlinx.coroutines.launch
                import kotlin.coroutines.EmptyCoroutineContext
                
                import org.junit.Test
                import org.junit.Assert.assertEquals
                
                class ComposeTest {
                    @Test
                    fun test() = runTest {
                        val clock = object : MonotonicFrameClock {
                            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                                return onFrame(0L)
                            }
                        }
                        val recomposer = Recomposer(EmptyCoroutineContext)
                        launch(clock) {
                            recomposer.runRecomposeAndApplyChanges()
                        }
                
                        val applier = object : AbstractApplier<Unit>(Unit) {
                            override fun insertBottomUp(index: Int, instance: Unit) {}
                            override fun insertTopDown(index: Int, instance: Unit) {}
                            override fun move(from: Int, to: Int, count: Int) {}
                            override fun remove(index: Int, count: Int) {}
                            override fun onClear() {}
                        }
                
                        val composition = Composition(applier, recomposer)
                        var composer: Composer? = null
                
                        val content = @Composable {
                            Text("Hello, World!", 0)
                        }
                
                        composition.setContent {
                            composer = currentComposer
                            currentComposer.collectParameterInformation()
                
                            content()
                        }
                
                        fun CompositionData.flatten(): List<CompositionGroup> =
                            compositionGroups.flatMap { it.flatten() + it }
                
                        val textGroup = composer!!.compositionData.flatten().find { it.sourceInfo?.contains("Text") == true }
                        assertEquals(
                            "C(Text)P(1):ComposeTest.kt#to5c3",
                            textGroup?.sourceInfo
                        )
                
                        recomposer.cancel()
                    }
                }
                
                @Composable fun Text(value: String, modifier: Int) {
                    println(value)
                    println(modifier)
                }
                """.trimIndent()
            )

            build("testReleaseUnitTest") {
                assertTasksExecuted(":testReleaseUnitTest")
                assertOutputDoesNotContain("org.junit.ComparisonFailure")
            }
        }
    }

    private fun Path.appendComposePlugin() {
        modify { originalBuildScript ->
            """
                |${originalBuildScript.substringBefore("plugins {")}
                |plugins {
                |    id("org.jetbrains.kotlin.plugin.compose")
                |${originalBuildScript.substringAfter("plugins {")}
            """.trimMargin()
        }
    }

    companion object {
        private const val APPLY_COMPOSE_SUGGESTION =
            "The Compose compiler plugin is now a part of Kotlin.\n" +
                    "Please apply the 'org.jetbrains.kotlin.plugin.compose' Gradle plugin to enable the Compose compiler plugin.\n" +
                    "Learn more about this at https://kotl.in/compose-plugin"

        private const val LEGACY_OPEN_FUNCTION_WARNING =
            "Detected a @Composable function that overrides an open function compiled with older compiler that is known to crash " +
                    "at runtime."
    }
}
