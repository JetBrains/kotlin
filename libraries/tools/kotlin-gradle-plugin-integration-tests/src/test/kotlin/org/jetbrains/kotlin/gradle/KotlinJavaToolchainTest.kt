/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.internal.jvm.JavaInfo
import org.gradle.internal.jvm.Jvm
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class KotlinJavaToolchainTest : KGPBaseTest() {

    @GradleTest
    @DisplayName("Should use by default same jvm as Gradle daemon")
    internal fun byDefaultShouldUseGradleJDK(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            build("assemble") {
                assertDaemonIsUsingJdk(getUserJdk().javaExecutableRealPath)
            }
        }
    }

    @GradleTest
    @DisplayName("Should use provided jdk location to compile Kotlin sources")
    internal fun customJdkHomeLocation(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            useJdk9ToCompile()

            build("assemble") {
                assertDaemonIsUsingJdk(getJdk9().javaExecutableRealPath)
            }
        }
    }

    @GradleTest
    @DisplayName("KotlinCompile task should use build cache when using provided JDK")
    internal fun customJdkBuildCache(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            useJdk9ToCompile()

            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            useJdk9ToCompile()

            build("assemble") {
                assertTasksFromCache(":compileKotlin")
            }
        }
    }

    @GradleTest
    @DisplayName("Kotlin compile task should not use build cache on using different JDK versions")
    internal fun differentJdkBuildCacheMiss(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            forceOutput = true,
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            enableBuildCacheDebug()
            useJdk9ToCompile()
            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            forceOutput = true,
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            enableBuildCacheDebug()
            build("assemble") {
                assertTasksExecuted(":compileKotlin")
            }
        }
    }

    @DisplayName("Kapt task should use only process worker isolation when kotlin java toolchain is set")
    @GradleTest
    internal fun kaptTasksShouldUseProcessWorkersIsolation(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            useJdk9ToCompile()
            gradleProperties.append(
                "kapt.workers.isolation = none"
            )

            build("assemble") {
                assertDaemonIsUsingJdk(getJdk9().javaExecutableRealPath)

                assertOutputContains("Using workers PROCESS isolation mode to run kapt")
                assertOutputContains("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
            }
        }
    }

    @DisplayName("Kapt task should use worker no-isolation mode when build is using Gradle JDK")
    @GradleTest
    internal fun kaptTasksShouldUseNoIsolationModeOnDefaultJvm(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            build("assemble") {
                assertOutputContains("Using workers NONE isolation mode to run kapt")
                assertOutputDoesNotContain("Using non-default Kotlin java toolchain - 'kapt.workers.isolation == none' property is ignored!")
            }
        }
    }

    @DisplayName("Kapt tasks with custom JDK should be cacheable")
    @GradleTest
    internal fun kaptTasksWithCustomJdkCacheable(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            useJdk9ToCompile()

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(buildCache)
            useJdk9ToCompile()

            build("assemble") {
                assertTasksFromCache(
                    ":kaptGenerateStubsKotlin",
                    ":kaptKotlin",
                    ":compileKotlin"
                )
            }
        }
    }

    @DisplayName("Kapt tasks with default JDK and different isolation modes should be cacheable")
    @GradleTest
    internal fun kaptCacheableOnSwitchingIsolationModeAndDefaultJDK(gradleVersion: GradleVersion) {
        val buildCache = workingDir.resolve("custom-jdk-build-cache")
        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "1/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(buildCache)
            gradleProperties.append(
                "kapt.workers.isolation = process"
            )

            build("assemble") {
                assertTasksFromCache(
                    ":kaptGenerateStubsKotlin",
                    ":kaptKotlin",
                    ":compileKotlin"
                )
            }
        }
    }

    private fun BuildResult.assertDaemonIsUsingJdk(
        javaexecPath: String
    ) = assertOutputContains("i: connected to the daemon. Daemon is using following 'java' executable to run itself: $javaexecPath")

    private fun getUserJdk(): JavaInfo = Jvm.forHome(File(System.getenv("JAVA_HOME")))
    private fun getJdk9(): JavaInfo = Jvm.forHome(File(System.getenv("JDK_9")))
    private val JavaInfo.javaExecutableRealPath
        get() = javaExecutable
            .toPath()
            .toRealPath()
            .toAbsolutePath()
            .toString()

    private val String.fullProjectName get() = "kotlin-java-toolchain/$this"

    private fun TestProject.useJdk9ToCompile() {
        // replace required for windows paths so Groovy will not complain about unexpected char '\'
        val jdk9Path = getJdk9().javaHome.absolutePath.replace("\\", "\\\\")
        //language=Groovy
        rootBuildGradle.append(
            """
            import org.gradle.api.JavaVersion
            import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
            
            project.tasks
                 .withType(UsesKotlinJavaToolchain.class)
                 .configureEach {
                      it.kotlinJavaToolchain.setJdkHome(
                           "$jdk9Path",
                           JavaVersion.VERSION_1_9
                      )
                 }
            """.trimIndent()
        )
    }
}