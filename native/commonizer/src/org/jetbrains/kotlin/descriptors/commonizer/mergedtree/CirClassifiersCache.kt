/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.SharedTarget
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderKotlinNativeSyntheticPackages
import org.jetbrains.kotlin.descriptors.commonizer.utils.resolveClassOrTypeAlias
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class CirKnownClassifiers(
    val commonized: CirCommonizedClassifiers,
    val forwardDeclarations: CirForwardDeclarations,
    val dependeeLibraries: Map<Target, CirProvidedClassifiers>
) {
    // a shortcut for fast access
    val commonDependeeLibraries: CirProvidedClassifiers =
        dependeeLibraries.filterKeys { it is SharedTarget }.values.singleOrNull() ?: CirProvidedClassifiers.EMPTY
}

interface CirCommonizedClassifiers {
    /* Accessors */
    fun classNode(classId: ClassId): CirClassNode?
    fun typeAliasNode(typeAliasId: ClassId): CirTypeAliasNode?

    /* Mutators */
    fun addClassNode(classId: ClassId, node: CirClassNode)
    fun addTypeAliasNode(typeAliasId: ClassId, node: CirTypeAliasNode)

    companion object {
        fun default() = object : CirCommonizedClassifiers {
            private val classNodes = THashMap<ClassId, CirClassNode>()
            private val typeAliases = THashMap<ClassId, CirTypeAliasNode>()

            override fun classNode(classId: ClassId) = classNodes[classId]
            override fun typeAliasNode(typeAliasId: ClassId) = typeAliases[typeAliasId]

            override fun addClassNode(classId: ClassId, node: CirClassNode) {
                val oldNode = classNodes.put(classId, node)
                check(oldNode == null) { "Rewriting class node $classId" }
            }

            override fun addTypeAliasNode(typeAliasId: ClassId, node: CirTypeAliasNode) {
                val oldNode = typeAliases.put(typeAliasId, node)
                check(oldNode == null) { "Rewriting type alias node $typeAliasId" }
            }
        }
    }
}

interface CirForwardDeclarations {
    /* Accessors */
    fun isExportedForwardDeclaration(classId: ClassId): Boolean

    /* Mutators */
    fun addExportedForwardDeclaration(classId: ClassId)

    companion object {
        fun default() = object : CirForwardDeclarations {
            private val exportedForwardDeclarations = THashSet<ClassId>()

            override fun isExportedForwardDeclaration(classId: ClassId) = classId in exportedForwardDeclarations

            override fun addExportedForwardDeclaration(classId: ClassId) {
                check(!classId.packageFqName.isUnderKotlinNativeSyntheticPackages)
                exportedForwardDeclarations += classId
            }
        }
    }
}

interface CirProvidedClassifiers {
    fun hasClassifier(classifierId: ClassId): Boolean

    // TODO: implement later
    //fun classifier(classifierId: ClassId): Any?

    companion object {
        internal val EMPTY = object : CirProvidedClassifiers {
            override fun hasClassifier(classifierId: ClassId) = false
        }

        // N.B. This is suboptimal implementation. It will be replaced by another implementation that will
        // retrieve classifier information directly from the metadata.
        fun fromModules(storageManager: StorageManager, modules: () -> Collection<ModuleDescriptor>) = object : CirProvidedClassifiers {
            private val nonEmptyMemberScopes: Map<FqName, MemberScope> by storageManager.createLazyValue {
                THashMap<FqName, MemberScope>().apply {
                    for (module in modules()) {
                        module.collectNonEmptyPackageMemberScopes(probeRootPackageForEmptiness = true) { packageFqName, memberScope ->
                            this[packageFqName.intern()] = memberScope
                        }
                    }
                }
            }

            private val presentClassifiers = THashSet<ClassId>()
            private val missingClassifiers = THashSet<ClassId>()

            override fun hasClassifier(classifierId: ClassId): Boolean {
                val relativeClassName: FqName = classifierId.relativeClassName
                if (relativeClassName.isRoot)
                    return false

                val packageFqName = classifierId.packageFqName
                val memberScope = nonEmptyMemberScopes[packageFqName] ?: return false

                return when (classifierId) {
                    in presentClassifiers -> true
                    in missingClassifiers -> false
                    else -> {
                        val found = memberScope.resolveClassOrTypeAlias(relativeClassName) != null
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
