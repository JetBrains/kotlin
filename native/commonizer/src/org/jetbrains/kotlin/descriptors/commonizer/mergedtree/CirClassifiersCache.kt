/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.name.ClassId

interface CirClassifiersCache {
    fun isExportedForwardDeclaration(classId: ClassId): Boolean

    fun classNode(classId: ClassId): CirClassNode?
    fun typeAliasNode(typeAliasId: ClassId): CirTypeAliasNode?

    fun addExportedForwardDeclaration(classId: ClassId)

    fun addClassNode(classId: ClassId, node: CirClassNode)
    fun addTypeAliasNode(typeAliasId: ClassId, node: CirTypeAliasNode)
}

class DefaultCirClassifiersCache : CirClassifiersCache {
    private val exportedForwardDeclarations = THashSet<ClassId>()
    private val classNodes = THashMap<ClassId, CirClassNode>()
    private val typeAliases = THashMap<ClassId, CirTypeAliasNode>()

    override fun isExportedForwardDeclaration(classId: ClassId) = classId in exportedForwardDeclarations
    override fun classNode(classId: ClassId) = classNodes[classId]
    override fun typeAliasNode(typeAliasId: ClassId) = typeAliases[typeAliasId]

    override fun addExportedForwardDeclaration(classId: ClassId) {
        check(!classId.packageFqName.isUnderKotlinNativeSyntheticPackages)
        exportedForwardDeclarations += classId
    }

    override fun addClassNode(classId: ClassId, node: CirClassNode) {
        val oldNode = classNodes.put(classId, node)
        check(oldNode == null) { "Rewriting class node $classId" }
    }

    override fun addTypeAliasNode(typeAliasId: ClassId, node: CirTypeAliasNode) {
        val oldNode = typeAliases.put(typeAliasId, node)
        check(oldNode == null) { "Rewriting type alias node $typeAliasId" }
    }
}
