/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * An [IdeDependencyEffect] is invoked last in the dependency resolution pipeline.
 * It is not intended/capable of changing the resolution result, but can trigger side-effects, being sure
 * that the dependency resolution result is final.
 */
fun interface IdeDependencyEffect {
    operator fun invoke(sourceSet: KotlinSourceSet, dependencies: Set<IdeDependency>)
}

fun IdeDependencyResolver.withEffect(effect: IdeDependencyEffect) = IdeDependencyResolver { sourceSet ->
    this@withEffect.resolve(sourceSet).also { dependencies -> effect(sourceSet, dependencies) }
}
