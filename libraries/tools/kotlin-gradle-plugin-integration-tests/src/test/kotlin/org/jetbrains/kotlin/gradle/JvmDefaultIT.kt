/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@DisplayName("'-Xjvm-default' override")
@JvmGradlePluginTests
internal class JvmDefaultIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        logLevel = LogLevel.INFO,
    )

    @DisplayName("No override happens without 'kotlin-dsl' plugin")
    @GradleTest
    fun noOverrideNoKotlinDslPlugin(
        gradleVersion: GradleVersion
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("jvm")
            }

            buildScriptInjection {
                project.applyJvm {
                    compilerOptions.freeCompilerArgs.add("-Xjvm-default=disable")
                }
            }
            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            build(":compileKotlin") {
                assertOutputDoesNotContain("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputDoesNotContain("Overriding '-Xjvm-default=")
            }
        }
    }

    @DisplayName("Should override 'kotlin-dsl' plugin value")
    @GradleTest
    fun overrideKotlinDslPlugin(
        gradleVersion: GradleVersion,
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("jvm")
                id("kotlin-dsl")
            }

            // Applied in Gradle 7.6.3 by 'kotlin-dsl' plugin language version '1.4' is not supported by the latest Kotlin compiler
            if (gradleVersion <= GradleVersion.version(TestVersions.Gradle.G_8_0)) {
                buildScriptInjection {
                    project.afterEvaluate {
                        project.afterEvaluate {
                            project.tasks.named("compileKotlin", KotlinJvmCompile::class.java) {
                                it.compilerOptions {
                                    @Suppress("DEPRECATION")
                                    languageVersion.set(KotlinVersion.KOTLIN_1_8)
                                    @Suppress("DEPRECATION")
                                    apiVersion.set(KotlinVersion.KOTLIN_1_8)
                                }
                            }
                        }
                    }
                }
            }

            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            build(":compileKotlin") {
                assertOutputDoesNotContain("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputContains(
                    "Overriding '-Xjvm-default=all' to stable compiler plugin argument '-jvm-default=no-compatibility' in the presence of 'kotlin-dsl' plugin."
                )
                assertCompilerArgument(":compileKotlin", "-jvm-default no-compatibility", LogLevel.INFO)
                assertNoCompilerArgument(":compileKotlin", "-Xjvm-default all", LogLevel.INFO)
            }
        }
    }

    @DisplayName("Stable -jvm-default option is specified")
    @GradleTest
    fun stableJvmDefaultOptionIsPresent(
        gradleVersion: GradleVersion,
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("jvm")
                id("kotlin-dsl")
            }

            buildScriptInjection {
                project.applyJvm {
                    compilerOptions {
                        jvmDefault.set(JvmDefaultMode.ENABLE)
                    }
                }
            }

            // Applied in Gradle 7.6.3 by 'kotlin-dsl' plugin language version '1.4' is not supported by the latest Kotlin compiler
            if (gradleVersion <= GradleVersion.version(TestVersions.Gradle.G_8_0)) {
                buildScriptInjection {
                    project.afterEvaluate {
                        project.afterEvaluate {
                            project.tasks.named("compileKotlin", KotlinJvmCompile::class.java) {
                                it.compilerOptions {
                                    @Suppress("DEPRECATION")
                                    languageVersion.set(KotlinVersion.KOTLIN_1_8)
                                    @Suppress("DEPRECATION")
                                    apiVersion.set(KotlinVersion.KOTLIN_1_8)
                                }
                            }
                        }
                    }
                }
            }

            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            build(":compileKotlin") {
                assertOutputContains("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputDoesNotContain(
                    "Overriding '-Xjvm-default=all' to stable compiler plugin argument '-jvm-default=no-compatibility' in the presence of 'kotlin-dsl' plugin."
                )

                assertCompilerArgument(":compileKotlin", "-jvm-default enable", LogLevel.INFO)
                assertNoCompilerArgument(":compileKotlin", "-Xjvm-default all", LogLevel.INFO)
            }
        }
    }

    private fun Path.writeMainFun() = resolve("main.kt").writeText(
        //language=kotlin
        """
        |package com.example
        |
        |fun main() {
        |    println("Hello, world!")
        |}
        """.trimMargin()
    )
}