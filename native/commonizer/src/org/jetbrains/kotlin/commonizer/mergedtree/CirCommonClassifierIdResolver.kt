/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MoveVariableDeclarationIntoWhen")

package org.jetbrains.kotlin.commonizer.mergedtree

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirTypeAlias

internal fun CirCommonClassifierIdResolver(
    classifierIndices: TargetDependent<CirClassifierIndex>,
    targetDependencies: TargetDependent<CirProvidedClassifiers>,
    commonDependencies: CirProvidedClassifiers = CirProvidedClassifiers.EMPTY,
    cache: CirCommonClassifierIdResolverCache = CirCommonClassifierIdResolverCache.create()
): CirCommonClassifierIdResolver {
    return CirCommonClassifierIdResolverImpl(classifierIndices, targetDependencies, commonDependencies, cache)
}

interface CirCommonClassifierIdResolver {
    fun findCommonId(id: CirEntityId): CirCommonClassifierId?
}

internal interface CirCommonClassifierIdResolverCache {

    operator fun set(id: CirEntityId, result: CirCommonClassifierId?)
    operator fun get(id: CirEntityId): CirCommonClassifierId?

    object None : CirCommonClassifierIdResolverCache {
        override fun set(id: CirEntityId, result: CirCommonClassifierId?) = Unit
        override fun get(id: CirEntityId): CirCommonClassifierId? = null
    }

    private class Default : CirCommonClassifierIdResolverCache {
        private val cachedResults = THashMap<CirEntityId, CirCommonClassifierId>()
        private val cachedNullResults = THashSet<CirEntityId>()

        override fun set(id: CirEntityId, result: CirCommonClassifierId?) {
            if (result == null) cachedNullResults.add(id)
            else cachedResults[id] = result
        }

        override fun get(id: CirEntityId): CirCommonClassifierId? {
            if (id in cachedNullResults) return null
            return cachedResults[id]
        }
    }

    companion object {
        fun create(): CirCommonClassifierIdResolverCache = Default()
    }
}

private class CirCommonClassifierIdResolverImpl(
    private val classifierIndices: TargetDependent<CirClassifierIndex>,
    private val targetDependencies: TargetDependent<CirProvidedClassifiers>,
    private val commonDependencies: CirProvidedClassifiers,
    private val cache: CirCommonClassifierIdResolverCache
) : CirCommonClassifierIdResolver {

    override fun findCommonId(id: CirEntityId): CirCommonClassifierId? {
        cache[id]?.let { return it }
        return doFindCommonId(id)
    }

    private fun doFindCommonId(id: CirEntityId): CirCommonClassifierId? {
        val results = ArrayList<CirEntityId>()

        /* Set of every classifier id that once was enqueued already */
        val visited = THashSet<CirEntityId>()

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

        val result = if (results.isNotEmpty()) CirCommonClassifierId(results) else null
        visited.forEach { visitedId -> cache[visitedId] = result }
        return result
    }
}
