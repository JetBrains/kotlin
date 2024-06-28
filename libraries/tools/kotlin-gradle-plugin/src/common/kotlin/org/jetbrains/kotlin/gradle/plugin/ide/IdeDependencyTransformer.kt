/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * An [IdeDependencyTransformer] will be invoked after all [IdeDependencyResolver] finished.
 * The transformation step is allowed to entirely modify the resolution result. Typical applications would be dependency filtering,
 * or grouping. Adding additional dependencies might be harmful, since other transformers might not be able to see this additional
 * dependency, running before the addition.
 *
 * Transformations can be scheduled in different phases see: [IdeMultiplatformImport.DependencyTransformationPhase]
 */
@ExternalKotlinTargetApi
fun interface IdeDependencyTransformer {
    fun transform(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>): Set<IdeaKotlinDependency>
}

/**
 * Creates a [IdeDependencyResolver] which will invoke the given [transformer] right after resolving the dependencies from the
 * receiver.
 */
@ExternalKotlinTargetApi
fun IdeDependencyResolver.withTransformer(transformer: IdeDependencyTransformer) = IdeDependencyResolver { sourceSet ->
    transformer.transform(sourceSet, this@withTransformer.resolve(sourceSet))
}

/**
 * Create a composite [IdeDependencyTransformer]
 * `null` instances will just be ignored.
 * The transformers will be invoked in the same order as specified to this function.
 */
@ExternalKotlinTargetApi
fun IdeDependencyTransformer(
    transformers: List<IdeDependencyTransformer?>
): IdeDependencyTransformer = IdeCompositeDependencyTransformer(transformers.filterNotNull())

/**
 * Create a composite [IdeDependencyTransformer]
 * `null` instances will just be ignored.
 * The transformers will be invoked in the same order as specified to this function.
 */
@ExternalKotlinTargetApi
fun IdeDependencyTransformer(
    vararg transformers: IdeDependencyTransformer?
): IdeDependencyTransformer = IdeDependencyTransformer(transformers.toList())

private class IdeCompositeDependencyTransformer(
    val transformers: List<IdeDependencyTransformer>
) : IdeDependencyTransformer {
    override fun transform(
        sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>
    ): Set<IdeaKotlinDependency> {
        return transformers.fold(dependencies) { currentDependencies, transformer -> transformer.transform(sourceSet, currentDependencies) }
    }
}
