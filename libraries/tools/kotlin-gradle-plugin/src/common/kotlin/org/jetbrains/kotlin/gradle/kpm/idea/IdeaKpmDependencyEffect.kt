/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment

/**
 * An [IdeaKpmDependencyEffect] is invoked last in the dependency resolution pipeline.
 * It is not intended/capable of changing the resolution result, but can trigger side-effects, being sure
 * that the dependency resolution result is final.
 */
fun interface IdeaKpmDependencyEffect {
    operator fun invoke(fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>)
}

fun IdeaKpmDependencyResolver.withEffect(effect: IdeaKpmDependencyEffect) = IdeaKpmDependencyResolver { fragment ->
    this@withEffect.resolve(fragment).also { dependencies -> effect(fragment, dependencies) }
}
