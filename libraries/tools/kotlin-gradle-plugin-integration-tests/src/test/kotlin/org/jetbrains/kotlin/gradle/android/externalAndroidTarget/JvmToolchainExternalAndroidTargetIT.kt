/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.androidLibrary
import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.testbase.*
import java.io.File
import kotlin.test.assertEquals

@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_813)
@AndroidGradlePluginTests
class JvmToolchainExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - jvmToolchain is applied to androidLibrary and androidHostTest compilation`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.multitarget"
                        withHostTest {}
                    }
                    jvmToolchain(11)

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package sample

                        import android.content.Context

                        class AndroidMain(val context: Context)
                        """.trimIndent()
                    )
                    sourceSets.getByName("androidHostTest").compileSource(
                        """
                        package sample

                        class HostTestProbe
                        """.trimIndent()
                    )
                }
            }


            build(":compileAndroidMain", ":compileAndroidHostTest") {
                assertTasksExecuted(":compileAndroidMain", ":compileAndroidHostTest")
                assertCompilerArgument(":compileAndroidMain", "-jvm-target 11", logLevel = LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-jdk-home ${jdk11Info.jdkRealPath}", logLevel = LogLevel.INFO)
                assertCompilerArgument(":compileAndroidHostTest", "-jvm-target 11", logLevel = LogLevel.INFO)
                assertCompilerArgument(":compileAndroidHostTest", "-jdk-home ${jdk11Info.jdkRealPath}", logLevel = LogLevel.INFO)
            }
        }
    }

    @GradleAndroidTest
    fun `test - jvmToolchain is applied to androidLibrary withJava Kotlin and Java compilations`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.withjava"
                        withJava()
                    }
                    jvmToolchain(11)
                }
            }

            // Read the toolchain chosen for the concrete Java task created by withJava()
            val androidMainJavacJdkHomePathProvider = buildScriptReturn {
                project.tasks
                    .named("compileAndroidMainJavaWithJavac", org.gradle.api.tasks.compile.JavaCompile::class.java).get()
                    .javaCompiler.get() // extracting compiler of this task
                    .metadata
                    .installationPath.asFile // JDK path of this compiler
                    .toPath().toRealPath().toString()
            }

            kotlinSourcesDir("androidMain").resolve("sample/KotlinUsesJava.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                    package sample

                    class KotlinUsesJava {
                        fun ping(): String = JavaPeer().value()
                    }
                    """.trimIndent()
                )
            }

            javaSourcesDir("androidMain").resolve("sample/JavaPeer.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                    package sample;

                    public class JavaPeer {
                        public String value() {
                            return "java";
                        }
                    }
                    """.trimIndent()
                )
            }

            val androidMainJavacJdkHomePath = androidMainJavacJdkHomePathProvider.buildAndReturn(
                ":compileAndroidMainJavaWithJavac",
                buildAction = BuildActions.buildWithAssertions {
                    assertTasksExecuted(":compileAndroidMain", ":compileAndroidMainJavaWithJavac")
                    assertCompilerArgument(":compileAndroidMain", "-jvm-target 11", logLevel = LogLevel.INFO)
                    assertCompilerArgument(":compileAndroidMain", "-jdk-home ${jdk11Info.jdkRealPath}", logLevel = LogLevel.INFO)
                }
            )

            assertEquals(
                jdk11Info.jdkRealPath,
                androidMainJavacJdkHomePath,
                "Expected :compileAndroidMainJavaWithJavac to use jvmToolchain(11). expected='${jdk11Info.jdkRealPath}', actual='$androidMainJavacJdkHomePath'"
            )
        }
    }

    @GradleAndroidTest
    fun `test - jvmToolchain and jvmTarget are applied independently for androidLibrary compilation`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "com.example.lib"
                        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                    }
                    jvmToolchain(17)

                    sourceSets.getByName("androidMain").compileSource(
                        """
                        package com.example.lib

                        import android.content.Context

                        class AndroidMain(val context: Context) {
                            fun increment(): Int = 1
                        }
                        """.trimIndent()
                    )
                }
            }

            build(":compileAndroidMain") {
                assertTasksExecuted(":compileAndroidMain")
                assertCompilerArgument(":compileAndroidMain", "-jvm-target 11", logLevel = LogLevel.INFO)
                assertCompilerArgument(":compileAndroidMain", "-jdk-home ${jdk17Info.jdkRealPath}", logLevel = LogLevel.INFO)
            }
        }
    }
}
