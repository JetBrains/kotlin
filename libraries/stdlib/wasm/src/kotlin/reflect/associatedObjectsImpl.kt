/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.wasm.internal.KClassImpl
import kotlin.reflect.KClass

internal var associatedObjects: Map<ULong, Any>? = null

@PublishedApi
internal fun findAssociatedObject(klass: KClass<*>, key: Int): Any? {
    val klassId = (klass as? KClassImpl<*>)?.typeData?.typeId ?: return null

    val map = associatedObjects ?: run {
        val newMap: MutableMap<ULong, Any> = mutableMapOf()
        initAssociatedObjects(newMap)
        associatedObjects = newMap
        newMap
    }

    return map[packIntoULong(klassId, key)]
}

internal fun packIntoULong(a: Int, b: Int): ULong =
    (a.toUInt().toULong() shl Int.SIZE_BITS) or b.toUInt().toULong()

internal fun addAssociatedObject(
    mapToInit: MutableMap<ULong, Any>,
    klassId: Int,
    keyId: Int,
    instance: Any
) {
    mapToInit[packIntoULong(klassId, keyId)] = instance
}

internal fun initAssociatedObjects(@Suppress("UNUSED_PARAMETER") mapToInit: MutableMap<ULong, Any>) {
    // Init implicitly with AssociatedObjectsLowering
    // addAssociatedObject(mapToInit, ...)
}