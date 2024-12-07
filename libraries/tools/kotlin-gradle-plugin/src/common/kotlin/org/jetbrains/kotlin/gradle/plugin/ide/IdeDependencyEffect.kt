/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * An [IdeDependencyEffect] is invoked last in the dependency resolution pipeline.
 * It is not intended/capable of changing the resolution result, but can trigger side-effects, being sure
 * that the dependency resolution result is final.
 */
@ExternalKotlinTargetApi
fun interface IdeDependencyEffect {
    operator fun invoke(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>)
}

/**
 * Wraps the given [IdeDependencyResolver] with the specified [effect]
 * The resulting resolver will first resolve the dependencies and then execute the effect on the result
 */
@ExternalKotlinTargetApi
fun IdeDependencyResolver.withEffect(effect: IdeDependencyEffect) = IdeDependencyResolver { sourceSet ->
    this@withEffect.resolve(sourceSet).also { dependencies -> effect(sourceSet, dependencies) }
}
