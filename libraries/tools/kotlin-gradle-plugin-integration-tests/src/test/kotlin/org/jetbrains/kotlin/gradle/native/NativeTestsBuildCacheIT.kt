/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("Kotlin/Native test tasks build cache")
@NativeGradlePluginTests
class NativeTestsBuildCacheIT : KGPBaseTest() {

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(buildCacheEnabled = true)

    private val localBuildCacheDir
        get() = workingDir.resolve("native-tests-build-cache")

    /**
     * The native-tests project tracks ANDROID_HOME, and its value comes from
     * the environment of the Gradle process running the build. TestKit
     * daemons are shared between parallel test forks, so builds of this test
     * class may be served by daemons started with different environments
     * (e.g. by other test classes passing ANDROID_HOME), which would
     * silently change the value of the tracked input and the cache key. Pin
     * the variable to a fixed value so that all builds observe the same
     * environment; [testTrackedEnvironmentVariableInvalidatesCache] varies
     * it explicitly instead.
     */
    private val stableEnvironment
        get() = EnvironmentalVariables(mapOf("ANDROID_HOME" to "/stable"))

    @TestMetadata("native-tests")
    @DisplayName("hostTest should be stored to and restored from the build cache")
    @GradleTest
    fun testHostTestFromCache(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion, environmentVariables = stableEnvironment) {
            enableLocalBuildCache(localBuildCacheDir)

            build("hostTest") {
                assertTasksExecuted(":hostTest")
                assertTasksPackedToCache(":hostTest")
            }

            build("clean", "hostTest") {
                assertTasksFromCache(":hostTest")
                // The restored task output contains the recorded test results:
                assertDirectoryInProjectExists("build/test-results/hostTest/binary")
            }

            // Changing the test source changes the test executable, so the
            // cache entry is not reused and the tests are executed again:
            projectPath.resolve("src/commonTest/kotlin/test.kt").appendText(
                """
                |
                |@Test
                |fun bazTest() {
                |    println(foo())
                |}
                """.trimMargin()
            )

            build("hostTest") {
                assertTasksExecuted(":hostTest")
            }
        }
    }

    @TestMetadata("native-tests")
    @DisplayName("suppressed test failures are cached and detected in restored results")
    @GradleTest
    fun testSuppressedFailuresAreCachedAndDetectedOnRestore(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion, environmentVariables = stableEnvironment) {
            enableLocalBuildCache(localBuildCacheDir)

            projectPath.resolve("src/commonTest/kotlin/test.kt").appendText(
                """
                |
                |@Test
                |fun fail() {
                |    throw IllegalStateException("FAILURE!")
                |}
                """.trimMargin()
            )

            // With the aggregated test report (allTests) in the task graph, the
            // failures of individual test tasks are suppressed, so hostTest
            // succeeds despite the failing test, and its execution (including
            // the recorded failure count) is stored in the build cache:
            buildAndFail("check") {
                assertTasksFailed(":allTests")
                assertTasksExecuted(":hostTest")
                assertTasksPackedToCache(":hostTest")
            }

            // The restored execution still fails the build: the aggregated
            // report detects the failures recorded in the restored outputs,
            // which the listener-based detection cannot do for FROM-CACHE
            // tasks:
            buildAndFail("clean", "check") {
                assertTasksFromCache(":hostTest")
                assertTasksFailed(":allTests")
            }

            // Standalone executions use a different cache key (failures are
            // not suppressed without the aggregate), so the stored entry can
            // never be restored there and the failing tests fail the task:
            buildAndFail("hostTest") {
                assertTasksFailed(":hostTest")
            }
        }
    }

    @TestMetadata("native-tests")
    @DisplayName("tracked environment variables should participate in the cache key")
    @GradleTest
    fun testTrackedEnvironmentVariableInvalidatesCache(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            // native-tests tracks ANDROID_HOME on its test tasks:
            val androidHomeFirst = EnvironmentalVariables(mapOf("ANDROID_HOME" to "/first"))
            val androidHomeSecond = EnvironmentalVariables(mapOf("ANDROID_HOME" to "/second"))

            build("hostTest", environmentVariables = androidHomeFirst) {
                assertTasksPackedToCache(":hostTest")
            }

            // Changing the tracked variable changes the cache key, so the tests
            // are executed again:
            build("hostTest", environmentVariables = androidHomeSecond) {
                assertTasksExecuted(":hostTest")
            }

            // The up-to-date check happens before the cache lookup, so the build
            // dir has to be cleaned to observe the restored entries:
            build("clean", "hostTest", environmentVariables = androidHomeSecond) {
                assertTasksFromCache(":hostTest")
            }

            // The entry stored under the first value is still in the cache:
            build("clean", "hostTest", environmentVariables = androidHomeFirst) {
                assertTasksFromCache(":hostTest")
            }
        }
    }

    @TestMetadata("native-tests")
    @DisplayName("cache entries should be reusable across different checkout paths")
    @GradleTest
    fun testCacheRelocation(gradleVersion: GradleVersion) {
        val firstProject = nativeProject("native-tests", gradleVersion, environmentVariables = stableEnvironment) {
            enableLocalBuildCache(localBuildCacheDir)
        }
        firstProject.build("hostTest") {
            assertTasksPackedToCache(":hostTest")
        }

        // The same project at a different absolute path: the inputs of the test
        // task do not leak the checkout path, so the entry is reused:
        val secondProject = nativeProject("native-tests", gradleVersion, environmentVariables = stableEnvironment) {
            enableLocalBuildCache(localBuildCacheDir)
        }
        secondProject.build("hostTest") {
            assertTasksFromCache(":hostTest")
        }
    }
}
