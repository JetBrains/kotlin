/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroupMap
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
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

    packageMemberScopes.forEachIndexed { index, memberScope ->
        memberScope?.collectProperties { propertyKey, property ->
            propertiesMap[propertyKey][index] = property
        }
    }

    for ((_, propertiesGroup) in propertiesMap) {
        node.properties += mergeProperties(propertiesGroup.toList())
    }

    // FIXME: traverse the rest - functions, classes, typealiases

    return node
}

private fun MemberScope.collectProperties(collector: (PropertyKey, PropertyDescriptor) -> Unit) {
    getContributedDescriptors(DescriptorKindFilter.VARIABLES).asSequence()
        .filterIsInstance<PropertyDescriptor>()
        .forEach { property ->
            collector(PropertyKey(property), property)
        }
}

private data class PropertyKey(
    val name: Name,
    val extensionReceiverParameterFqName: FqName?
) {
    constructor(property: PropertyDescriptor) : this(
        property.name,
        property.extensionReceiverParameter?.run { type.constructor.declarationDescriptor!!.fqNameSafe }
    )
}
