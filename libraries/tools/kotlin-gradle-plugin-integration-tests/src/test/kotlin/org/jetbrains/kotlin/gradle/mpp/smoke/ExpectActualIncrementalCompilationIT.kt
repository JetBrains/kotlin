/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

/**
 *
 * Touch file with expect fun, target IC adds actual to the build set
 * Variants: expect class
 * Touch file with actual fun, target IC adds expect to the build set
 * Variants: expect class
 *
 */
@DisplayName("Incremental scenarios with expect/actual - K2")
@MppGradlePluginTests
open class ExpectActualIncrementalCompilationIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK2()

    @DisplayName("File with actual declaration needs recompiling")
    @GradleTest
    fun testRecompilationOfActualFun(gradleVersion: GradleVersion) {
        nativeProject("expect-actual-fun-or-class-ic", gradleVersion) {
            build("assemble")

            listOf(
                kotlinSourcesDir("jvmMain").resolve("ActualFunFoo.kt"),
                kotlinSourcesDir("nativeMain").resolve("ActualFunFoo.kt"),
                kotlinSourcesDir("jsMain").resolve("ActualFunFoo.kt")
            ).forEach {
                it.replaceFirst("\"foo", "\"bar")
            }

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(":compileKotlinJvm", ":compileKotlinJs", ":compileKotlinNative")
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("jvmMain").resolve("ActualFunFoo.kt"),
                        kotlinSourcesDir("jsMain").resolve("ActualFunFoo.kt"),
                        kotlinSourcesDir("commonMain").resolve("ExpectFunFoo.kt")
                    ).relativizeTo(projectPath)
                )
            }
        }
    }

    @DisplayName("File with expect declaration needs recompiling indirectly")
    @GradleTest
    fun testRecompilationOfExpectFun(gradleVersion: GradleVersion) {
        nativeProject("expect-actual-fun-or-class-ic", gradleVersion) {
            build("assemble")

            val unusedKtPath = kotlinSourcesDir("commonMain").resolve("Unused.kt")
            unusedKtPath.replaceFirst(
                "val secret = 1",
                "val secret = \"k2\""
            )

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(":compileKotlinJvm", ":compileKotlinJs", ":compileKotlinNative")
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("jvmMain").resolve("ActualFunFoo.kt"),
                        kotlinSourcesDir("jsMain").resolve("ActualFunFoo.kt"),
                        kotlinSourcesDir("commonMain").resolve("ExpectFunFoo.kt"),
                        unusedKtPath
                    ).relativizeTo(projectPath)
                )
            }
        }
    }

    @DisplayName("File with expect class declaration needs recompiling")
    @GradleTest
    fun testRecompilationOfExpectClass(gradleVersion: GradleVersion) {
        nativeProject("expect-actual-fun-or-class-ic", gradleVersion) {
            build("assemble")

            kotlinSourcesDir("commonMain").resolve("ExpectClassBar.kt").appendText("val irrelevant = 2")

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(":compileKotlinJvm", ":compileKotlinJs", ":compileKotlinNative")
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("jvmMain").resolve("ActualClassBar.kt"),
                        kotlinSourcesDir("jsMain").resolve("ActualClassBar.kt"),
                        kotlinSourcesDir("commonMain").resolve("ExpectClassBar.kt")
                    ).relativizeTo(projectPath)
                )
            }
        }
    }
}

@DisplayName("Incremental scenarios with expect/actual - K1")
class ExpectActualIncrementalCompilationK1IT : ExpectActualIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}
