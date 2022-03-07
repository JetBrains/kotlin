/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

fun interface IdeaKotlinDependencyTransformer {
    fun transform(
        fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinDependency>
    ): Set<IdeaKotlinDependency>
}

fun IdeaKotlinDependencyResolver.withTransformer(transformer: IdeaKotlinDependencyTransformer) = IdeaKotlinDependencyResolver { fragment ->
    transformer.transform(fragment, this@withTransformer.resolve(fragment))
}

fun IdeaKotlinDependencyTransformer(
    transformers: List<IdeaKotlinDependencyTransformer>
): IdeaKotlinDependencyTransformer = CompositeIdeaKotlinDependencyTransformer(transformers)

fun IdeaKotlinDependencyTransformer(
    vararg transformers: IdeaKotlinDependencyTransformer
): IdeaKotlinDependencyTransformer = IdeaKotlinDependencyTransformer(transformers.toList())

operator fun IdeaKotlinDependencyTransformer.plus(other: IdeaKotlinDependencyTransformer):
        IdeaKotlinDependencyTransformer {
    if (this is CompositeIdeaKotlinDependencyTransformer && other is CompositeIdeaKotlinDependencyTransformer) {
        return CompositeIdeaKotlinDependencyTransformer(this.transformers + other.transformers)
    }

    if (this is CompositeIdeaKotlinDependencyTransformer) {
        return CompositeIdeaKotlinDependencyTransformer(this.transformers + other)
    }

    if (other is CompositeIdeaKotlinDependencyTransformer) {
        return CompositeIdeaKotlinDependencyTransformer(listOf(this) + other.transformers)
    }

    return CompositeIdeaKotlinDependencyTransformer(listOf(this, other))
}

private class CompositeIdeaKotlinDependencyTransformer(
    val transformers: List<IdeaKotlinDependencyTransformer>
) : IdeaKotlinDependencyTransformer {
    override fun transform(
        fragment: KotlinGradleFragment, dependencies: Set<IdeaKotlinDependency>
    ): Set<IdeaKotlinDependency> {
        return transformers.fold(dependencies) { currentDependencies, transformer -> transformer.transform(fragment, currentDependencies) }
    }
}
