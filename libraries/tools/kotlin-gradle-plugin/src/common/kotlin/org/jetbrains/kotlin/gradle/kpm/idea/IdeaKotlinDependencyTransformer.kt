/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment

/**
 * An [IdeaKpmDependencyTransformer] will be invoked after all [IdeaKpmDependencyResolver] finished.
 * The transformation step is allowed to entirely modify the resolution result. Typical applications would be dependency filtering,
 * or grouping. Adding additional dependencies might be harmful, since other transformers might not be able to see this additional
 * dependency, running before the addition.
 *
 * Transformations can be scheduled in different phases see: [IdeaKpmProjectModelBuilder.DependencyTransformationPhase]
 */
fun interface IdeaKpmDependencyTransformer {
    fun transform(
        fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>
    ): Set<IdeaKpmDependency>
}

fun IdeaKpmDependencyResolver.withTransformer(transformer: IdeaKpmDependencyTransformer) = IdeaKpmDependencyResolver { fragment ->
    transformer.transform(fragment, this@withTransformer.resolve(fragment))
}

fun IdeaKpmDependencyTransformer(
    transformers: List<IdeaKpmDependencyTransformer>
): IdeaKpmDependencyTransformer = IdeaKpmCompositeDependencyTransformer(transformers)

fun IdeaKpmDependencyTransformer(
    vararg transformers: IdeaKpmDependencyTransformer
): IdeaKpmDependencyTransformer = IdeaKpmDependencyTransformer(transformers.toList())

operator fun IdeaKpmDependencyTransformer.plus(other: IdeaKpmDependencyTransformer):
        IdeaKpmDependencyTransformer {
    if (this is IdeaKpmCompositeDependencyTransformer && other is IdeaKpmCompositeDependencyTransformer) {
        return IdeaKpmCompositeDependencyTransformer(this.transformers + other.transformers)
    }

    if (this is IdeaKpmCompositeDependencyTransformer) {
        return IdeaKpmCompositeDependencyTransformer(this.transformers + other)
    }

    if (other is IdeaKpmCompositeDependencyTransformer) {
        return IdeaKpmCompositeDependencyTransformer(listOf(this) + other.transformers)
    }

    return IdeaKpmCompositeDependencyTransformer(listOf(this, other))
}

private class IdeaKpmCompositeDependencyTransformer(
    val transformers: List<IdeaKpmDependencyTransformer>
) : IdeaKpmDependencyTransformer {
    override fun transform(
        fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>
    ): Set<IdeaKpmDependency> {
        return transformers.fold(dependencies) { currentDependencies, transformer -> transformer.transform(fragment, currentDependencies) }
    }
}
