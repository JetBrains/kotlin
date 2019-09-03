/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.fqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.descriptors.commonizer.ir.PackageNode
import org.jetbrains.kotlin.descriptors.commonizer.ir.buildPackageNode

internal fun mergePackages(
    packageFqName: FqName,
    packageMemberScopes: List<MemberScope?>
): PackageNode {
    val node = buildPackageNode(packageFqName, packageMemberScopes)

    val propertiesMap = CommonizedGroupMap<PropertyKey, PropertyDescriptor>(packageMemberScopes.size)
    val functionsMap = CommonizedGroupMap<FunctionKey, SimpleFunctionDescriptor>(packageMemberScopes.size)

    packageMemberScopes.forEachIndexed { index, memberScope ->
        memberScope?.collectProperties { propertyKey, property ->
            propertiesMap[propertyKey][index] = property
        }
        memberScope?.collectFunctions { functionKey, function ->
            functionsMap[functionKey][index] = function
        }
    }

    for ((_, propertiesGroup) in propertiesMap) {
        node.properties += mergeProperties(propertiesGroup.toList())
    }

    for ((_, functionsGroup) in functionsMap) {
        node.functions += mergeFunctions(functionsGroup.toList())
    }

    // FIXME: traverse the rest - classes, typealiases

    return node
}

internal data class PropertyKey(
    val name: Name,
    val extensionReceiverParameterFqName: FqName?
) {
    constructor(property: PropertyDescriptor) : this(
        property.name,
        property.extensionReceiverParameter?.type?.fqName
    )
}

internal fun MemberScope.collectProperties(collector: (PropertyKey, PropertyDescriptor) -> Unit) {
    getContributedDescriptors(DescriptorKindFilter.VARIABLES).asSequence()
        .filterIsInstance<PropertyDescriptor>()
        .forEach { property ->
            collector(PropertyKey(property), property)
        }
}

internal data class FunctionKey(
    val name: Name,
    val valueParameters: List<Pair<Name, FqName>>,
    val extensionReceiverParameterFqName: FqName?
) {
    constructor(function: SimpleFunctionDescriptor) : this(
        function.name,
        function.valueParameters.map { it.name to it.type.fqName },
        function.extensionReceiverParameter?.type?.fqName
    )
}

internal fun MemberScope.collectFunctions(collector: (FunctionKey, SimpleFunctionDescriptor) -> Unit) {
    getContributedDescriptors(DescriptorKindFilter.FUNCTIONS).asSequence()
        .filterIsInstance<SimpleFunctionDescriptor>()
        .forEach { function ->
            collector(FunctionKey(function), function)
        }
}
