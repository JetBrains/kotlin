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
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirRootNode.ClassifiersCacheImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

internal fun buildRootNode(
    storageManager: StorageManager,
    targets: List<InputTarget>
): CirRootNode = CirRootNode(
    target = targets.map { CirRoot(it) },
    common = storageManager.createNullableLazyValue {
        CirRoot(OutputTarget(targets.toSet()))
    }
)

internal fun buildModuleNode(
    storageManager: StorageManager,
    modules: List<ModuleDescriptor?>
): CirModuleNode = buildNode(
    storageManager = storageManager,
    descriptors = modules,
    targetDeclarationProducer = ::CirModule,
    commonValueProducer = { commonize(it, ModuleCommonizer.default()) },
    recursionMarker = null,
    nodeProducer = ::CirModuleNode
)

internal fun buildPackageNode(
    storageManager: StorageManager,
    packageFqName: FqName,
    packageMemberScopes: List<MemberScope?>
): CirPackageNode = buildNode(
    storageManager = storageManager,
    descriptors = packageMemberScopes,
    targetDeclarationProducer = { CirPackage(packageFqName) },
    commonValueProducer = { CirPackage(packageFqName) },
    recursionMarker = null,
    nodeProducer = ::CirPackageNode
).also { node ->
    node.fqName = packageFqName
}

internal fun buildPropertyNode(
    storageManager: StorageManager,
    cache: CirClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    properties: List<PropertyDescriptor?>
): CirPropertyNode = buildNode(
    storageManager = storageManager,
    descriptors = properties,
    targetDeclarationProducer = ::CirWrappedProperty,
    commonValueProducer = { commonize(containingDeclarationCommon, it, PropertyCommonizer(cache)) },
    recursionMarker = null,
    nodeProducer = ::CirPropertyNode
)

internal fun buildFunctionNode(
    storageManager: StorageManager,
    cache: CirClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    functions: List<SimpleFunctionDescriptor?>
): CirFunctionNode = buildNode(
    storageManager = storageManager,
    descriptors = functions,
    targetDeclarationProducer = ::CirWrappedFunction,
    commonValueProducer = { commonize(containingDeclarationCommon, it, FunctionCommonizer(cache)) },
    recursionMarker = null,
    nodeProducer = ::CirFunctionNode
)

internal fun buildClassNode(
    storageManager: StorageManager,
    cacheRW: ClassifiersCacheImpl,
    containingDeclarationCommon: NullableLazyValue<*>?,
    classes: List<ClassDescriptor?>
): CirClassNode = buildNode(
    storageManager = storageManager,
    descriptors = classes,
    targetDeclarationProducer = ::CirWrappedClass,
    commonValueProducer = { commonize(containingDeclarationCommon, it, ClassCommonizer(cacheRW)) },
    recursionMarker = CirClassRecursionMarker,
    nodeProducer = ::CirClassNode
).also { node ->
    classes.firstNonNull().fqNameSafe.let { fqName ->
        node.fqName = fqName
        cacheRW.classes.putSafe(fqName, node)
    }
}

internal fun buildClassConstructorNode(
    storageManager: StorageManager,
    cache: CirClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    constructors: List<ClassConstructorDescriptor?>
): CirClassConstructorNode = buildNode(
    storageManager = storageManager,
    descriptors = constructors,
    targetDeclarationProducer = ::CirWrappedClassConstructor,
    commonValueProducer = { commonize(containingDeclarationCommon, it, ClassConstructorCommonizer(cache)) },
    recursionMarker = null,
    nodeProducer = ::CirClassConstructorNode
)

internal fun buildTypeAliasNode(
    storageManager: StorageManager,
    cacheRW: ClassifiersCacheImpl,
    typeAliases: List<TypeAliasDescriptor?>
): CirTypeAliasNode = buildNode(
    storageManager = storageManager,
    descriptors = typeAliases,
    targetDeclarationProducer = ::CirWrappedTypeAlias,
    commonValueProducer = { commonize(it, TypeAliasCommonizer(cacheRW)) },
    recursionMarker = CirClassRecursionMarker,
    nodeProducer = ::CirTypeAliasNode
).also { node ->
    typeAliases.firstNonNull().fqNameSafe.let { fqName ->
        node.fqName = fqName
        cacheRW.typeAliases.putSafe(fqName, node)
    }
}

private fun <D : Any, T : CirDeclaration, R : CirDeclaration, N : CirNode<T, R>> buildNode(
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

@Suppress("NOTHING_TO_INLINE")
private inline fun <K, V : Any> MutableMap<K, V>.putSafe(key: K, value: V) = put(key, value)?.let { oldValue ->
    error("${oldValue::class.java} with key=$key has been overwritten: $oldValue")
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
