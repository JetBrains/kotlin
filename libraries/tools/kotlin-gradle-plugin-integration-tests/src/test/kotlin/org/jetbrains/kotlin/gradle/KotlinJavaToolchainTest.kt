/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.internal.jvm.JavaInfo
import org.gradle.internal.jvm.Jvm
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

@SimpleGradlePluginTests
@DisplayName("Kotlin Java Toolchain support")
class KotlinJavaToolchainTest : KGPBaseTest() {

    @GradleTestVersions(additionalVersions = ["6.7.1"])
    @GradleTest
    @DisplayName("Should use by default same jvm as Gradle daemon for jdkHome")
    internal fun byDefaultShouldUseGradleJDK(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            build("assemble", enableGradleDebug = true) {
                assertOutputDoesNotContain("'kotlinOptions.jdkHome' is deprecated and will be ignored in Kotlin 1.7!")
                assertJdkHomeIsUsingJdk(getUserJdk().javaHomeRealPath)
            }
        }
    }

    @GradleTestVersions(minVersion = "6.7.1")
    @GradleTest
    @DisplayName("Default Kotlin toolchain should still allow to set Java source and target compatibility")
    internal fun shouldNotFailWithDefaultJdkAndCompatibility(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion
        ) {
            //language=Groovy
            rootBuildGradle.append(
                """
                
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
                """.trimIndent()
            )

            build("assemble")
        }
    }

    @GradleTestVersions(maxVersion = "6.6.1")
    @GradleTest
    @DisplayName("Should use provided jdk location to compile Kotlin sources")
    internal fun customJdkHomeLocation(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
        ) {
            useJdk9ToCompile()

            build("assemble") {
                assertJdkHomeIsUsingJdk(getJdk9().javaHomeRealPath)
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
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdk9ToCompile()
            }

            build("assemble")
        }

        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true)
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdk9ToCompile()
            }

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
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdk9ToCompile()
            }
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
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdk9ToCompile()
            }
            gradleProperties.append(
                "kapt.workers.isolation = none"
            )

            build("assemble") {
                assertJdkHomeIsUsingJdk(
                    if (shouldUseToolchain(gradleVersion)) {
                        getToolchainExecPathFromLogs()
                    } else {
                        getJdk9().javaHomeRealPath
                    }
                )

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
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdk9ToCompile()
            }

            build("assemble")
        }

        project(
            projectName = "simpleWithKapt".fullProjectName,
            gradleVersion = gradleVersion,
            projectPathAdditionalSuffix = "2/cache-test",
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(buildCache)
            if (shouldUseToolchain(gradleVersion)) {
                useToolchainExtension(11)
            } else {
                useJdk9ToCompile()
            }

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

    @DisplayName("User provided jdkHome Kotlin option should produce deprecation warning on Gradle builds")
    @GradleTest
    internal fun jdkHomeIsDeprecated(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            //language=Groovy
            rootBuildGradle.append(
                """
                import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
                
                tasks.withType(KotlinCompile).configureEach {
                    kotlinOptions {
                        jdkHome = "${getJdk9Path()}"
                    }
                }
                """.trimIndent()
            )
            build("assemble") {
                assertJdkHomeIsUsingJdk(getJdk9().javaHomeRealPath)
                assertOutputContains("'kotlinOptions.jdkHome' is deprecated and will be ignored in Kotlin 1.7!")
            }
        }
    }

    @DisplayName("Should allow to set JDK version for tasks via Java toolchain")
    @GradleTestVersions(minVersion = "6.7.1")
    @GradleTest
    internal fun setJdkUsingJavaToolchain(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            forceOutput = true
        ) {
            useToolchainToCompile(11)
            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    @DisplayName("Should allow to set Java toolchain via extension")
    @GradleTestVersions(minVersion = "6.7.1")
    @GradleTest
    internal fun setJdkUsingJavaToolchainViaExtension(gradleVersion: GradleVersion) {
        project(
            projectName = "simple".fullProjectName,
            gradleVersion = gradleVersion,
            forceOutput = true
        ) {
            useToolchainExtension(11)
            build("assemble") {
                assertJdkHomeIsUsingJdk(getToolchainExecPathFromLogs())
            }
        }
    }

    private fun BuildResult.assertJdkHomeIsUsingJdk(
        javaexecPath: String
    ) = assertOutputContains("[KOTLIN] Kotlin compilation 'jdkHome' argument: $javaexecPath")

    private fun getUserJdk(): JavaInfo = Jvm.forHome(File(System.getenv("JAVA_HOME")))
    private fun getJdk9(): JavaInfo = Jvm.forHome(File(System.getenv("JDK_9")))
    // replace required for windows paths so Groovy will not complain about unexpected char '\'
    private fun getJdk9Path(): String = getJdk9().javaHome.absolutePath.replace("\\", "\\\\")
    private val JavaInfo.javaHomeRealPath
        get() = javaHome
            .toPath()
            .toRealPath()
            .toAbsolutePath()
            .toString()

    private val String.fullProjectName get() = "kotlin-java-toolchain/$this"

    private fun TestProject.useJdk9ToCompile() {
        //language=Groovy
        rootBuildGradle.append(
            """
            import org.gradle.api.JavaVersion
            import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
            
            project.tasks
                 .withType(UsesKotlinJavaToolchain.class)
                 .configureEach {
                      it.kotlinJavaToolchain.jdk.use(
                           "${getJdk9Path()}",
                           JavaVersion.VERSION_1_9
                      )
                 }
            """.trimIndent()
        )
    }

    private fun TestProject.useToolchainToCompile(
        jdkVersion: Int
    ) {
        //language=Groovy
        rootBuildGradle.append(
            """
            import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
            
            def toolchain = project.extensions.getByType(JavaPluginExtension.class).toolchain
            toolchain.languageVersion.set(JavaLanguageVersion.of($jdkVersion))
            def service = project.extensions.getByType(JavaToolchainService.class)
            def defaultLauncher = service.launcherFor(toolchain)
                    
            project.tasks
                 .withType(UsesKotlinJavaToolchain.class)
                 .configureEach {
                      it.kotlinJavaToolchain.toolchain.use(
                          defaultLauncher
                      )
                 }
            
            afterEvaluate {
                logger.info("Toolchain jdk path: ${'$'}{defaultLauncher.get().metadata.installationPath.asFile.absolutePath}")
            }
            """.trimIndent()
        )
    }

    private fun TestProject.useToolchainExtension(
        jdkVersion: Int
    ) {
        //language=Groovy
        rootBuildGradle.append(
            """
            import org.gradle.api.plugins.JavaPluginExtension
            import org.gradle.jvm.toolchain.JavaLanguageVersion
            import org.gradle.jvm.toolchain.JavaToolchainService
            
            kotlin {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of($jdkVersion))
                }
            }
            
            afterEvaluate {
                def toolchain = project.extensions.getByType(JavaPluginExtension.class).toolchain
                def service = project.extensions.getByType(JavaToolchainService.class)
                def defaultLauncher = service.launcherFor(toolchain)
                logger.info("Toolchain jdk path: ${'$'}{defaultLauncher.get().metadata.installationPath.asFile.absolutePath}")
            }
            """.trimIndent()
        )
    }

    private fun shouldUseToolchain(gradleVersion: GradleVersion) = gradleVersion >= GradleVersion.version("6.7")

    private fun BuildResult.getToolchainExecPathFromLogs() = output
        .lineSequence()
        .first { it.startsWith("Toolchain jdk path:") }
        .substringAfter("Toolchain jdk path: ")
}