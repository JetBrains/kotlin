/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages

class CirKnownClassifiers(
    val commonized: CirCommonizedClassifiers,
    val forwardDeclarations: CirForwardDeclarations,
    val dependencies: Map<CommonizerTarget, CirProvidedClassifiers>
) {
    // a shortcut for fast access
    val commonDependencies: CirProvidedClassifiers =
        dependencies.filterKeys { it is SharedCommonizerTarget }.values.singleOrNull() ?: CirProvidedClassifiers.EMPTY
}

/** A set of all CIR nodes built for commonized classes and type aliases. */
interface CirCommonizedClassifiers {
    /* Accessors */
    fun classNode(classId: CirEntityId): CirClassNode?
    fun typeAliasNode(typeAliasId: CirEntityId): CirTypeAliasNode?

    /* Mutators */
    fun addClassNode(classId: CirEntityId, node: CirClassNode)
    fun addTypeAliasNode(typeAliasId: CirEntityId, node: CirTypeAliasNode)

    companion object {
        fun default() = object : CirCommonizedClassifiers {
            private val classNodes = THashMap<CirEntityId, CirClassNode>()
            private val typeAliases = THashMap<CirEntityId, CirTypeAliasNode>()

            override fun classNode(classId: CirEntityId) = classNodes[classId]
            override fun typeAliasNode(typeAliasId: CirEntityId) = typeAliases[typeAliasId]

            override fun addClassNode(classId: CirEntityId, node: CirClassNode) {
                val oldNode = classNodes.put(classId, node)
                check(oldNode == null) { "Rewriting class node $classId" }
            }

            override fun addTypeAliasNode(typeAliasId: CirEntityId, node: CirTypeAliasNode) {
                val oldNode = typeAliases.put(typeAliasId, node)
                check(oldNode == null) { "Rewriting type alias node $typeAliasId" }
            }
        }
    }
}

/** A set of all exported forward declaration classes/objects/structs. */
interface CirForwardDeclarations {
    /* Accessors */
    fun isExportedForwardDeclaration(classId: CirEntityId): Boolean

    /* Mutators */
    fun addExportedForwardDeclaration(classId: CirEntityId)

    companion object {
        fun default() = object : CirForwardDeclarations {
            private val exportedForwardDeclarations = THashSet<CirEntityId>()

            override fun isExportedForwardDeclaration(classId: CirEntityId) = classId in exportedForwardDeclarations

            override fun addExportedForwardDeclaration(classId: CirEntityId) {
                check(classId.packageName.isUnderKotlinNativeSyntheticPackages)
                exportedForwardDeclarations += classId
            }
        }
    }
}

/** A set of classes and type aliases provided by libraries (either the libraries to commonize, or their dependency libraries)/ */
interface CirProvidedClassifiers {
    fun hasClassifier(classifierId: CirEntityId): Boolean

    // TODO: implement later
    //fun classifier(classifierId: ClassId): Any?

    companion object {
        internal val EMPTY = object : CirProvidedClassifiers {
            override fun hasClassifier(classifierId: CirEntityId) = false
        }
    }
}
