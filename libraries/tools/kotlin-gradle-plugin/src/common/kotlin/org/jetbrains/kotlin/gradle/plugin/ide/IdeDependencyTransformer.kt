/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * An [IdeDependencyTransformer] will be invoked after all [IdeDependencyResolver] finished.
 * The transformation step is allowed to entirely modify the resolution result. Typical applications would be dependency filtering,
 * or grouping. Adding additional dependencies might be harmful, since other transformers might not be able to see this additional
 * dependency, running before the addition.
 *
 * Transformations can be scheduled in different phases see: [IdeMultiplatformImport.DependencyTransformationPhase]
 */
fun interface IdeDependencyTransformer {
    fun transform(sourceSet: KotlinSourceSet, dependencies: Set<IdeDependency>): Set<IdeDependency>
}

fun IdeDependencyResolver.withTransformer(transformer: IdeDependencyTransformer) = IdeDependencyResolver { sourceSet ->
    transformer.transform(sourceSet, this@withTransformer.resolve(sourceSet))
}

fun IdeDependencyTransformer(
    transformers: List<IdeDependencyTransformer?>
): IdeDependencyTransformer = IdeCompositeDependencyTransformer(transformers.filterNotNull())

fun IdeDependencyTransformer(
    vararg transformers: IdeDependencyTransformer?
): IdeDependencyTransformer = IdeDependencyTransformer(transformers.toList())

operator fun IdeDependencyTransformer.plus(other: IdeDependencyTransformer):
        IdeDependencyTransformer {
    if (this is IdeCompositeDependencyTransformer && other is IdeCompositeDependencyTransformer) {
        return IdeCompositeDependencyTransformer(this.transformers + other.transformers)
    }

    if (this is IdeCompositeDependencyTransformer) {
        return IdeCompositeDependencyTransformer(this.transformers + other)
    }

    if (other is IdeCompositeDependencyTransformer) {
        return IdeCompositeDependencyTransformer(listOf(this) + other.transformers)
    }

    return IdeCompositeDependencyTransformer(listOf(this, other))
}

private class IdeCompositeDependencyTransformer(
    val transformers: List<IdeDependencyTransformer>
) : IdeDependencyTransformer {
    override fun transform(
        sourceSet: KotlinSourceSet, dependencies: Set<IdeDependency>
    ): Set<IdeDependency> {
        return transformers.fold(dependencies) { currentDependencies, transformer -> transformer.transform(sourceSet, currentDependencies) }
    }
}
