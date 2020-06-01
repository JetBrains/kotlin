/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.TargetProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirDeclaration
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassRecursionMarker
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassifierRecursionMarker
import org.jetbrains.kotlin.descriptors.commonizer.core.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirRootNode.ClassifiersCacheImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.utils.firstNonNull
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

internal fun buildRootNode(
    storageManager: StorageManager,
    targetProviders: List<TargetProvider>
): CirRootNode = buildNode(
    storageManager = storageManager,
    descriptors = targetProviders,
    targetDeclarationProducer = { CirRootFactory.create(it.target, it.builtInsClass.name, it.builtInsProvider) },
    commonValueProducer = { commonize(it, RootCommonizer()) },
    recursionMarker = null,
    nodeProducer = ::CirRootNode
)

internal fun buildModuleNode(
    storageManager: StorageManager,
    modules: List<ModuleDescriptor?>
): CirModuleNode = buildNode(
    storageManager = storageManager,
    descriptors = modules,
    targetDeclarationProducer = CirModuleFactory::create,
    commonValueProducer = { commonize(it, ModuleCommonizer()) },
    recursionMarker = null,
    nodeProducer = ::CirModuleNode
)

internal fun buildPackageNode(
    storageManager: StorageManager,
    moduleName: Name,
    packageFqName: FqName,
    packageMemberScopes: List<MemberScope?>
): CirPackageNode = buildNode(
    storageManager = storageManager,
    descriptors = packageMemberScopes,
    targetDeclarationProducer = { CirPackageFactory.create(packageFqName) },
    commonValueProducer = { CirPackageFactory.create(packageFqName) },
    recursionMarker = null,
    nodeProducer = ::CirPackageNode
).also { node ->
    node.moduleName = moduleName
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
    targetDeclarationProducer = CirPropertyFactory::create,
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
    targetDeclarationProducer = CirFunctionFactory::create,
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
    targetDeclarationProducer = CirClassFactory::create,
    commonValueProducer = { commonize(containingDeclarationCommon, it, ClassCommonizer(cacheRW)) },
    recursionMarker = CirClassRecursionMarker,
    nodeProducer = ::CirClassNode
).also { node ->
    classes.firstNonNull().fqNameSafe.intern().let { fqName ->
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
    targetDeclarationProducer = CirClassConstructorFactory::create,
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
    targetDeclarationProducer = CirTypeAliasFactory::create,
    commonValueProducer = { commonize(it, TypeAliasCommonizer(cacheRW)) },
    recursionMarker = CirClassifierRecursionMarker,
    nodeProducer = ::CirTypeAliasNode
).also { node ->
    typeAliases.firstNonNull().fqNameSafe.intern().let { fqName ->
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
