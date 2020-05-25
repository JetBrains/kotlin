/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager

internal fun mergePackages(
    storageManager: StorageManager,
    cacheRW: CirRootNode.ClassifiersCacheImpl,
    moduleName: Name,
    packageFqName: FqName,
    packageMemberScopes: List<MemberScope?>
): CirPackageNode {
    val node = buildPackageNode(storageManager, moduleName, packageFqName, packageMemberScopes)

    val propertiesMap = CommonizedGroupMap<PropertyApproximationKey, PropertyDescriptor>(packageMemberScopes.size)
    val functionsMap = CommonizedGroupMap<FunctionApproximationKey, SimpleFunctionDescriptor>(packageMemberScopes.size)
    val classesMap = CommonizedGroupMap<Name, ClassDescriptor>(packageMemberScopes.size)
    val typeAliasesMap = CommonizedGroupMap<Name, TypeAliasDescriptor>(packageMemberScopes.size)

    packageMemberScopes.forEachIndexed { index, memberScope ->
        memberScope?.collectMembers(
            PropertyCollector { propertiesMap[PropertyApproximationKey(it)][index] = it },
            FunctionCollector { functionsMap[FunctionApproximationKey(it)][index] = it },
            ClassCollector { classesMap[it.name.intern()][index] = it },
            TypeAliasCollector { typeAliasesMap[it.name.intern()][index] = it }
        )
    }

    for ((_, propertiesGroup) in propertiesMap) {
        node.properties += mergeProperties(storageManager, cacheRW, null, propertiesGroup.toList())
    }

    for ((_, functionsGroup) in functionsMap) {
        node.functions += mergeFunctions(storageManager, cacheRW, null, functionsGroup.toList())
    }

    for ((_, classesGroup) in classesMap) {
        node.classes += mergeClasses(storageManager, cacheRW, null, classesGroup.toList())
    }

    for ((_, typeAliasesGroup) in typeAliasesMap) {
        node.typeAliases += mergeTypeAliases(storageManager, cacheRW, typeAliasesGroup.toList())
    }

    return node
}
