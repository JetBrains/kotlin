/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KmpIsolatedProjectsSupport
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for multiplatform testing")
class MppTestsIT : KGPBaseTest() {
    @DisplayName("KT-54634: MPP testing logic is compatible with API changes in Gradle 7.6")
    @GradleTest
    fun testKt54634(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                freeArgs = listOf("--continue"), // to ensure that all the tests are run
            )
        ) {
            val nativeTarget = MPPNativeTargets.current

            build(":allTests") {
                assertTasksExecuted(
                    ":jsNodeTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test"
                )
            }

            // break all the test tasks
            kotlinSourcesDir("commonTest").resolve("TestCommonCode.kt").modify {
                it.replace("expectedFun()", "assertEquals(0, 1)")
            }
            buildAndFail(":allTests") {
                assertTasksFailed(
                    ":jsNodeTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test"
                )
                assertOutputDoesNotContain("does not define or inherit an implementation of the resolved method")
                assertOutputDoesNotContain("NoSuchMethodError")
                assertOutputContainsExactlyTimes("AssertionError:", 3)
            }
        }
    }

    // Android Studio has build service which checks all output files of all tasks in task graph
    // https://cs.android.com/android-studio/platform/tools/base/+/0185d5af71ba51c64681731f99f319bfcaeb0174:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/attribution/BuildAnalyzerConfiguratorService.kt;l=78-84
    @DisplayName("KTIJ-25757: MPP is compatible with getting all task output files before execution (Android Studio case)")
    @GradleTest
    fun testKtij25757AllTaskOutputFilesBeforeExecution(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion
        ) {
            buildGradle.modify {
                it + "\n" +
                        """
                        kotlin {
                            js {
                                binaries.executable()
                                browser {
                                }
                            }
                        }
                        
                        gradle.taskGraph.whenReady { taskGraph ->
                            taskGraph.allTasks.forEach { task ->
                                // to not execute, just check configuration
                                task.enabled = false
                                task.outputs.files.forEach { outputFile ->
                                    println(outputFile.path)
                                }
                            }
                        }
                        """.trimIndent()

            }

            build(":assemble") {
            }
        }
    }

    @DisplayName("KT-68638: Kotlin Native Link Task failed to serialize due to configuration resolution error")
    @GradleTest
    fun testKt68638KotlinNativeLinkApiFilesResolutionError(gradleVersion: GradleVersion) {
        project("kt-68638-native-link-self-dependency", gradleVersion) {
            val buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                freeArgs = listOf("--dry-run")
            )
            // no build failure is expected
            build(":p1:linkDebugTestLinuxX64", buildOptions = buildOptions) {}
        }
    }

    @DisplayName("KT-62911: Project Isolation and Project 2 Project Dependencies")
    @GradleTest
    fun testKt62911ProjectIsolationWithP2PDependencies(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.enableIsolatedProjects()
        project("kt-62911-project-isolation-with-p2p-dependencies", gradleVersion, buildOptions = buildOptions) {
            build("assemble")
            assertFileContains(
                projectPath.resolve("shared/build/kotlinProjectStructureMetadata/kotlin-project-structure-metadata.json"),
                """"my-custom-group:my-custom-id""""
            )
        }
    }
}