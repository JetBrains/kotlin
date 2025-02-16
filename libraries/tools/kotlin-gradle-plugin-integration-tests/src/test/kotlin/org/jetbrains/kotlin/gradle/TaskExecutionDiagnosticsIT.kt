/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Execution time diagnostics")
class TaskExecutionDiagnosticsIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @GradleTest
    fun shouldProduceErrorOnFirIcRunnerAndLv19(
        gradleVersion: GradleVersion
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

    @JvmGradlePluginTests
    @GradleTest
    fun shouldProduceErrorOnFirIcRunnerAndDisabledClasspathSnapshots(
        gradleVersion: GradleVersion,
    ) {
        val project = project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(useGradleClasspathSnapshot = false)
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyJvm {
                    jvmToolchain(17)
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
                |kotlin.internal.suppressGradlePluginErrors=DeprecatedJvmHistoryBasedIncrementalCompilationDiagnostic
                """.trimMargin()
            )
        }

        project.buildAndFail("compileKotlin") {
            assertHasDiagnostic(KotlinToolingDiagnostics.IcFirMisconfigurationRequireClasspathSnapshots)
        }
    }
}