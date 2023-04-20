/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.Kapt3BaseIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

@DisplayName("android with kapt3 incremental build tests")
@AndroidGradlePluginTests
open class Kapt3AndroidIncrementalIT : Kapt3BaseIT() {
    @DisplayName("stubs generation is incremental on changes in android variant java sources")
    @GradleAndroidTest
    fun generateStubsTaskShouldRunIncrementallyOnChangesInAndroidVariantJavaSources(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-dagger".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val javaFile = subProject("app").javaSourcesDir().resolve("com/example/dagger/kotlin/Utils.java")
            javaFile.writeText(
                //language=Java
                """
                package com.example.dagger.kotlin;

                class Utils {
                    public String oneMethod() {
                        return "fake!";
                    }
                }
                """.trimIndent()
            )

            build(":app:kaptDebugKotlin") {
                assertTasksExecuted(":app:kaptGenerateStubsDebugKotlin")
            }

            javaFile.writeText(
                //language=Java
                """
                package com.example.dagger.kotlin;

                class Utils {
                    public String oneMethod() {
                        return "fake!";
                    }
                    
                    public void anotherMethod() {
                        int one = 1;
                    }
                }
                """.trimIndent()
            )

            build(":app:kaptDebugKotlin") {
                assertTasksExecuted(":app:kaptGenerateStubsDebugKotlin")
                assertOutputDoesNotContain(
                    "The input changes require a full rebuild for incremental task ':app:kaptGenerateStubsDebugKotlin'."
                )
            }
        }
    }

    @DisplayName("incremental compilation works with dagger")
    @GradleAndroidTest
    fun testAndroidDaggerIC(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidDaggerProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")

            val androidModuleKt = subProject("app").javaSourcesDir().resolve("com/example/dagger/kotlin/AndroidModule.kt")
            androidModuleKt.modify {
                it.replace(
                    "fun provideApplicationContext(): Context {",
                    "fun provideApplicationContext(): Context? {"
                )
            }

            build(":app:assembleDebug", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(
                    ":app:kaptGenerateStubsDebugKotlin",
                    ":app:kaptDebugKotlin",
                    ":app:compileDebugKotlin",
                    ":app:compileDebugJavaWithJavac"
                )

                // Output is combined with previous build, but we are only interested in the compilation
                // from second build to avoid false positive test failure
                val filteredOutput = output
                    .lineSequence()
                    .filter { it.contains("[KOTLIN] compile iteration:") }
                    .drop(1)
                    .joinToString(separator = "/n")
                assertCompiledKotlinSources(listOf(androidModuleKt).relativizeTo(projectPath), output = filteredOutput)
            }
        }
    }

    @DisplayName("incremental compilation with android and kapt")
    @GradleAndroidTest
    fun testAndroidWithKaptIncremental(
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
            val appProject = subProject("app")
            appProject.buildGradle.modify {
                //language=Gradle
                """
                apply plugin: 'org.jetbrains.kotlin.kapt'
                $it
                """.trimIndent()
            }

            build(":app:testDebugUnitTest")

            appProject.kotlinSourcesDir().resolve("com/example/KotlinActivity.kt").appendText(
                //language=kt
                """
                {
                  private val x = 1
                }
                """.trimIndent()
            )

            build(":app:testDebugUnitTest") {
                assertOutputDoesNotContain(NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED)
            }
        }
    }

    @DisplayName("inter-project IC works with kapt")
    @GradleAndroidTest
    fun testInterProjectIC(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-inter-project-ic".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertKaptSuccessful()
            }

            fun modifyAndCheck(utilKt: Path, useUtilFileName: String) {
                utilKt.modify {
                    it.checkedReplace("Int", "Number")
                }

                build("assembleDebug", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                    val affectedFile = subProject("app").kotlinSourcesDir().resolve("org.example.inter.project.ic")
                        .resolve(useUtilFileName).relativeTo(projectPath)
                    assertCompiledKotlinSources(
                        listOf(affectedFile),
                        getOutputForTask(":app:kaptGenerateStubsDebugKotlin"),
                        errorMessageSuffix = " in task ':app:kaptGenerateStubsDebugKotlin"
                    )
                    assertCompiledKotlinSources(
                        listOf(affectedFile),
                        getOutputForTask(":app:compileDebugKotlin"),
                        errorMessageSuffix = " in task ':app:compileDebugKotlin"
                    )
                }
            }

            val libAndroidProject = subProject("lib-android")
            modifyAndCheck(libAndroidProject.kotlinSourcesDir().resolve("libAndroidUtil.kt"), "useLibAndroidUtil.kt")
            val libJvmProject = subProject("lib-jvm")
            modifyAndCheck(libJvmProject.kotlinSourcesDir().resolve("libJvmUtil.kt"), "useLibJvmUtil.kt")
        }
    }
}

@DisplayName("android with kapt3 incremental build tests with precise compilation outputs backup")
class Kapt3AndroidIncrementalWithPreciseBackupIT : Kapt3AndroidIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
}