/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MoveVariableDeclarationIntoWhen")

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirProvided
import org.jetbrains.kotlin.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap
import org.jetbrains.kotlin.commonizer.utils.CommonizerSet

internal fun AssociatedClassifierIdsResolver(
    classifierIndices: TargetDependent<CirClassifierIndex>,
    targetDependencies: TargetDependent<CirProvidedClassifiers>,
    commonDependencies: CirProvidedClassifiers = CirProvidedClassifiers.EMPTY,
    cache: AssociatedClassifierIdsResolverCache = AssociatedClassifierIdsResolverCache.create()
): AssociatedClassifierIdsResolver {
    return AssociatedClassifierIdsResolverImpl(classifierIndices, targetDependencies, commonDependencies, cache)
}

interface AssociatedClassifierIdsResolver {
    fun resolveAssociatedIds(id: CirEntityId): AssociatedClassifierIds?
}

internal interface AssociatedClassifierIdsResolverCache {

    operator fun set(id: CirEntityId, result: AssociatedClassifierIds?)
    operator fun get(id: CirEntityId): AssociatedClassifierIds?

    object None : AssociatedClassifierIdsResolverCache {
        override fun set(id: CirEntityId, result: AssociatedClassifierIds?) = Unit
        override fun get(id: CirEntityId): AssociatedClassifierIds? = null
    }

    private class Default : AssociatedClassifierIdsResolverCache {
        private val cachedResults = CommonizerMap<CirEntityId, AssociatedClassifierIds>()
        private val cachedNullResults = CommonizerSet<CirEntityId>()

        override fun set(id: CirEntityId, result: AssociatedClassifierIds?) {
            if (result == null) cachedNullResults.add(id)
            else cachedResults[id] = result
        }

        override fun get(id: CirEntityId): AssociatedClassifierIds? {
            if (id in cachedNullResults) return null
            return cachedResults[id]
        }
    }

    companion object {
        fun create(): AssociatedClassifierIdsResolverCache = Default()
    }
}

private class AssociatedClassifierIdsResolverImpl(
    private val classifierIndices: TargetDependent<CirClassifierIndex>,
    private val targetDependencies: TargetDependent<CirProvidedClassifiers>,
    private val commonDependencies: CirProvidedClassifiers,
    private val cache: AssociatedClassifierIdsResolverCache
) : AssociatedClassifierIdsResolver {

    override fun resolveAssociatedIds(id: CirEntityId): AssociatedClassifierIds? {
        cache[id]?.let { return it }

        val results = CommonizerSet<CirEntityId>()

        /* Set of every classifier id that once was enqueued already */
        val visited = CommonizerSet<CirEntityId>()

        /* Actual, current queue of classifiers to resolve */
        val queue = ArrayDeque<CirEntityId>()

        visited.add(id)
        queue.add(id)

        while (queue.isNotEmpty()) {
            val nextClassifierId = queue.removeFirst()

            /* Either CirClassifier or CirProvided.Classifier or null */
            val foundClassifiers = classifierIndices.targets.associateWith { index ->
                classifierIndices[index].findClassifier(nextClassifierId)
                    ?: targetDependencies[index].classifier(nextClassifierId)
                    ?: commonDependencies.classifier(nextClassifierId)
            }

            /* Classifier is available for all targets */
            if (foundClassifiers.all { (_, classifier) -> classifier != null }) {
                results.add(nextClassifierId)
            }

            foundClassifiers.forEach { (target, classifier) ->
                if (classifier == null) return@forEach

                // Propagate to the left (towards typealias)
                classifierIndices[target].findTypeAliasesWithUnderlyingType(nextClassifierId).forEach { alias ->
                    if (visited.add(alias.id)) {
                        queue.add(alias.id)
                    }
                }

                targetDependencies[target].findTypeAliasesWithUnderlyingType(nextClassifierId).forEach { aliasId ->
                    if (visited.add(aliasId)) {
                        queue.add(aliasId)
                    }
                }

                commonDependencies.findTypeAliasesWithUnderlyingType(nextClassifierId).forEach { aliasId ->
                    if (visited.add(aliasId)) {
                        queue.add(aliasId)
                    }
                }

                // Propagate to the right (towards expansion)
                if (classifier is CirTypeAlias) {
                    if (visited.add(classifier.underlyingType.classifierId)) {
                        queue.add(classifier.underlyingType.classifierId)
                    }
                }

                if (classifier is CirProvided.TypeAlias) {
                    if (visited.add(classifier.underlyingType.classifierId)) {
                        queue.add(classifier.underlyingType.classifierId)
                    }
                }
            }
        }

        val result = if (results.isNotEmpty()) AssociatedClassifierIds(results) else null
        visited.forEach { visitedId -> cache[visitedId] = result }
        return result
    }
}
