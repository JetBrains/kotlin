/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Cross-module type alias KMP incremental compilation")
class CrossModuleTypeAliasIncrementalIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(languageVersion = "2.0")

    @GradleTest
    @DisplayName("A change in the expanded class recompiles alias consumers (separate KMP compilation disabled)")
    fun testWithoutSeparateCompilation(gradleVersion: GradleVersion) {
        // Without separate compilation `app` compiles against lib's .class files, so the change is visible without expansion tracking.
        doTestExpandedClassChangeRecompilesAliasConsumer(
            gradleVersion, separateCompilation = false, expandTypeAliases = false, expectedOutput = "output: Int"
        )
    }

    @GradleTest
    @DisplayName("A change in the expanded class does not recompile alias consumers without type alias expansion tracking (separate KMP compilation enabled)")
    fun testSeparateCompilationWithoutTypeAliasExpansion(gradleVersion: GradleVersion) {
        // Documents the stale result that expansion tracking exists to prevent: `app` keeps calling `foo(Any)`.
        doTestExpandedClassChangeRecompilesAliasConsumer(
            gradleVersion, separateCompilation = true, expandTypeAliases = false, expectedOutput = "output: Any"
        )
    }

    @GradleTest
    @DisplayName("A change in the expanded class recompiles alias consumers with type alias expansion tracking (separate KMP compilation enabled)")
    fun testWithSeparateCompilation(gradleVersion: GradleVersion) {
        doTestExpandedClassChangeRecompilesAliasConsumer(
            gradleVersion, separateCompilation = true, expandTypeAliases = true, expectedOutput = "output: Int"
        )
    }

    private fun doTestExpandedClassChangeRecompilesAliasConsumer(
        gradleVersion: GradleVersion,
        separateCompilation: Boolean,
        expandTypeAliases: Boolean,
        expectedOutput: String,
    ) {
        val buildOptions = defaultBuildOptions
            .copy(separateCompilation = separateCompilation, expandTypeAliasesInClasspathSnapshots = expandTypeAliases)
            .disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        project("empty", gradleVersion, buildOptions = buildOptions) {
            addKgpToBuildScriptCompilationClasspath()

            val lib = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        js()
                    }
                }
                kotlinSourcesDir("commonMain").source("com/example/A.kt") {
                    """
                    expect class A() {
                        fun foo(a: Any): String
                    }
                    """.trimIndent()
                }
                kotlinSourcesDir("jvmMain").source("com/example/B.kt") {
                    """
                    class B {
                        fun foo(a: Any): String = "Any"
                    }

                    actual typealias A = B
                    """.trimIndent()
                }
                // A second (non-JVM) target is required so that lib publishes a commonMain metadata klib
                kotlinSourcesDir("jsMain").source("com/example/A.js.kt") {
                    """
                    actual class A actual constructor() {
                        actual fun foo(a: Any): String = ""
                    }
                    """.trimIndent()
                }
            }

            val app = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        js()

                        sourceSets.commonMain {
                            dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    }
                }
                kotlinSourcesDir("commonMain").source("bar.kt") {
                    """
                    fun bar() = A().foo(42)
                    """.trimIndent()
                }
                kotlinSourcesDir("jvmMain").source("Main.kt") {
                    """
                    fun main() {
                        println("output: " + bar())
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
            libInRoot.kotlinSourcesDir("jsMain").resolve("com/example/A.js.kt").modify { content ->
                content.replace(
                    "    actual fun foo(a: Any): String = \"\"",
                    "    actual fun foo(a: Any): String = \"\"\n    actual fun foo(a: Int): String = \"\"",
                )
            }

            build(":app:jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":lib:compileKotlinJvm", ":app:compileKotlinJvm")
                assertOutputContains(expectedOutput)
            }
        }
    }
}
