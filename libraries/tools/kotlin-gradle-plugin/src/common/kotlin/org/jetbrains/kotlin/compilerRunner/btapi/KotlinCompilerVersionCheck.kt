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
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic

internal fun Project.checkKotlinCompilerVersionResolves(pluginVersion: String) {
    val compilerVersion = kotlinExtensionOrNull?.compilerVersion?.get() ?: return
    val unresolvedCompiler = configurations.getByName(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
        .incoming.resolutionResult.root.dependencies
        .filterIsInstance<UnresolvedDependencyResult>()
        .firstOrNull { (it.attempted as? ModuleComponentSelector)?.module == KOTLIN_BUILD_TOOLS_API_IMPL }
        ?: return
    val attemptedVersion = (unresolvedCompiler.attempted as ModuleComponentSelector).version
    reportDiagnostic(
        KotlinToolingDiagnostics.KotlinCompilerVersionNotFound(
            compilerVersion, attemptedVersion, pluginVersion, unresolvedCompiler.failure.message
        )
    )
}
