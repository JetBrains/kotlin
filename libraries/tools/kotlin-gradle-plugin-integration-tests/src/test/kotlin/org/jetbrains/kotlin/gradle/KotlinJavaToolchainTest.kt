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