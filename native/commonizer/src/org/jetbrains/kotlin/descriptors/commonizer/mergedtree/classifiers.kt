/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirRootNode.ClassifiersCacheImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

internal fun mergeClasses(
    storageManager: StorageManager,
    cacheRW: ClassifiersCacheImpl,
    containingDeclarationCommon: NullableLazyValue<*>?,
    classes: List<ClassDescriptor?>
): CirClassNode {
    val node = buildClassNode(storageManager, cacheRW, containingDeclarationCommon, classes)

    val constructorsMap = CommonizedGroupMap<ConstructorApproximationKey, ClassConstructorDescriptor>(classes.size)
    val propertiesMap = CommonizedGroupMap<PropertyApproximationKey, PropertyDescriptor>(classes.size)
    val functionsMap = CommonizedGroupMap<FunctionApproximationKey, SimpleFunctionDescriptor>(classes.size)
    val classesMap = CommonizedGroupMap<Name, ClassDescriptor>(classes.size)

    classes.forEachIndexed { index, clazz ->
        clazz?.constructors?.forEach { constructorsMap[ConstructorApproximationKey(it)][index] = it }
        clazz?.unsubstitutedMemberScope?.collectMembers(
            PropertyCollector { propertiesMap[PropertyApproximationKey(it)][index] = it },
            FunctionCollector { functionsMap[FunctionApproximationKey(it)][index] = it },
            ClassCollector { classesMap[it.name.intern()][index] = it }
        )
    }

    for ((_, constructorsGroup) in constructorsMap) {
        node.constructors += mergeClassConstructors(storageManager, cacheRW, node.common, constructorsGroup.toList())
    }

    for ((_, propertiesGroup) in propertiesMap) {
        node.properties += mergeProperties(storageManager, cacheRW, node.common, propertiesGroup.toList())
    }

    for ((_, functionsGroup) in functionsMap) {
        node.functions += mergeFunctions(storageManager, cacheRW, node.common, functionsGroup.toList())
    }

    for ((_, classesGroup) in classesMap) {
        node.classes += mergeClasses(storageManager, cacheRW, node.common, classesGroup.toList())
    }

    return node
}

internal fun mergeTypeAliases(
    storageManager: StorageManager,
    cacheRW: ClassifiersCacheImpl,
    typeAliases: List<TypeAliasDescriptor?>
) = buildTypeAliasNode(storageManager, cacheRW, typeAliases)

