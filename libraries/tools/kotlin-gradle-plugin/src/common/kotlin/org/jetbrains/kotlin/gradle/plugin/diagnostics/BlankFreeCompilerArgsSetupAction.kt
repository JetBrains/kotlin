/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.BlankFreeCompilerArgs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinToolTask

/**
 * Reports [BlankFreeCompilerArgs] right before a Kotlin task runs.
 *
 * Every Kotlin task that accepts `freeCompilerArgs` exposes them through [KotlinCompilationTask] or [KotlinToolTask],
 * so new tasks are covered without having to call the check themselves.
 */
internal val BlankFreeCompilerArgsSetupAction = KotlinProjectSetupAction {
    tasks.withType(UsesKotlinToolingDiagnostics::class.java).configureEach { task ->
        val options = when (task) {
            is KotlinCompilationTask<*> -> task.compilerOptions
            is KotlinToolTask<*> -> task.toolOptions
            else -> return@configureEach
        }
        task.reportBlankFreeCompilerArgs(options)
    }
}

private fun UsesKotlinToolingDiagnostics.reportBlankFreeCompilerArgs(options: KotlinCommonCompilerToolOptions) {
    val freeCompilerArgs = options.freeCompilerArgs
    doFirst {
        if (freeCompilerArgs.get().any { it.isBlank() }) {
            reportDiagnostic(BlankFreeCompilerArgs(path))
        }
    }
}
