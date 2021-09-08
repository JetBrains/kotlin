/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MoveVariableDeclarationIntoWhen")

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.commonizer.forEachWithTarget
import org.jetbrains.kotlin.commonizer.mapValue

internal class CirCommonClassifierIdResolver(private val classifierIndices: TargetDependent<CirClassifierIndex>) {
    fun findCommonId(id: CirEntityId): CirCommonClassifierId? {
        val results = mutableSetOf<CirEntityId>()

        /* Set of every classifier id that once was enqueued already */
        val enqueued = mutableSetOf<CirEntityId>()

        /* Actual, current queue of classifiers to resolve */
        val queue = ArrayDeque<CirEntityId>()

        enqueued.add(id)
        queue.add(id)

        while (queue.isNotEmpty()) {
            val nextClassifierId = queue.removeFirst()
            val foundClassifiers = classifierIndices.mapValue { index -> index.findClassifier(nextClassifierId) }

            /* Classifier is available for all targets */
            if (foundClassifiers.all { it != null }) {
                results.add(nextClassifierId)
            }

            foundClassifiers.forEachWithTarget forEach@{ target, classifier ->
                if (classifier == null) return@forEach

                val classifierExpansionId = when (classifier) {
                    is CirClass -> nextClassifierId
                    is CirTypeAlias -> classifier.expandedType.classifierId
                }

                if (enqueued.add(classifierExpansionId)) {
                    queue.add(classifierExpansionId)
                }

                val aliases = classifierIndices[target].findAllTypeAliasesWithUnderlyingType(classifierExpansionId)
                aliases.forEach { alias ->
                    if (enqueued.add(alias.id)) {
                        queue.add(alias.id)
                    }
                }
            }
        }

        return if(results.isNotEmpty()) CirCommonClassifierId(results) else null
    }
}
