/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap

class CirKnownClassifiers(
    val classifierIndices: TargetDependent<CirClassifierIndex>,
    val targetDependencies: TargetDependent<CirProvidedClassifiers>,
    val commonizedNodes: CirCommonizedClassifierNodes,
    val commonDependencies: CirProvidedClassifiers,
    val associatedIdsResolver: AssociatedClassifierIdsResolver =
        AssociatedClassifierIdsResolver(classifierIndices, targetDependencies, commonDependencies),
)

/** A set of all CIR nodes built for commonized classes and type aliases. */
interface CirCommonizedClassifierNodes {
    /* Accessors */
    fun classNode(classId: CirEntityId): CirClassNode?
    fun typeAliasNode(typeAliasId: CirEntityId): CirTypeAliasNode?

    /* Mutators */
    fun addClassNode(classId: CirEntityId, node: CirClassNode)
    fun addTypeAliasNode(typeAliasId: CirEntityId, node: CirTypeAliasNode)

    companion object {
        fun default(allowedDuplicates: Set<CirEntityId> = setOf()) = object : CirCommonizedClassifierNodes {
            private val classNodes = CommonizerMap<CirEntityId, CirClassNode>()
            private val typeAliases = CommonizerMap<CirEntityId, CirTypeAliasNode>()

            override fun classNode(classId: CirEntityId) = classNodes[classId]
            override fun typeAliasNode(typeAliasId: CirEntityId) = typeAliases[typeAliasId]

            override fun addClassNode(classId: CirEntityId, node: CirClassNode) {
                val oldNode = classNodes.put(classId, node)
                check(oldNode == null || classId in allowedDuplicates) { "Rewriting class node $classId" }
            }

            override fun addTypeAliasNode(typeAliasId: CirEntityId, node: CirTypeAliasNode) {
                val oldNode = typeAliases.put(typeAliasId, node)
                check(oldNode == null || typeAliasId in allowedDuplicates) { "Rewriting type alias node $typeAliasId" }
            }
        }
    }
}
