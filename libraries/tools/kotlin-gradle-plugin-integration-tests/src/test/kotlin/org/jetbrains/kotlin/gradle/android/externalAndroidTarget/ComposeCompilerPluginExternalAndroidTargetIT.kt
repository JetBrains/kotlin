/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.kotlin.dsl.getByType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.gradle.kotlin.dsl.kotlin
import kotlin.io.path.listDirectoryEntries

// Used AGP 9.0 as the minimal stable version supported for the android library compose setup.
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
@AndroidGradlePluginTests
class ComposeCompilerPluginExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - androidLibrary and host test with compose compiler plugin compile`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val testProject = project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
                id("org.jetbrains.kotlin.plugin.compose")
            }

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                        target.compileSdk = 34
                        target.namespace = "org.jetbrains.sample.compose.android"
                        target.withHostTest {}
                        target.withDeviceTest {}
                    }
                    iosArm64()
                    sourceSets.getByName("commonMain").dependencies {
                        implementation("org.jetbrains.compose.runtime:runtime:1.9.1")
                    }
                    sourceSets.getByName("commonMain").compileSource(
                        """
                        package sample

                        import androidx.compose.runtime.Composable

                        class SharedApi

                        @Composable
                        fun SharedGreeting(name: String) {
                            Text(name)
                        }

                        @Composable
                        private fun Text(value: String) {
                            println(value)
                        }
                        """.trimIndent()
                    )
                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample

                        import androidx.compose.runtime.Composable

                        class AndroidConsumer(
                            val api: SharedApi = SharedApi(),
                        )

                        @Composable
                        fun AndroidGreeting() {
                            SharedGreeting("android")
                        }

                        @Composable
                        fun AndroidRender() {
                            SharedGreeting("android")
                        }
                        """.trimIndent()
                    )
                    sourceSets.getByName("androidHostTest").dependencies {
                        implementation("org.jetbrains.compose.runtime:runtime:1.9.1")
                        implementation("junit:junit:4.13.2")
                    }
                    sourceSets.getByName("androidHostTest").compileSource(
                        """
                        package sample

                        import androidx.compose.runtime.Composable
                        import org.junit.Test

                        class AndroidHostComposeTest {
                            @Test
                            fun smoke() {
                                println("host")
                            }
                        }

                        @Composable
                        fun HostGreeting() {
                            Text("host")
                        }

                        @Composable
                        private fun Text(value: String) {
                            println(value)
                        }
                        """.trimIndent()
                    )
                }

                val composeCompiler = project.extensions.getByType<ComposeCompilerGradlePluginExtension>()
                composeCompiler.reportsDestination.set(project.layout.buildDirectory.dir("compose-reports"))
            }
        }

        testProject.build(":compileCommonMainKotlinMetadata")

        testProject.build(":compileAndroidMain") {
            assertTasksExecuted(":compileAndroidMain")
            assertComposeCompilerReportsArgument(testProject, ":compileAndroidMain")
            testProject.assertComposeReportsContain(
                "fun sample.SharedGreeting(",
                "fun sample.Text(",
                "fun sample.AndroidGreeting()",
                "fun sample.AndroidRender()",
            )
        }

        testProject.build(":testAndroidHostTest") {
            assertTasksExecuted(
                ":compileAndroidHostTest",
                ":testAndroidHostTest"
            )
            assertComposeCompilerReportsArgument(testProject, ":compileAndroidHostTest")
            testProject.assertComposeReportsContain(
                "fun sample.HostGreeting()",
                "fun sample.Text("
            )
        }
    }

    private fun TestProject.assertComposeReportsContain(vararg expectedText: String) {
        assertFilesCombinedContains(
            composeReportsPath.listDirectoryEntries("*-composables.txt"),
            *expectedText
        )
    }

    private fun BuildResult.assertComposeCompilerReportsArgument(
        testProject: TestProject,
        vararg taskPaths: String,
    ) {
        taskPaths.forEach { taskPath ->
            assertCompilerArgument(
                taskPath,
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${testProject.composeReportsPath.toRealPath()}",
                logLevel = LogLevel.INFO
            )
        }
    }

    private val TestProject.composeReportsPath
        get() = projectPath.resolve("build/compose-reports")
}
