/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.SharedTarget
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.descriptors.commonizer.utils.resolveClassOrTypeAlias
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class CirKnownClassifiers(
    val commonized: CirCommonizedClassifiers,
    val forwardDeclarations: CirForwardDeclarations,
    val dependencies: Map<CommonizerTarget, CirProvidedClassifiers>
) {
    // a shortcut for fast access
    val commonDependencies: CirProvidedClassifiers =
        dependencies.filterKeys { it is SharedTarget }.values.singleOrNull() ?: CirProvidedClassifiers.EMPTY
}

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
                check(!classId.packageName.isUnderKotlinNativeSyntheticPackages)
                exportedForwardDeclarations += classId
            }
        }
    }
}

interface CirProvidedClassifiers {
    fun hasClassifier(classifierId: CirEntityId): Boolean

    // TODO: implement later
    //fun classifier(classifierId: ClassId): Any?

    companion object {
        internal val EMPTY = object : CirProvidedClassifiers {
            override fun hasClassifier(classifierId: CirEntityId) = false
        }

        // N.B. This is suboptimal implementation. It will be replaced by another implementation that will
        // retrieve classifier information directly from the metadata.
        fun fromModules(storageManager: StorageManager, modules: () -> Collection<ModuleDescriptor>) = object : CirProvidedClassifiers {
            private val nonEmptyMemberScopes: Map<CirPackageName, MemberScope> by storageManager.createLazyValue {
                THashMap<CirPackageName, MemberScope>().apply {
                    for (module in modules()) {
                        module.collectNonEmptyPackageMemberScopes(probeRootPackageForEmptiness = true) { packageName, memberScope ->
                            this[packageName] = memberScope
                        }
                    }
                }
            }

            private val presentClassifiers = THashSet<CirEntityId>()
            private val missingClassifiers = THashSet<CirEntityId>()

            override fun hasClassifier(classifierId: CirEntityId): Boolean {
                if (classifierId.relativeNameSegments.isEmpty())
                    return false

                val memberScope = nonEmptyMemberScopes[classifierId.packageName] ?: return false

                return when (classifierId) {
                    in presentClassifiers -> true
                    in missingClassifiers -> false
                    else -> {
                        val found = memberScope.resolveClassOrTypeAlias(classifierId.relativeNameSegments) != null
                        when (found) {
                            true -> presentClassifiers += classifierId
                            false -> missingClassifiers += classifierId
                        }
                        found
                    }
                }
            }
        }
    }
}
