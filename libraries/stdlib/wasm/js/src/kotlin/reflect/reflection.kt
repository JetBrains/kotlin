/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.*

@Suppress("UNUSED_PARAMETER")
private fun getConstructor(obj: JsAny): JsAny? =
    js("obj.constructor")

@Suppress("UNCHECKED_CAST")
internal actual fun <T : Any> getKClassForObject(obj: Any): KClass<T> {
    if (obj !is JsExternalBox) return KClassImpl(getTypeInfoTypeDataByPtr(obj.typeInfo))
    val jsConstructor = getConstructor(obj.ref) ?: error("JavaScript constructor is not defined")
    return KExternalClassImpl(jsConstructor)
}