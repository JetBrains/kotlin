/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Will be invoked once a new [KotlinTarget] was created.
 * See [registerKotlinPluginExtensions] to register an effect.
 *
 * ### Example
 * ```kotlin
 * internal val KotlinNativeTargetLoggingSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> {
 *     project.logger.debug(konanTarget.name)
 * }
 * ```
 *
 * Will print the name of a konanTarget once a new KotlinNativeTarget was created
 */
internal fun interface KotlinTargetSideEffect {
    operator fun invoke(target: KotlinTarget)

    companion object {
        val extensionPoint = KotlinGradlePluginExtensionPoint<KotlinTargetSideEffect>()
    }
}

/**
 * see [KotlinTargetSideEffect]
 */
internal inline fun <reified T : KotlinTarget> KotlinTargetSideEffect(crossinline effect: (T) -> Unit) = KotlinTargetSideEffect { target ->
    if (target is T) effect(target)
}


internal fun KotlinTarget.runKotlinTargetSideEffects() {
    KotlinTargetSideEffect.extensionPoint[project].forEach { effect ->
        effect(this)
    }
}
