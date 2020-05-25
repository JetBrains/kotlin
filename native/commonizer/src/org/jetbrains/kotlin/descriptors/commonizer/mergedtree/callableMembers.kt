/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

internal fun mergeProperties(
    storageManager: StorageManager,
    cache: CirClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    properties: List<PropertyDescriptor?>
) = buildPropertyNode(storageManager, cache, containingDeclarationCommon, properties)

internal fun mergeFunctions(
    storageManager: StorageManager,
    cache: CirClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    properties: List<SimpleFunctionDescriptor?>
) = buildFunctionNode(storageManager, cache, containingDeclarationCommon, properties)

internal fun mergeClassConstructors(
    storageManager: StorageManager,
    cache: CirClassifiersCache,
    containingDeclarationCommon: NullableLazyValue<*>?,
    constructors: List<ClassConstructorDescriptor?>
) = buildClassConstructorNode(storageManager, cache, containingDeclarationCommon, constructors)
