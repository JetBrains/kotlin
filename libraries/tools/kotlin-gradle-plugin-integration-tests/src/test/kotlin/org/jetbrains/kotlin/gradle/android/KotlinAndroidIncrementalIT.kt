/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@DisplayName("kotlin-android incremental compilation tests")
@AndroidGradlePluginTests
open class KotlinAndroidIncrementalIT : KGPBaseTest() {
    @DisplayName("incremental compilation works with single-module android projects")
    @GradleAndroidTest
    fun testIncrementalCompilation(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIncrementalSingleModuleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")

            val modifiedSrc = subProject("app").kotlinSourcesDir().resolve("com/example/getSomething.kt")
            modifiedSrc.writeText(
                //language=kt
                """
                package com.example

                fun getSomething() = 10
                """.trimIndent()
            )

            build("assembleDebug", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                val affectedKotlinFiles = listOf(
                    "app/src/main/kotlin/com/example/KotlinActivity1.kt",
                    "app/src/main/kotlin/com/example/getSomething.kt",
                )
                val affectedJavaFiles = listOf(
                    "app/src/main/java/com/example/JavaActivity.java",
                )
                assertCompiledKotlinSources(affectedKotlinFiles.toPaths(), output)
                assertCompiledJavaSources(affectedJavaFiles.toPaths(), output)
            }
        }
    }

    @DisplayName("incremental compilation works with multi-module android projects")
    @GradleAndroidTest
    fun testMultiModuleIC(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIncrementalMultiModule",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")

            val libAndroidUtilKt = subProject("libAndroid").kotlinSourcesDir().resolve("com/example/libAndroidUtil.kt")
            libAndroidUtilKt.modify { it.replace("fun libAndroidUtil(): String", "fun libAndroidUtil(): CharSequence") }

            val optionsWithDebug = buildOptions.copy(logLevel = LogLevel.DEBUG)

            build("assembleDebug", buildOptions = optionsWithDebug) {
                val affectedSources = listOf(
                    libAndroidUtilKt,
                    subProject("app").kotlinSourcesDir().resolve("com/example/useLibAndroidUtil.kt")
                ).relativizeTo(projectPath)
                assertCompiledKotlinSources(affectedSources, output)
            }

            val libAndroidClassesOnlyUtilKt = subProject("libAndroidClassesOnly").kotlinSourcesDir().resolve("com/example/LibAndroidClassesOnlyUtil.kt")
            libAndroidClassesOnlyUtilKt.modify {
                it.replace(
                    "fun libAndroidClassesOnlyUtil(): String",
                    "fun libAndroidClassesOnlyUtil(): CharSequence"
                )
            }

            build("assembleDebug", buildOptions = optionsWithDebug) {
                val affectedSources = listOf(
                    libAndroidClassesOnlyUtilKt,
                    subProject("app").kotlinSourcesDir().resolve("com/example/useLibAndroidClassesOnlyUtil.kt")
                ).relativizeTo(projectPath)
                assertCompiledKotlinSources(affectedSources, output)
            }

            val libJvmUtilKt = subProject("libJvmClassesOnly").kotlinSourcesDir().resolve("com/example/LibJvmUtil.kt")
            libJvmUtilKt.modify { it.replace("fun libJvmUtil(): String", "fun libJvmUtil(): CharSequence") }

            build("assembleDebug", buildOptions = optionsWithDebug) {
                val affectedSources = listOf(
                    libJvmUtilKt,
                    subProject("app").kotlinSourcesDir().resolve("com/example/useLibJvmUtil.kt")
                ).relativizeTo(projectPath)
                assertCompiledKotlinSources(affectedSources, output)
            }
        }
    }

    @DisplayName("incremental compilation does nothing without changes")
    @GradleAndroidTest
    fun testIncrementalBuildWithNoChanges(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIncrementalSingleModuleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val expectedTasks = listOf(
                ":app:compileDebugKotlin",
                ":app:compileDebugJavaWithJavac"
            )

            build("assembleDebug") {
                assertTasksExecuted(expectedTasks)
            }

            build("assembleDebug") {
                assertTasksUpToDate(expectedTasks)
            }
        }
    }

    @DisplayName("KT-49066: incremental compilation with custom module name")
    @GradleAndroidTest
    fun testCustomModuleName(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIncrementalMultiModule",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("libAndroid").buildGradle.appendText(
                //language=Gradle
                """

                android.kotlinOptions {
                    moduleName = "custom_path"
                }
                """.trimIndent()
            )

            build("assembleDebug") {
                assertFileInProjectExists("libAndroid/build/tmp/kotlin-classes/debug/META-INF/custom_path_debug.kotlin_module")
            }

            val libAndroidUtilKt = subProject("libAndroid").kotlinSourcesDir().resolve("com/example/libAndroidUtil.kt")
            libAndroidUtilKt.modify { it.replace("fun libAndroidUtil(): String", "fun libAndroidUtil(): CharSequence") }
            build("assembleDebug", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                val affectedSources = listOf(
                    libAndroidUtilKt,
                    subProject("app").kotlinSourcesDir().resolve("com/example/useLibAndroidUtil.kt")
                ).relativizeTo(projectPath)
                assertCompiledKotlinSources(affectedSources, output)
            }
        }
    }
}

@DisplayName("Android incremental compilation with disabled precise compilation outputs backup")
class KotlinAndroidIncrementalWithoutPreciseBackupIT : KotlinAndroidIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = false, keepIncrementalCompilationCachesInMemory = false)
}

class KotlinAndroidIncrementalBuildToolsApiDaemonIT : KotlinAndroidIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(runViaBuildToolsApi = true, compilerExecutionStrategy = KotlinCompilerExecutionStrategy.DAEMON)
}

class KotlinAndroidIncrementalBuildToolsApiInProcessIT : KotlinAndroidIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(runViaBuildToolsApi = true, compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS)
}