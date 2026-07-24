/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.deleteExisting

@MppGradlePluginTests
@DisplayName("JVM classpath metadata incremental compilation")
class JvmClasspathMetadataIncrementalIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            logLevel = LogLevel.DEBUG,
            languageVersion = "2.0",
            enableUnsafeIncrementalCompilationForMultiplatform = true,
        )

    @GradleTest
    @DisplayName("Verify that incremental compilation without JVM classpath metadata leads to incorrect resolution")
    fun testWithJvmClasspathMetadataDisabled(gradleVersion: GradleVersion) {
        project(
            projectName = "empty", gradleVersion = gradleVersion, buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = false)
        ) {
            setupJvmClasspathMetadataProject()

            build("jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("bar.kt"),
                            kotlinSourcesDir("commonMain").resolve("foo.kt"),
                            kotlinSourcesDir("jvmMain").resolve("main.kt"),
                        )
                    ), output = output
                )
                assertOutputContains("KMP output: Any")
            }

            kotlinSourcesDir("commonMain").resolve("foo.kt").modify { content ->
                content.replace("fun foo() = bar(42)", "fun foo() = bar(41)")
            }

            /**
             * Without kotlin.internal.kmp.jvmClasspathMetadata, the compiler produces incorrect output (Int instead of the expected Any).
             * During incremental builds, commonMain incorrectly gains access to declarations from jvmMain through artifacts left in the build/classes/ directory.
             */
            build("jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("foo.kt"))), output = output
                )
                assertOutputContains("KMP output: Int") // wrong result
            }
        }
    }

    @GradleTest
    @DisplayName("Verify that incremental compilation with JVM classpath metadata leads to correct resolution")
    fun testWithJvmClasspathMetadataEnabled(gradleVersion: GradleVersion) {
        project(
            projectName = "empty", gradleVersion = gradleVersion, buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = true)
        ) {
            setupJvmClasspathMetadataProject()

            build("jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("bar.kt"),
                            kotlinSourcesDir("commonMain").resolve("foo.kt"),
                            kotlinSourcesDir("jvmMain").resolve("main.kt"),
                        )
                    ), output = output
                )
                assertOutputContains("KMP output: Any")
            }

            kotlinSourcesDir("commonMain").resolve("foo.kt").modify { content ->
                content.replace("fun foo() = bar(42)", "fun foo() = bar(41)")
            }

            build("jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("foo.kt"))), output = output
                )
                assertOutputContains("KMP output: Any")
            }
        }
    }

    private fun TestProject.setupJvmClasspathMetadataProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection {
            project.applyMultiplatform {
                jvm()
            }
        }
        kotlinSourcesDir("commonMain").apply {
            source("bar.kt") {
                """
                fun bar(a: Any) = "Any"
                """.trimIndent()
            }

            source("foo.kt") {
                """
                fun foo() = bar(42)
                """.trimIndent()
            }
        }

        kotlinSourcesDir("jvmMain").source("main.kt") {
            $$"""
            fun bar(i: Int) = "Int"

            fun main() {
                println("KMP output: ${foo()}")
            }
            """.trimIndent()
        }
    }

    @GradleTest
    @DisplayName("Verify incremental compilation with JVM classpath metadata resolves correctly across two common modules")
    fun testTwoCommonModulesWithJvmClasspathMetadata(gradleVersion: GradleVersion) {
        project(
            projectName = "empty", gradleVersion = gradleVersion, buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = true)
        ) {
            setupTwoCommonModulesProject()

            build("jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("bar.kt"),
                            kotlinSourcesDir("intermediateMain").resolve("foo.kt"),
                            kotlinSourcesDir("jvmMain").resolve("main.kt"),
                        )
                    ), output = output
                )
                assertOutputContains("KMP output: Any")
            }

            kotlinSourcesDir("intermediateMain").resolve("foo.kt").modify { content ->
                content.replace("fun foo() = bar(42)", "fun foo() = bar(41)")
            }

            build("jvmRun", "-DmainClass=MainKt") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("intermediateMain").resolve("foo.kt"))), output = output
                )
                assertOutputContains("KMP output: Any")
            }
        }
    }

    private fun TestProject.setupTwoCommonModulesProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection {
            project.applyMultiplatform {
                jvm()
                sourceSets {
                    val intermediateMain = it.create("intermediateMain")
                    intermediateMain.dependsOn(it.commonMain.get())
                    it.jvmMain {
                        dependsOn(intermediateMain)
                    }
                }
            }
        }
        kotlinSourcesDir("commonMain").source("bar.kt") {
            """
            fun bar(a: Any) = "Any"
            """.trimIndent()
        }

        kotlinSourcesDir("intermediateMain").source("foo.kt") {
            """
            fun foo() = bar(42)
            """.trimIndent()
        }

        kotlinSourcesDir("jvmMain").source("main.kt") {
            $$"""
            fun bar(i: Int) = "Int"

            fun main() {
                println("KMP output: ${foo()}")
            }
            """.trimIndent()
        }
    }

    @GradleTest
    @DisplayName("Verify incremental compilation preserves packages of files not recompiled")
    fun testIncrementalCompilationPreservesUntouchedPackages(gradleVersion: GradleVersion) {
        project(
            projectName = "empty", gradleVersion = gradleVersion, buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = true)
        ) {
            setupMultiPackageProject()

            build("compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("com/example/one/bar.kt"),
                            kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt")
                        )
                    ), output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt").modify { content ->
                content.replace("fun foo() = bar(42)", "fun foo() = bar(41)")
            }

            build("compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt"))),
                    output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt").modify { content ->
                content.replace("fun foo() = bar(41)", "fun foo() = bar(40)")
            }

            build("compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt"))),
                    output = output
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Verify removed package does not break incremental compilation")
    fun testRemovedPackageDoesNotBreakIncrementalCompilation(gradleVersion: GradleVersion) {
        project(
            projectName = "empty", gradleVersion = gradleVersion, buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = true)
        ) {
            setupMultiPackageProject()

            build("compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("com/example/one/bar.kt"),
                            kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt")
                        )
                    ), output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt").deleteExisting()

            build("compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = emptyList(),
                    output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("com/example/one/bar.kt").modify { content ->
                content.replace("fun bar(a: Any) = \"Any\"", "fun bar(a: Any) = \"Any!\"")
            }

            build("compileKotlinJvm") {
                assertTasksExecuted(":compileKotlinJvm")
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("com/example/one/bar.kt"))),
                    output = output
                )
            }
        }
    }

    private fun TestProject.setupMultiPackageProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection {
            project.applyMultiplatform {
                jvm()
            }
        }
        kotlinSourcesDir("commonMain").apply {
            source("com/example/one/bar.kt") {
                """
                    package com.example.one
                    fun bar(a: Any) = "Any"
                    """.trimIndent()
            }

            source("com/example/two/foo.kt") {
                """
                    package com.example.two
                    import com.example.one.bar
                    fun foo() = bar(42)
                    """.trimIndent()
            }
        }
    }
}
