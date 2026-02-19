/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.util.useCompilerVersion
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

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

            overrideOldGradleBoundLanguageVersionsWith19()
            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            checkJvmDefaultReplacement(
                "no-compatibility",
                null
            ) {
                assertOutputDoesNotContain("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputContains(
                    "Overriding '-Xjvm-default=all' to stable compiler plugin argument '-jvm-default=no-compatibility' in the presence of 'kotlin-dsl' plugin."
                )
            }
        }
    }

    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    @DisplayName("Should not override 'kotlin-dsl' plugin value if using BTA with older Kotlin compiler version")
    @GradleTest
    fun notOverrideKotlinDslPluginOnUsingBtaWithOlderCompilerVersion(
        gradleVersion: GradleVersion,
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("jvm")
                id("kotlin-dsl")
            }

            gradleProperties.appendText(
                """
                kotlin.compiler.runViaBuildToolsApi=true
                """.trimIndent()
            )
            buildScriptInjection {
                useCompilerVersion("2.2.0-RC")
            }
            overrideOldGradleBoundLanguageVersionsWith19()
            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            build(":compileKotlin") {
                assertOutputDoesNotContain("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputDoesNotContain("Overriding '-Xjvm-default=")
            }
        }
    }

    @DisplayName("Should override 'kotlin-dsl' plugin value if using BTA with current Kotlin compiler version")
    @GradleTest
    fun overrideKotlinDslPluginOnUsingBtaWithCurrentCompilerVersion(
        gradleVersion: GradleVersion,
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("jvm")
                id("kotlin-dsl")
            }

            gradleProperties.appendText(
                """
                kotlin.compiler.runViaBuildToolsApi=true
                """.trimIndent()
            )

            overrideOldGradleBoundLanguageVersionsWith19()
            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            checkJvmDefaultReplacement(
                "no-compatibility",
                null,
            ) {
                assertOutputDoesNotContain("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputContains(
                    "Overriding '-Xjvm-default=all' to stable compiler plugin argument '-jvm-default=no-compatibility' in the presence of 'kotlin-dsl' plugin."
                )
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
            overrideOldGradleBoundLanguageVersionsWith19()

            kotlinSourcesDir().also { it.createDirectories() }.writeMainFun()

            checkJvmDefaultReplacement(
                "enable",
                null,
            ) {
                assertOutputContains("Stable '-jvm-default' argument is configured in the presence of 'kotlin-dsl' plugin, no need to override.")
                assertOutputDoesNotContain(
                    "Overriding '-Xjvm-default=all' to stable compiler plugin argument '-jvm-default=no-compatibility' in the presence of 'kotlin-dsl' plugin."
                )
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

    // Gradle versions 8.* and before use either LV 1.4 or 1.8, but they both are currently disabled
    private fun TestProject.overrideOldGradleBoundLanguageVersionsWith19() {
        buildScriptInjection {
            project.afterEvaluate {
                project.afterEvaluate {
                    project.tasks.named("compileKotlin", KotlinJvmCompile::class.java) {
                        @Suppress("DEPRECATION")
                        it.compilerOptions {
                            // Note: with LV 2.0 here, the test notOverrideKotlinDslPluginOnUsingBtaWithOlderCompilerVersion
                            // fails on Gradle 8.14 with CNFE: KtDiagnosticsContainer
                            // Probably some conflict arises between compiler version 2.2.0-RC used in the test
                            // and current master (KtDiagnosticsContainer was introduced in 2.2.0-Beta1 timeframe)
                            // Error disappears if we comment the last line in FirAssignmentPluginExtensionRegistrar
                            // With LV 1.9, this class isn't actual at all.
                            // This is a very specific case that is caused by usage of KGP 2.3 but with Kotlin compiler 2.2 through BTA.
                            // The problem is that compiler plugins are not yet aligned with the version of compiler configured for BTA,
                            // so it's compiler 2.2 + assignment compiler plugin 2.3. See KT-68107.
                            languageVersion.set(KotlinVersion.KOTLIN_1_9)
                            apiVersion.set(KotlinVersion.KOTLIN_1_9)
                        }
                    }
                }
            }
        }
    }

    private fun TestProject.checkJvmDefaultReplacement(
        expectedJvmDefaultStable: String?,
        expectedJvmDefaultDeprecated: String?,
        buildOutputAssertions: BuildResult.() -> Unit = {}
    ) {
        val jvmArgs = providerBuildScriptReturn {
            kotlinJvm.target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compileTaskProvider.map {
                (it as KotlinCompile).createCompilerArguments(KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.default)
            }
        }.buildAndReturn(":compileKotlin", buildAction = BuildActions.buildWithAssertions(buildOutputAssertions))

        assertEquals(expectedJvmDefaultStable, jvmArgs.jvmDefaultStable)
        @Suppress("DEPRECATION")
        assertEquals(expectedJvmDefaultDeprecated, jvmArgs.jvmDefault)
    }
}
