/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.launch

internal fun interface KotlinCompilationSideEffect {
    operator fun invoke(compilation: KotlinCompilation<*>)

    companion object {
        val extensionPoint = KotlinGradlePluginExtensionPoint<KotlinCompilationSideEffect>()
    }
}

internal inline fun <reified T : KotlinCompilation<*>> KotlinCompilationSideEffect(
    crossinline effect: (T) -> Unit,
) = KotlinCompilationSideEffect { compilation ->
    if (compilation is T) effect(compilation)
}

internal inline fun <reified T : KotlinCompilation<*>> KotlinCompilationSideEffectCoroutine(
    crossinline effect: suspend (T) -> Unit,
) = KotlinCompilationSideEffect { compilation ->
    if (compilation is T) compilation.project.launch { effect(compilation) }
}

internal fun KotlinTarget.runKotlinCompilationSideEffects() = compilations.all { compilation ->
    KotlinCompilationSideEffect.extensionPoint[project].forEach { effect -> effect(compilation) }
}
