/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.internal.crash.afterEvaluate
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.compareTo
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Execution time diagnostics")
class TaskExecutionDiagnosticsIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @GradleTest
    fun shouldProduceErrorOnFirIcRunnerAndLv19(
        gradleVersion: GradleVersion,
    ) {
        val project = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyJvm {
                    jvmToolchain(17)
                    compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_9)
                }
            }

            kotlinSourcesDir().source("main.kt") {
                """
                |fun main() {}
                """.trimMargin()
            }

            gradleProperties.appendText(
                """
                |kotlin.incremental.jvm.fir=true
                """.trimMargin()
            )
        }

        project.buildAndFail("compileKotlin") {
            assertHasDiagnostic(KotlinToolingDiagnostics.IcFirMisconfigurationLV)
        }
    }

    @DisplayName("KT-79851: unsupported version, but no kotlin-dsl: should be no new diagnostic")
    @JvmGradlePluginTests
    @GradleTest
    fun noKt79851DiagnosticWithoutKotlinDsl(gradleVersion: GradleVersion) {
        val project = project("emptyKts", gradleVersion) {
            plugins {
                kotlin("jvm")
            }
            buildScriptInjection {
                project.applyJvm {
                    jvmToolchain(17)
                    compilerOptions.apiVersion.set(KotlinVersion.KOTLIN_1_8)
                    compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_8)
                }
            }

            kotlinSourcesDir().source("main.kt") {
                """
                |fun main() {}
                """.trimMargin()
            }
        }

        project.buildAndFail("compileKotlin") {
            assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinVersionKotlinDsl)
        }
    }

    @DisplayName("KT-79851: emit unsupported language version kotlin-dsl diagnostic, default compiler")
    @JvmGradlePluginTests
    @GradleTest
    fun emitDiagnosticOnUnsupportedVersionAlongKotlinDsl(gradleVersion: GradleVersion) =
        emitDiagnosticOnUnsupportedVersionAlongKotlinDsl(gradleVersion, btaVersion = null)

    @DisplayName("KT-79851: emit unsupported language version kotlin-dsl diagnostic, custom compiler via BTA with deprecation")
    @JvmGradlePluginTests
    @GradleTest
    fun emitDiagnosticOnUnsupportedVersionAlongKotlinDslCustomVersionDeprecation(gradleVersion: GradleVersion) =
        emitDiagnosticOnUnsupportedVersionAlongKotlinDsl(gradleVersion, btaVersion = "2.2.10")

    @DisplayName("KT-79851: emit unsupported language version kotlin-dsl diagnostic, custom compiler via BTA without deprecation")
    @JvmGradlePluginTests
    @GradleTest
    fun emitDiagnosticOnUnsupportedVersionAlongKotlinDslCustomVersion(gradleVersion: GradleVersion) =
        emitDiagnosticOnUnsupportedVersionAlongKotlinDsl(gradleVersion, btaVersion = "2.1.20")

    private fun emitDiagnosticOnUnsupportedVersionAlongKotlinDsl(
        gradleVersion: GradleVersion,
        btaVersion: String? = null,
    ) {
        val project =
            project("emptyKts", gradleVersion, buildOptions = defaultBuildOptions.copy(runViaBuildToolsApi = btaVersion != null)) {
                plugins {
                    kotlin("jvm")
                    id("kotlin-dsl")
                }
                // before Gradle 8.1, kotlin-dsl plugin configured the versions in afterEvaluate, forcing usage of afterEvaluate to override its configuration
                val haveToUseAfterEvaluate = gradleVersion < GradleVersion.version("8.1")
                buildScriptInjection {
                    project.applyJvm {
                        jvmToolchain(17)
                        @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
                        if (btaVersion != null) {
                            compilerVersion.set(btaVersion)
                        }
                    }
                    // to make the test more reliable, fixate AV/LV. Those particular values are defaults for Gradle 8
                    val configureKotlin = {
                        project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                            it.compilerOptions.apiVersion.set(KotlinVersion.KOTLIN_1_8)
                            it.compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_8)
                        }
                    }
                    if (haveToUseAfterEvaluate) {
                        project.afterEvaluate { configureKotlin() }
                    } else {
                        configureKotlin()
                    }

                }

                kotlinSourcesDir().source("main.kt") {
                    """
                |fun main() {}
                """.trimMargin()
                }
            }

        val btaToolingVersion = btaVersion?.let { KotlinToolingVersion(it) }
        val canCompileWithoutWarningsViaBta = when {
            btaToolingVersion == null -> false
            btaToolingVersion < "2.2.0" -> true
            else -> false
        }
        val canCompileWithoutErrorsViaBta = when {
            btaToolingVersion == null -> false
            btaToolingVersion < "2.3.0" -> true
            else -> false
        }
        val expectFail = when {
            canCompileWithoutErrorsViaBta -> false
            else -> true
        }
        val assertions: BuildResult.() -> Unit = {
            if (canCompileWithoutWarningsViaBta) {
                assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinVersionKotlinDsl)
            } else {
                val expectedVersions = """
                    - API version: 1.8
                    - language version: 1.8
                """.trimIndent()
                assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedKotlinVersionKotlinDsl, expectedVersions)
            }
        }
        if (expectFail) {
            project.buildAndFail("compileKotlin", assertions = assertions)
        } else {
            project.build("compileKotlin", assertions = assertions)
        }
    }
}