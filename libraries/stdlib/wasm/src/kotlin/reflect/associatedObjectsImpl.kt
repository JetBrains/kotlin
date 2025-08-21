/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.KClass

@PublishedApi
internal fun findAssociatedObject(klass: KClass<*>, key: Long): Any? {
    val typeId = when (klass) {
        is KClassImpl<*> -> getTypeId(klass.rtti)
        is KClassInterfaceImpl<*> -> klass.typeData.typeId
        else -> return null
    }
    return tryGetAssociatedObject(typeId, key)
}

internal fun tryGetAssociatedObject(klassId: Long, keyId: Long): Any? {
    return moduleDescriptors.firstNotNullOfOrNull { moduleDescriptor ->
        callAssociatedObjectGetter(klassId, keyId, moduleDescriptor.associatedObjectGetter)
    }
}