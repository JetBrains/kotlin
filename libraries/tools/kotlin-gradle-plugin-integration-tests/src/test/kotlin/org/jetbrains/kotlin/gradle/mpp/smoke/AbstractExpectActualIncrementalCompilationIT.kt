/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceWithVersion
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

/**
 * Touch file with expect fun, target IC adds actual to the build set
 * Variants: expect class
 * Touch file with actual fun, target IC adds expect to the build set
 * Variants: expect class
 */
@MppGradlePluginTests
abstract class AbstractExpectActualIncrementalCompilationIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK2().copy(
            // disable IC-breaking feature; it's tested separately in [org.jetbrains.kotlin.gradle.mpp.CommonCodeWithPlatformSymbolsITBase]
            enableUnsafeIncrementalCompilationForMultiplatform = true,
            logLevel = LogLevel.DEBUG,
        )

    @DisplayName("File with actual declaration needs recompiling")
    @GradleTest
    @TestMetadata("expect-actual-fun-or-class-ic")
    fun testRecompilationOfActualFun(gradleVersion: GradleVersion) {
        nativeProject(
            "expect-actual-fun-or-class-ic",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            build("assemble")

            listOf("jvmMain", "jsMain", "nativeMain").forEach { sourceSet ->
                // just touch the file. expected logic is this:
                // actual declaration needs to be recompiled -> we add expect declaration to the list of compiled files
                kotlinSourcesDir(sourceSet).resolve("ActualFunFoo.kt").addPrivateVal()
            }

            build("assemble") {
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
    @TestMetadata("expect-actual-fun-or-class-ic")
    fun testRecompilationOfExpectFun(gradleVersion: GradleVersion) {
        nativeProject(
            "expect-actual-fun-or-class-ic",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            build("assemble")

            val commonSourceKt = kotlinSourcesDir("commonMain").resolve("UsedInFileWithExpectFun.kt")
            commonSourceKt.replaceWithVersion("sourceCompatibleAbiChange")

            build("assemble") {
                assertTasksExecuted(":compileKotlinJvm", ":compileKotlinJs", ":compileKotlinNative")
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("jvmMain").resolve("ActualFunFoo.kt"),
                        kotlinSourcesDir("jsMain").resolve("ActualFunFoo.kt"),
                        kotlinSourcesDir("commonMain").resolve("ExpectFunFoo.kt"),
                        commonSourceKt
                    ).relativizeTo(projectPath)
                )
            }
        }
    }

    @DisplayName("File with expect class declaration needs recompiling")
    @GradleTest
    @TestMetadata("expect-actual-fun-or-class-ic")
    fun testRecompilationOfExpectClass(gradleVersion: GradleVersion) {
        nativeProject(
            "expect-actual-fun-or-class-ic",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            build("assemble")

            kotlinSourcesDir("commonMain").resolve("ExpectClassBar.kt").addPrivateVal()

            build("assemble") {
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

@DisplayName("Incremental scenarios with expect/actual - K2")
class ExpectActualIncrementalCompilationK2IT : AbstractExpectActualIncrementalCompilationIT() {

    @DisplayName("Incremental compilation with lenient mode")
    @GradleTest
    fun testLenientModeIncrementalCompilation(gradleVersion: GradleVersion) {
        project("lenientMode", gradleVersion) {
            build("compileKotlinJvm")

            kotlinSourcesDir("jvmMain").resolve("jvm.kt").writeText(
                """
                actual fun foo() {}
            """.trimIndent()
            )

            build("compileKotlinJvm") {
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("commonMain").resolve("common.kt"),
                        kotlinSourcesDir("jvmMain").resolve("jvm.kt")
                    ).relativizeTo(projectPath)
                )
            }

            kotlinSourcesDir("jvmMain").resolve("jvm2.kt").writeText(
                """
                fun bar() {}
            """.trimIndent()
            )

            build("compileKotlinJvm") {
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("jvmMain").resolve("jvm2.kt")
                    ).relativizeTo(projectPath)
                )
            }
        }
    }

    @DisplayName("Incremental compilation with lenient mode 2")
    @GradleTest
    fun testLenientModeIncrementalCompilation2(gradleVersion: GradleVersion) {
        project("lenientMode", gradleVersion) {
            kotlinSourcesDir("commonMain").resolve("common.kt").writeText(
                """
                expect fun foo()
                expect fun bar()
            """.trimIndent()
            )

            kotlinSourcesDir("jvmMain").resolve("jvm.kt").writeText(
                """
                actual fun foo() {}
            """.trimIndent()
            )

            build("compileKotlinJvm")

            kotlinSourcesDir("commonMain").resolve("common2.kt").writeText(
                """
                fun baz() {}
            """.trimIndent()
            )

            build("compileKotlinJvm") {
                assertIncrementalCompilation(
                    listOf(
                        kotlinSourcesDir("commonMain").resolve("common.kt"),
                        kotlinSourcesDir("commonMain").resolve("common2.kt"),
                        kotlinSourcesDir("jvmMain").resolve("jvm.kt"),
                    ).relativizeTo(projectPath)
                )
            }
        }
    }
}
