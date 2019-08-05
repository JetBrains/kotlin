/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.ir

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.core.Commonizer
import org.jetbrains.kotlin.descriptors.commonizer.core.PropertyCommonizer
import org.jetbrains.kotlin.descriptors.commonizer.firstNonNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal fun buildRootNode(targets: List<ConcreteTargetId>): RootNode = RootNode(
    targets.map { Root(it) },
    Root(CommonTargetId(targets.toSet()))
)

internal fun buildModuleNode(modules: List<ModuleDescriptor?>): ModuleNode = buildNode(
    modules,
    { Module(it.name) },
    { Module(it.firstNonNull().name) },
    ::ModuleNode
)

internal fun buildPackageNode(packageFqName: FqName, packageMemberScopes: List<MemberScope?>): PackageNode = buildNode(
    packageMemberScopes,
    { Package(packageFqName) },
    { Package(packageFqName) },
    ::PackageNode
)

internal fun buildPropertyNode(properties: List<PropertyDescriptor?>): PropertyNode = buildNode(
    properties,
    { TargetProperty(it) },
    { commonize(it, PropertyCommonizer()) },
    ::PropertyNode
)

private fun <T : Any, D : Declaration, N : Node<D>> buildNode(
    descriptors: List<T?>,
    targetDeclarationProducer: (T) -> D,
    commonDeclarationProducer: (List<T?>) -> D?,
    nodeProducer: (List<D?>, D?) -> N
): N {
    val target = CommonizedGroup<D>(descriptors.size)
    var canHaveCommon = descriptors.size > 1

    descriptors.forEachIndexed { index, descriptor ->
        if (descriptor != null)
            target[index] = targetDeclarationProducer(descriptor)
        else
            canHaveCommon = false
    }

    val common = if (canHaveCommon) commonDeclarationProducer(descriptors) else null

    return nodeProducer(target.toList(), common)
}

private fun <T : Any, R : Declaration> commonize(descriptors: List<T?>, commonizer: Commonizer<T, R>): R? {
    for (item in descriptors) {
        if (item == null || !commonizer.commonizeWith(item))
            return null
    }

    return commonizer.result
}
