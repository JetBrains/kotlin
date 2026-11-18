/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.plugin.BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_METADATA_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_WASM_RUN_COMPILER_VIA_BUILD_TOOLS_API
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

internal fun Project.checkKotlinCompilerVersionResolves(pluginVersion: String) {
    val compilerVersion = kotlinExtensionOrNull?.compilerVersion?.get() ?: return
    if (compilerVersion == pluginVersion) return
    val unresolvedCompiler = configurations.getByName(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
        .incoming.resolutionResult.root.dependencies
        .filterIsInstance<UnresolvedDependencyResult>()
        .firstOrNull { (it.attempted as? ModuleComponentSelector)?.module == KOTLIN_BUILD_TOOLS_API_IMPL }
        ?: return
    val attemptedVersion = (unresolvedCompiler.attempted as ModuleComponentSelector).version
    reportDiagnostic(
        KotlinToolingDiagnostics.KotlinCompilerVersionNotFound(compilerVersion, attemptedVersion, unresolvedCompiler.failure.message)
    )
}

internal fun Project.checkKotlinCompilerVersionWithoutBuildToolsApi(task: AbstractKotlinCompileTool<*>, pluginVersion: String) {
    val propertyName = task.runViaBuildToolsApiPropertyName ?: return
    val compilerVersion = kotlinExtensionOrNull?.compilerVersion?.get() ?: return
    if (compilerVersion == pluginVersion) return
    reportDiagnostic(
        KotlinToolingDiagnostics.KotlinCompilerVersionWithoutBuildToolsApi(task.path, compilerVersion, pluginVersion, propertyName)
    )
}

// Only these tasks have a property that disables the Build Tools API; Kotlin/Native tasks don't support it yet
private val AbstractKotlinCompileTool<*>.runViaBuildToolsApiPropertyName: String?
    get() = when (this) {
        is KotlinCompileCommon -> KOTLIN_METADATA_RUN_COMPILER_VIA_BUILD_TOOLS_API
        is Kotlin2JsCompile -> if (getIsWasmPlatform.getOrElse(false)) {
            KOTLIN_WASM_RUN_COMPILER_VIA_BUILD_TOOLS_API
        } else {
            KOTLIN_JS_RUN_COMPILER_VIA_BUILD_TOOLS_API
        }
        else -> null
    }
