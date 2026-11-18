/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_METADATA_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_WASM_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.test.Test
import kotlin.test.assertFails

/**
 * Tasks that run without the Build Tools API compile with the Kotlin Gradle Plugin version of the compiler,
 * so a different `kotlin.compilerVersion` must be reported instead of being ignored.
 */
class CompilerVersionWithoutBuildToolsApiTest {

    @Test
    fun testJsCompilationWithoutBuildToolsApiReportsCompilerVersion() {
        val project = buildProject(disabledProperty = KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API)

        project.assertCompilerVersionReported(":compileKotlinJs", KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API)
    }

    @Test
    fun testWasmCompilationWithoutBuildToolsApiReportsCompilerVersion() {
        val project = buildProject(disabledProperty = KOTLIN_WASM_RUN_COMPILER_VIA_BUILD_TOOLS_API)

        project.assertCompilerVersionReported(":compileKotlinWasmJs", KOTLIN_WASM_RUN_COMPILER_VIA_BUILD_TOOLS_API)
    }

    @Test
    fun testMetadataCompilationWithoutBuildToolsApiReportsCompilerVersion() {
        val project = buildProject(disabledProperty = KOTLIN_METADATA_RUN_COMPILER_VIA_BUILD_TOOLS_API)

        project.assertCompilerVersionReported(":compileCommonMainKotlinMetadata", KOTLIN_METADATA_RUN_COMPILER_VIA_BUILD_TOOLS_API)
    }

    @Test
    fun testCompilationViaBuildToolsApiDoesNotReportCompilerVersion() {
        val project = buildProject(disabledProperty = KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API, disabled = false)

        // The test project has no repository with the custom compiler, so resolving it may fail with KotlinCompilerVersionNotFound
        runCatching { project.resolveTaskDependencies(":compileKotlinJs") }
        project.assertNoDiagnostics(KotlinToolingDiagnostics.KotlinCompilerVersionWithoutBuildToolsApi)
    }

    @Test
    fun testCompilerVersionOfThePluginDoesNotReportCompilerVersion() {
        val project = buildProject(disabledProperty = KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API) { getKotlinPluginVersion() }

        project.resolveTaskDependencies(":compileKotlinJs")
        project.assertNoDiagnostics(KotlinToolingDiagnostics.KotlinCompilerVersionWithoutBuildToolsApi)
    }

    @Test
    fun testNativeCompilationDoesNotReportCompilerVersion() {
        val project = buildProject(disabledProperty = KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API, disabled = false)

        project.resolveTaskDependencies(":compileKotlinLinuxX64")
        project.assertNoDiagnostics(KotlinToolingDiagnostics.KotlinCompilerVersionWithoutBuildToolsApi)
    }

    private fun buildProject(
        disabledProperty: String,
        disabled: Boolean = true,
        compilerVersion: Project.() -> String = { COMPILER_VERSION },
    ): Project = buildProjectWithMPP(
        preApplyCode = {
            if (disabled) propertiesExtension.set(disabledProperty, "false")
        },
    ) {
        with(multiplatformExtension) {
            js()
            wasmJs()
            linuxX64()
            this.compilerVersion.set(compilerVersion())
        }
    }.also { it.evaluate() }

    private fun Project.assertCompilerVersionReported(taskPath: String, propertyName: String) {
        // The diagnostic is fatal, so it fails the task dependency resolution
        assertFails { resolveTaskDependencies(taskPath) }
        assertContainsDiagnostic(
            KotlinToolingDiagnostics.KotlinCompilerVersionWithoutBuildToolsApi(
                taskPath,
                COMPILER_VERSION,
                getKotlinPluginVersion(),
                propertyName,
            )
        )
    }

    private fun Project.resolveTaskDependencies(taskPath: String) {
        val task = tasks.getByPath(taskPath)
        task.taskDependencies.getDependencies(task)
    }

    private companion object {
        const val COMPILER_VERSION = "2.4.20"
    }
}
