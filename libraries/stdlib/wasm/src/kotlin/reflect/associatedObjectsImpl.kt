/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.KClass

private fun getIdForKClass(klass: KClass<*>): Long? = when (klass) {
    is KClassImpl<*> -> getTypeId(klass.rtti)
    is KClassInterfaceImpl<*> -> klass.typeData.typeId
    else -> null
}

@PublishedApi
internal fun findAssociatedObject(klass: KClass<*>, key: KClass<*>): Any? {
    val klassId = getIdForKClass(klass) ?: return null
    val keyId = getIdForKClass(key) ?: return null
    return tryGetAssociatedObject(klassId, keyId)
}

internal fun tryGetAssociatedObject(
    @Suppress("UNUSED_PARAMETER") klassId: Long,
    @Suppress("UNUSED_PARAMETER") keyId: Long,
): Any? {
    // Init implicitly with AssociatedObjectsLowering and WasmCompiledModuleFragment::createTryGetAssociatedObjectFunction:
    // if (C1.klassId == klassId) if (Key1.klassId == keyId) return OBJ1
    // if (C2.klassId == klassId) if (Key2.klassId == keyId) return OBJ2
    // ...
    return null
}
