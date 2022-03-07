/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

fun interface IdeaKotlinDependencyEffect {
    operator fun invoke(fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinDependency>)
}

fun IdeaKotlinDependencyResolver.withEffect(effect: IdeaKotlinDependencyEffect) = IdeaKotlinDependencyResolver { fragment ->
    this@withEffect.resolve(fragment).also { dependencies -> effect(fragment, dependencies) }
}
