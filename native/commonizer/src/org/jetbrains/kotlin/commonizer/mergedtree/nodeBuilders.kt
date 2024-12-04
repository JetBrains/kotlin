/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.core.*
import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

internal fun buildRootNode(
    storageManager: StorageManager,
    commonDependencies: CirProvidedClassifiers,
    size: Int
): CirRootNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = null,
    commonizerProducer = ::RootCommonizer,
    nodeProducer = { targetDeclarations, commonDeclaration -> CirRootNode(commonDependencies, targetDeclarations, commonDeclaration) }
)

internal fun buildModuleNode(
    storageManager: StorageManager,
    size: Int
): CirModuleNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = null,
    commonizerProducer = ::ModuleCommonizer,
    nodeProducer = ::CirModuleNode
)

internal fun buildPackageNode(
    storageManager: StorageManager,
    size: Int
): CirPackageNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = null,
    commonizerProducer = ::PackageCommonizer,
    nodeProducer = ::CirPackageNode
)

internal fun buildPropertyNode(
    storageManager: StorageManager,
    size: Int,
    classifiers: CirKnownClassifiers,
    settings: CommonizerSettings,
    nodeRelationship: CirNodeRelationship? = null,
): CirPropertyNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = nodeRelationship,
    commonizerProducer = {
        PropertyCommonizer(FunctionOrPropertyBaseCommonizer(classifiers, settings, TypeCommonizer(classifiers, settings)))
    },
    nodeProducer = ::CirPropertyNode
)

internal fun buildFunctionNode(
    storageManager: StorageManager,
    size: Int,
    classifiers: CirKnownClassifiers,
    settings: CommonizerSettings,
    nodeRelationship: CirNodeRelationship?,
): CirFunctionNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = nodeRelationship,
    commonizerProducer = {
        val typeCommonizer = TypeCommonizer(classifiers, settings)
        FunctionCommonizer(typeCommonizer, FunctionOrPropertyBaseCommonizer(classifiers, settings, typeCommonizer)).asCommonizer()
    },
    nodeProducer = ::CirFunctionNode
)

internal fun buildClassNode(
    storageManager: StorageManager,
    size: Int,
    classifiers: CirKnownClassifiers,
    settings: CommonizerSettings,
    nodeRelationship: CirNodeRelationship?,
    classId: CirEntityId,
): CirClassNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = nodeRelationship,
    commonizerProducer = {
        val typeCommonizer = TypeCommonizer(classifiers, settings)
        ClassCommonizer(typeCommonizer, ClassSuperTypeCommonizer(classifiers, typeCommonizer))
    },
    recursionMarker = CirClassRecursionMarker,
    nodeProducer = { targetDeclarations, commonDeclaration ->
        CirClassNode(classId, targetDeclarations, commonDeclaration).also {
            classifiers.commonizedNodes.addClassNode(classId, it)
        }
    }
)


internal fun buildClassConstructorNode(
    storageManager: StorageManager,
    size: Int,
    classifiers: CirKnownClassifiers,
    settings: CommonizerSettings,
    nodeRelationship: CirNodeRelationship?,
): CirClassConstructorNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = nodeRelationship,
    commonizerProducer = { ClassConstructorCommonizer(TypeCommonizer(classifiers, settings)) },
    nodeProducer = ::CirClassConstructorNode
)

internal fun buildTypeAliasNode(
    storageManager: StorageManager,
    size: Int,
    classifiers: CirKnownClassifiers,
    settings: CommonizerSettings,
    typeAliasId: CirEntityId,
): CirTypeAliasNode = buildNode(
    storageManager = storageManager,
    size = size,
    nodeRelationship = null,
    commonizerProducer = { TypeAliasCommonizer(classifiers, settings, TypeCommonizer(classifiers, settings)).asCommonizer() },
    recursionMarker = CirTypeAliasRecursionMarker,
    nodeProducer = { targetDeclarations, commonDeclaration ->
        CirTypeAliasNode(typeAliasId, targetDeclarations, commonDeclaration).also {
            classifiers.commonizedNodes.addTypeAliasNode(typeAliasId, it)
        }
    }
)

private fun <T : CirDeclaration, R : CirDeclaration, N : CirNode<T, R>> buildNode(
    storageManager: StorageManager,
    size: Int,
    nodeRelationship: CirNodeRelationship?,
    commonizerProducer: () -> Commonizer<T, R?>,
    recursionMarker: R? = null,
    nodeProducer: (CommonizedGroup<T>, NullableLazyValue<R>) -> N
): N {
    val targetDeclarations = CommonizedGroup<T>(size)

    val commonComputable = { commonize(nodeRelationship, targetDeclarations, commonizerProducer()) }

    val commonLazyValue = if (recursionMarker != null)
        storageManager.createRecursionTolerantNullableLazyValue(commonComputable, recursionMarker)
    else
        storageManager.createNullableLazyValue(commonComputable)

    return nodeProducer(targetDeclarations, commonLazyValue)
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any, R> commonize(
    targetDeclarations: CommonizedGroup<T>,
    commonizer: Commonizer<T, R>
): R? {
    if (targetDeclarations.any { it == null }) return null
    return commonizer.commonize(targetDeclarations as List<T>)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Any, R> commonize(
    nodeRelationship: CirNodeRelationship?,
    targetDeclarations: CommonizedGroup<T>,
    commonizer: Commonizer<T, R>
): R? {
    if (nodeRelationship.shouldCommonize()) {
        return commonize(targetDeclarations, commonizer)
    }

    return null
}

private fun CirNodeRelationship?.shouldCommonize(): Boolean {
    return when (this) {
        null -> true
        is CirNodeRelationship.ParentNode -> node.commonDeclaration() != null
        is CirNodeRelationship.PreferredNode -> node.commonDeclaration() == null
        is CirNodeRelationship.Composite -> relationships.all { it.shouldCommonize() }
    }
}
