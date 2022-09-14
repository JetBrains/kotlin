/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.ir

import org.jetbrains.kotlin.dispatcher.common.FqnUtils
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

private typealias EnumEntryMapping = Map<String, IrEnumEntry>

class EnumEntryProvider(storageManager: StorageManager) {
    private val enumEntryCache = storageManager.createCacheWithNotNullValues<IrType, EnumEntryMapping>()

    fun getEnumEntryByName(enumType: IrType, name: String): IrEnumEntry? {
        val mapping = enumEntryCache.computeIfAbsent(enumType) {
            val enumClass = enumType.getClass()
            require(enumClass != null)

            enumClass.declarations.filterIsInstance<IrEnumEntry>().associateBy { it.name.identifier }
        }
        return mapping[name]
    }
}

class DispatchedVisitorInfo(
    val basicNodeClass: IrClass,
    val enumType: IrType
)

class DispatchedVisitorInfoProvider(storageManager: StorageManager) {
     private val cache = storageManager.createCacheWithNotNullValues<IrClass, DispatchedVisitorInfo>()

    fun getInfo(dispatchedVisitor: IrClass): DispatchedVisitorInfo {
        return cache.computeIfAbsent(dispatchedVisitor) {
            val annotation = dispatchedVisitor.getAnnotation(FqnUtils.DispatchedVisitor.DISPATCHED_VISITOR_ANNOTATION_FQN)
            require(annotation != null)

            val basicNodeClass = getConstructorTypeArgument(annotation, 0)?.getClass()
            require(basicNodeClass != null)

            val basicNodeAnnotation = basicNodeClass.getAnnotation(FqnUtils.Kind.WITH_ABSTRACT_KIND_ANNOTATION_FQN)
            require(basicNodeAnnotation != null)

            val enumType = getConstructorTypeArgument(basicNodeAnnotation, 0)
            require(enumType != null)

            DispatchedVisitorInfo(basicNodeClass, enumType)
        }
    }
}

class DispatcherPluginContext {
    private val storageManager = LockBasedStorageManager("bla-bla-bla")
    val enumEntryProvider = EnumEntryProvider(storageManager)
    val dispatchedVisitorInfoProvider = DispatchedVisitorInfoProvider(storageManager)
}