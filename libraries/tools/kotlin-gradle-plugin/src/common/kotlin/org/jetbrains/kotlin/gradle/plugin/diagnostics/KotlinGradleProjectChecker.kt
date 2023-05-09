/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.CommonMainWithDependsOnChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DeprecatedKotlinNativeTargetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.MissingNativeStdlibChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.UnusedSourceSetsChecker

/**
 * Interface for generic checks of a Gradle Project with Kotlin Plugin applied
 *
 * All checkers are guaranteed to be launched in [org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution],
 * meaning that all changes to DSL are expected to be received, and all structures of KGP are finalised their state.
 *
 * Checkers **will not** be launched if the project's configuration has finished with failure (e.g. some plugin or a build script
 * threw an exception). If your check aims to prevent some exception, then code that check as soon as possible in the `apply`-block
 * of the [org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper] or appropriate inheritor
 */
internal fun interface KotlinGradleProjectChecker {
    fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector)

    companion object {
        val ALL_CHECKERS: List<KotlinGradleProjectChecker> = listOf(
            CommonMainWithDependsOnChecker,
            DeprecatedKotlinNativeTargetsChecker,
            MissingNativeStdlibChecker,
            UnusedSourceSetsChecker,
        )
    }
}

internal open class KotlinGradleProjectCheckerContext(
    val project: Project,
    val kotlinPropertiesProvider: PropertiesProvider,
    val multiplatformExtension: KotlinMultiplatformExtension?,
)
