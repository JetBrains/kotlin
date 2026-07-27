/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Cross-module type alias incremental compilation")
class CrossModuleTypeAliasIncrementalIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            logLevel = LogLevel.DEBUG,
            languageVersion = "2.0",
        )

    @GradleTest
    @DisplayName("A change in the expanded class recompiles alias consumers (default - separate KMP compilation disabled)")
    fun testWithoutSeparateCompilation(gradleVersion: GradleVersion) {
        doTestExpandedClassChangeRecompilesAliasConsumer(gradleVersion, separateCompilation = false)
    }

    @GradleTest
    @DisplayName("A change in the expanded class recompiles alias consumers (separate KMP compilation enabled)")
    fun testWithSeparateCompilation(gradleVersion: GradleVersion) {
        doTestExpandedClassChangeRecompilesAliasConsumer(gradleVersion, separateCompilation = true)
    }

    private fun doTestExpandedClassChangeRecompilesAliasConsumer(gradleVersion: GradleVersion, separateCompilation: Boolean) {
        val buildOptions = defaultBuildOptions.copy(separateCompilation = true)
        project("empty", gradleVersion, buildOptions = buildOptions) {
            addKgpToBuildScriptCompilationClasspath()

            val lib = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                    }
                }
                kotlinSourcesDir("commonMain").source("com/example/A.kt") {
                    """
                    package com.example

                    expect class A() {
                        fun foo(a: Any): String
                    }
                    """.trimIndent()
                }
                kotlinSourcesDir("jvmMain").source("com/example/B.kt") {
                    """
                    package com.example

                    class B {
                        fun foo(a: Any): String = "Any"
                    }

                    actual typealias A = B
                    """.trimIndent()
                }
            }

            val app = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()

                        sourceSets.jvmMain {
                            dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    }
                }
                kotlinSourcesDir("jvmMain").source("Main.kt") {
                    """
                    import com.example.A

                    fun main() {
                        println("output: " + A().foo(42))
                    }
                    """.trimIndent()
                }
            }

            include(lib, "lib", useSymlink = false)
            include(app, "app", useSymlink = false)

            build(":app:jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":lib:compileKotlinJvm", ":app:compileKotlinJvm")
                assertOutputContains("output: Any")
            }

            val libInRoot = subProject("lib")
            libInRoot.kotlinSourcesDir("commonMain").resolve("com/example/A.kt").modify { content ->
                content.replace(
                    "    fun foo(a: Any): String",
                    "    fun foo(a: Any): String\n    fun foo(a: Int): String",
                )
            }
            libInRoot.kotlinSourcesDir("jvmMain").resolve("com/example/B.kt").modify { content ->
                content.replace(
                    "    fun foo(a: Any): String = \"Any\"",
                    "    fun foo(a: Any): String = \"Any\"\n    fun foo(a: Int): String = \"Int\"",
                )
            }

            build(":app:jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":lib:compileKotlinJvm", ":app:compileKotlinJvm")
                assertOutputContains("output: Int")
            }
        }
    }
}
