/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.wasm.internal.KClassImpl
import kotlin.reflect.KClass

@PublishedApi
internal fun findAssociatedObject(klass: KClass<*>, key: Int): Any? {
    val klassId = (klass as? KClassImpl<*>)?.typeData?.typeId ?: return null
    return tryGetAssociatedObject(klassId, key)
}

internal fun tryGetAssociatedObject(
    @Suppress("UNUSED_PARAMETER") klassId: Int,
    @Suppress("UNUSED_PARAMETER") keyId: Int,
): Any? {
    // Init implicitly with AssociatedObjectsLowering:
    // if (C1.klassId == klassId) if (Key1.klassId == keyId) return OBJ1
    // if (C2.klassId == klassId) if (Key2.klassId == keyId) return OBJ2
    // ...
    return null
}

// Remove after bootstrap KT-65322
@Suppress("UNUSED_PARAMETER")
internal fun addAssociatedObject(
    mapToInit: MutableMap<ULong, Any>,
    klassId: Int,
    keyId: Int,
    instance: Any
): Unit = error("Remove after bootstrap")

// Remove after bootstrap KT-65322
internal fun initAssociatedObjects(): Unit = error("Remove after bootstrap")