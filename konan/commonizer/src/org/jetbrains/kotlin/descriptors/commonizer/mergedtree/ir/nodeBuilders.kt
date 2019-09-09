/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.core.*
import org.jetbrains.kotlin.descriptors.commonizer.firstNonNull
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.RootNode.ClassifiersCacheImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

internal fun buildRootNode(
    targets: List<ConcreteTargetId>
): RootNode = RootNode(
    target = targets.map { Root(it) },
    common = LockBasedStorageManager.NO_LOCKS.createNullableLazyValue {
        Root(CommonTargetId(targets.toSet()))
    }
)

internal fun buildModuleNode(
    moduleName: Name,
    modules: List<ModuleDescriptor?>
): ModuleNode = buildNode(
    storageManager = LockBasedStorageManager.NO_LOCKS,
    descriptors = modules,
    targetDeclarationProducer = { Module(moduleName) },
    commonValueProducer = { Module(moduleName) },
    recursionMarker = null,
    nodeProducer = ::ModuleNode
)

internal fun buildPackageNode(
    packageFqName: FqName,
    packageMemberScopes: List<MemberScope?>
): PackageNode = buildNode(
    storageManager = LockBasedStorageManager.NO_LOCKS,
    descriptors = packageMemberScopes,
    targetDeclarationProducer = { Package(packageFqName) },
    commonValueProducer = { Package(packageFqName) },
    recursionMarker = null,
    nodeProducer = ::PackageNode
)

internal fun buildPropertyNode(
    storageManager: StorageManager,
    cache: ClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    properties: List<PropertyDescriptor?>
): PropertyNode = buildNode(
    storageManager = storageManager,
    descriptors = properties,
    targetDeclarationProducer = ::TargetProperty,
    commonValueProducer = { commonize(containingDeclarationCommon, it, PropertyCommonizer(cache)) },
    recursionMarker = null,
    nodeProducer = ::PropertyNode
)

internal fun buildFunctionNode(
    storageManager: StorageManager,
    cache: ClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    functions: List<SimpleFunctionDescriptor?>
): FunctionNode = buildNode(
    storageManager = storageManager,
    descriptors = functions,
    targetDeclarationProducer = ::TargetFunction,
    commonValueProducer = { commonize(containingDeclarationCommon, it, FunctionCommonizer(cache)) },
    recursionMarker = null,
    nodeProducer = ::FunctionNode
)

internal fun buildClassNode(
    storageManager: StorageManager,
    cacheRW: ClassifiersCacheImpl,
    containingDeclarationCommon: NullableLazyValue<*>?,
    classes: List<ClassDescriptor?>
): ClassNode {
    val fqName = classes.firstNonNull().fqNameSafe

    return buildNode(
        storageManager = storageManager,
        descriptors = classes,
        targetDeclarationProducer = ::TargetClassDeclaration,
        commonValueProducer = { commonize(containingDeclarationCommon, it, ClassCommonizer(cacheRW)) },
        recursionMarker = ClassDeclarationRecursionMarker,
        nodeProducer = ::ClassNode
    ).also { node ->
        node.fqName = fqName
        cacheRW.classes.put(fqName, node)?.let { oldNode ->
            throw IllegalStateException("Class node with FQ name $fqName has been overwritten: $oldNode")
        }
    }
}

internal fun buildClassConstructorNode(
    storageManager: StorageManager,
    cache: ClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    constructors: List<ClassConstructorDescriptor?>
): ClassConstructorNode = buildNode(
    storageManager = storageManager,
    descriptors = constructors,
    targetDeclarationProducer = ::TargetClassConstructor,
    commonValueProducer = { commonize(containingDeclarationCommon, it, ClassConstructorCommonizer(cache)) },
    recursionMarker = null,
    nodeProducer = ::ClassConstructorNode
)

internal fun buildTypeAliasNode(
    storageManager: StorageManager,
    cacheRW: ClassifiersCacheImpl,
    typeAliases: List<TypeAliasDescriptor?>
): TypeAliasNode {
    val fqName = typeAliases.firstNonNull().fqNameSafe

    return buildNode(
        storageManager = storageManager,
        descriptors = typeAliases,
        targetDeclarationProducer = ::TargetTypeAlias,
        commonValueProducer = { commonize(it, TypeAliasCommonizer(cacheRW)) },
        recursionMarker = ClassDeclarationRecursionMarker,
        nodeProducer = ::TypeAliasNode
    ).also { node ->
        node.fqName = fqName
        cacheRW.typeAliases.put(fqName, node)?.let { oldNode ->
            throw IllegalStateException("Type alias node with FQ name $fqName has been overwritten: $oldNode")
        }
    }
}

private fun <D : Any, T : Declaration, R : Declaration, N : Node<T, R>> buildNode(
    storageManager: StorageManager,
    descriptors: List<D?>,
    targetDeclarationProducer: (D) -> T,
    commonValueProducer: (List<T?>) -> R?,
    recursionMarker: R?,
    nodeProducer: (List<T?>, NullableLazyValue<R>) -> N
): N {
    val declarationsGroup = CommonizedGroup<T>(descriptors.size)
    var canHaveCommon = descriptors.size > 1

    descriptors.forEachIndexed { index, descriptor ->
        if (descriptor != null)
            declarationsGroup[index] = targetDeclarationProducer(descriptor)
        else
            canHaveCommon = false
    }

    val declarations = declarationsGroup.toList()

    val commonComputable: () -> R? = if (canHaveCommon) {
        { commonValueProducer(declarations) }
    } else {
        { null }
    }

    val commonLazyValue = if (recursionMarker != null)
        storageManager.createRecursionTolerantNullableLazyValue(commonComputable, recursionMarker)
    else
        storageManager.createNullableLazyValue(commonComputable)

    return nodeProducer(declarations, commonLazyValue)
}

internal fun <T, R> commonize(declarations: List<T?>, commonizer: Commonizer<T, R>): R? {
    for (declaration in declarations) {
        if (declaration == null || !commonizer.commonizeWith(declaration))
            return null
    }

    return commonizer.result
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T, R> commonize(
    containingDeclarationCommon: NullableLazyValue<*>?,
    declarations: List<T?>,
    commonizer: Commonizer<T, R>
): R? {
    if (containingDeclarationCommon != null && containingDeclarationCommon.invoke() == null) {
        // don't commonize declaration if it has commonizable containing declaration that has not been successfully commonized
        return null
    }

    return commonize(declarations, commonizer)
}
