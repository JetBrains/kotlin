/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteExisting
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Incremental compilation of common KMP sources must resolve them against their own fragment, like a non-incremental build does.
 *
 * The platform declarations in the test sources deliberately differ in return type from the common ones they would shadow,
 * so resolving a common call against the platform view of the module fails the compilation.
 */
@MustRunOnChangesInGradle
@MustRunOnChangesInBuildToolsApi
abstract class ClasspathMetadataIncrementalITBase(
    val platformType: KotlinPlatformType,
    val taskToExecute: String,
    val setupBuildScript: GradleProjectBuildScriptInjectionContext.() -> Unit,
) : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(logLevel = LogLevel.DEBUG).let {
            when (platformType) {
                KotlinPlatformType.js -> it.copy(enableJsUnsafeIncrementalCompilationForMultiplatform = true)
                    .disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
                KotlinPlatformType.wasm -> it.copy(enableWasmUnsafeIncrementalCompilationForMultiplatform = true)
                    .disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
                else -> it.copy(enableJvmIncrementalCompilationOfCommonSources = true, jvmClasspathMetadata = true)
            }
        }

    protected val platformSourceSet = when (platformType) {
        KotlinPlatformType.wasm -> "wasmJsMain"
        else -> "${platformType.name}Main"
    }

    @GradleTest
    @DisplayName("A recompiled common source resolves against common declarations only")
    fun testCommonSourceResolvesAgainstCommonDeclarations(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            setupCommonOverloadResolutionProject()

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("bar.kt"),
                            kotlinSourcesDir("commonMain").resolve("foo.kt"),
                            kotlinSourcesDir(platformSourceSet).resolve("platform.kt"),
                        )
                    ), output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("foo.kt").modify { content ->
                content.replace("fun foo(): String = bar(42)", "fun foo(): String = bar(41)")
            }

            // `foo()` must keep resolving `bar(41)` to `commonMain`'s `bar(Any): String`, not the platform `bar(Int): Int`
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("foo.kt"))), output = output
                )
            }
        }
    }

    protected fun TestProject.setupCommonOverloadResolutionProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection(setupBuildScript)
        kotlinSourcesDir("commonMain").apply {
            source("bar.kt") {
                """
                fun bar(a: Any): String = "Any"
                """.trimIndent()
            }

            source("foo.kt") {
                """
                fun foo(): String = bar(42)
                """.trimIndent()
            }
        }

        kotlinSourcesDir(platformSourceSet).source("platform.kt") {
            """
            fun bar(i: Int): Int = i
            """.trimIndent()
        }
    }

    @GradleTest
    @DisplayName("A recompiled intermediate source resolves against its own and common declarations only")
    fun testIntermediateSourceResolvesAgainstCommonDeclarations(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            setupTwoCommonModulesProject()

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("bar.kt"),
                            kotlinSourcesDir("intermediateMain").resolve("foo.kt"),
                            kotlinSourcesDir(platformSourceSet).resolve("platform.kt"),
                        )
                    ), output = output
                )
            }

            kotlinSourcesDir("intermediateMain").resolve("foo.kt").modify { content ->
                content.replace("fun foo(): String = bar(42)", "fun foo(): String = bar(41)")
            }

            // `foo()` lives in the intermediate fragment; it must keep resolving `bar(41)` to `commonMain`'s `bar(Any): String`,
            // not the leaf platform `bar(Int): Int`
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("intermediateMain").resolve("foo.kt"))), output = output
                )
            }
        }
    }

    private fun TestProject.setupTwoCommonModulesProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection(setupBuildScript)
        val platformSourceSet = platformSourceSet
        buildScriptInjection {
            kotlinMultiplatform.sourceSets.apply {
                val intermediateMain = create("intermediateMain")
                intermediateMain.dependsOn(getByName("commonMain"))
                getByName(platformSourceSet).dependsOn(intermediateMain)
            }
        }
        kotlinSourcesDir("commonMain").source("bar.kt") {
            """
            fun bar(a: Any): String = "Any"
            """.trimIndent()
        }

        kotlinSourcesDir("intermediateMain").source("foo.kt") {
            """
            fun foo(): String = bar(42)
            """.trimIndent()
        }

        kotlinSourcesDir(platformSourceSet).source("platform.kt") {
            """
            fun bar(i: Int): Int = i
            """.trimIndent()
        }
    }

    @GradleTest
    @DisplayName("KT-89044: adding an overload to an expect class with an actual typealias recompiles common call sites")
    fun testExpectClassOverloadAddedViaActualTypealias(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            setupExpectTypealiasOverloadProject()

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            kotlinSourcesDir("commonMain").resolve("base.kt").modify { content ->
                content.replace("fun foo(x: Any): String", "fun foo(x: Any): String\n    fun foo(x: Int): String")
            }
            kotlinSourcesDir(platformSourceSet).resolve("b1.kt").modify { content ->
                content.replace(
                    "fun foo(x: Any): String = \"Any\"",
                    "fun foo(x: Any): String = \"Any\"\n    fun foo(x: Int): String = \"Int\""
                )
            }

            // `result.kt` resolved `A1().foo(42)` against the expect class, so only a diff of the cached common metadata
            // for `base.kt` makes it recompile
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(
                        listOf(
                            kotlinSourcesDir("commonMain").resolve("base.kt"),
                            kotlinSourcesDir("commonMain").resolve("result.kt"),
                            kotlinSourcesDir(platformSourceSet).resolve("actual.kt"),
                            kotlinSourcesDir(platformSourceSet).resolve("b1.kt"),
                        )
                    ), output = output
                )
            }
        }
    }

    private fun TestProject.setupExpectTypealiasOverloadProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection(setupBuildScript)
        kotlinSourcesDir("commonMain").apply {
            source("base.kt") {
                """
                expect class A1() {
                    fun foo(x: Any): String
                }
                """.trimIndent()
            }
            source("result.kt") {
                """
                fun result1(): String = A1().foo(42)
                """.trimIndent()
            }
        }

        kotlinSourcesDir(platformSourceSet).apply {
            source("b1.kt") {
                """
                class B1 {
                    fun foo(x: Any): String = "Any"
                }
                """.trimIndent()
            }
            source("actual.kt") {
                """
                actual typealias A1 = B1
                """.trimIndent()
            }
        }
    }

    @GradleTest
    @DisplayName("Incremental compilation preserves packages of files not recompiled")
    fun testIncrementalCompilationPreservesUntouchedPackages(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            setupMultiPackageProject()

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
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

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt"))),
                    output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt").modify { content ->
                content.replace("fun foo() = bar(41)", "fun foo() = bar(40)")
            }

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("com/example/two/foo.kt"))),
                    output = output
                )
            }
        }
    }

    @GradleTest
    @DisplayName("A removed package does not break incremental compilation")
    fun testRemovedPackageDoesNotBreakIncrementalCompilation(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            setupMultiPackageProject()

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
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

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = emptyList(),
                    output = output
                )
            }

            kotlinSourcesDir("commonMain").resolve("com/example/one/bar.kt").modify { content ->
                content.replace("fun bar(a: Any) = \"Any\"", "fun bar(a: Any) = \"Any!\"")
            }

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertCompiledKotlinSources(
                    expectedSources = relativeToProject(listOf(kotlinSourcesDir("commonMain").resolve("com/example/one/bar.kt"))),
                    output = output
                )
            }
        }
    }

    private fun TestProject.setupMultiPackageProject() {
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection(setupBuildScript)
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

@MppGradlePluginTests
@DisplayName("Incremental compilation of common sources within a KMP module - Jvm")
class ClasspathMetadataIncrementalJvmIT : ClasspathMetadataIncrementalITBase(
    platformType = KotlinPlatformType.jvm,
    taskToExecute = ":compileKotlinJvm",
    setupBuildScript = {
        project.applyMultiplatform {
            jvm()
        }
    }
) {
    @GradleTest
    @DisplayName("Regression control: legacy incremental compilation resolves a recompiled common source against platform declarations")
    fun testLegacyIncrementalCompilationResolvesAgainstPlatformDeclarations(gradleVersion: GradleVersion) {
        // the legacy mode only exists on JVM
        project("empty", gradleVersion, buildOptions = defaultBuildOptions.copy(jvmClasspathMetadata = false)) {
            setupCommonOverloadResolutionProject()

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            kotlinSourcesDir("commonMain").resolve("foo.kt").modify { content ->
                content.replace("fun foo(): String = bar(42)", "fun foo(): String = bar(41)")
            }

            // The recompiled `foo.kt` incorrectly sees the platform `bar(i: Int): Int` through artifacts left in build/classes/,
            // so `foo(): String` no longer compiles. This proves the test sources detect the bug the other tests guard against.
            buildAndFail(taskToExecute) {
                assertTasksFailed(taskToExecute)
                assertEquals(
                    listOf("RETURN_TYPE_MISMATCH" to "Return type mismatch: expected 'String', actual 'Int'."),
                    extractProblemsReportDiagnostics(gradleVersion),
                )
            }
        }
    }

    /**
     * Extracts the diagnostics of the Problems API report as pairs of the diagnostic id and the message.
     *
     * Compiler errors can't be taken from the build output: since Gradle 9, KGP reports them only through the Problems API.
     */
    private fun BuildResult.extractProblemsReportDiagnostics(gradleVersion: GradleVersion): List<Pair<String, String>> {
        val reportUrl =
            assertNotNull(ProblemsApiTestUtils.extractProblemReportUrl(output), "Problems API report URL not found in build output")
        val report =
            ProblemsApiTestUtils.parseProblemReportFromScript(ProblemsApiTestUtils.readProblemReportContent(reportUrl), gradleVersion)
        return report.diagnostics.map { it.problemId.last().name to it.problemDetailsActual }
    }
}

@MppGradlePluginTests
@DisplayName("Incremental compilation of common sources within a KMP module - Js")
class ClasspathMetadataIncrementalJsIT : ClasspathMetadataIncrementalITBase(
    platformType = KotlinPlatformType.js,
    taskToExecute = ":compileKotlinJs",
    setupBuildScript = {
        project.applyMultiplatform {
            js()
        }
    }
)

@OptIn(ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Incremental compilation of common sources within a KMP module - Wasm")
class ClasspathMetadataIncrementalWasmIT : ClasspathMetadataIncrementalITBase(
    platformType = KotlinPlatformType.wasm,
    taskToExecute = ":compileKotlinWasmJs",
    setupBuildScript = {
        project.applyMultiplatform {
            wasmJs()
        }
    }
)
