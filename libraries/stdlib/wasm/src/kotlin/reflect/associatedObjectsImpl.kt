/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.KClass

private fun KClass<*>.getTypeId(): Long? = when (this) {
    is KClassImpl<*> -> getTypeId(rtti)
    is KClassInterfaceImpl<*> -> typeData.typeId
    else -> return null
}

@PublishedApi
internal fun findAssociatedObject(klass: KClass<*>, key: KClass<*>): Any? {
    val klassId = klass.getTypeId() ?: return null
    val keyId = key.getTypeId() ?: return null
    return tryGetAssociatedObjectWithWrapper(klassId, keyId)
}

// TODO: Should be removed after bootstrap
internal fun tryGetAssociatedObject(klassId: Long, keyId: Long): Any? = null

// TODO: Should be renamed after bootstrap
internal fun tryGetAssociatedObjectWithWrapper(klassId: Long, keyId: Long): Any? {
    return moduleDescriptors.firstNotNullOfOrNull { moduleDescriptor ->
        callAssociatedObjectGetter(klassId, keyId, moduleDescriptor.associatedObjectGetter)
    }
}