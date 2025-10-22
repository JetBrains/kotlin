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
    return tryGetAssociatedObject(klassId, keyId)
}

// the calls of this method are replaced to others depending on the compilation mode:
// - for "single-module", to calls of `tryGetAssociatedObject_singleModuleImpl()`
// - otherwise, to calls of the dynamically created module-local `_associatedObjectGetter()`
@ExcludedFromCodegen
internal fun tryGetAssociatedObject(klassId: Long, keyId: Long): Any? = implementedAsIntrinsic

internal fun tryGetAssociatedObject_singleModuleImpl(klassId: Long, keyId: Long): Any? {
    return moduleDescriptors.firstNotNullOfOrNull { moduleDescriptor ->
        callAssociatedObjectGetter(klassId, keyId, moduleDescriptor.associatedObjectGetter)
    }
}