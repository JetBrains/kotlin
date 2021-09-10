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
    dependencies: CirProvidedClassifiers = CirProvidedClassifiers.EMPTY
): CirCommonClassifierIdResolver {
    return CirCommonClassifierIdResolverImpl(classifierIndices, dependencies)
}

interface CirCommonClassifierIdResolver {
    fun findCommonId(id: CirEntityId): CirCommonClassifierId?
}

private class CirCommonClassifierIdResolverImpl(
    private val classifierIndices: TargetDependent<CirClassifierIndex>,
    private val dependencies: CirProvidedClassifiers
) : CirCommonClassifierIdResolver {

    private val cachedResults = THashMap<CirEntityId, CirCommonClassifierId>()
    private val cachedNullResults = THashSet<CirEntityId>()

    override fun findCommonId(id: CirEntityId): CirCommonClassifierId? {
        cachedResults[id]?.let { return it }
        if (id in cachedNullResults) return null
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
            val foundClassifiers = classifierIndices.associateWith { index ->
                index.findClassifier(nextClassifierId) ?: dependencies.classifier(nextClassifierId)
            }

            /* Classifier is available for all targets */
            if (foundClassifiers.all { (_, classifier) -> classifier != null }) {
                results.add(nextClassifierId)
            }

            foundClassifiers.forEach { (index, classifier) ->
                if (classifier == null) return@forEach

                // Propagate to the left (towards typealias)
                index.findTypeAliasesWithUnderlyingType(nextClassifierId).forEach { alias ->
                    if (visited.add(alias.id)) {
                        queue.add(alias.id)
                    }
                }

                dependencies.findTypeAliasesWithUnderlyingType(nextClassifierId).forEach { aliasId ->
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
        if (result != null) visited.forEach { cachedResults[it] = result }
        else visited.forEach { cachedNullResults.add(it) }
        return result
    }
}
