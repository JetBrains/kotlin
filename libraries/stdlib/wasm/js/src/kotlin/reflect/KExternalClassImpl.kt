/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.*

@Suppress("UNUSED_PARAMETER")
private fun getJsClassName(jsKlass: JsAny): String? =
    js("jsKlass.name")

@Suppress("UNUSED_PARAMETER")
private fun instanceOf(ref: JsAny, jsKlass: JsAny): Boolean =
    js("ref instanceof jsKlass")

internal class KExternalClassImpl<T : Any> @WasmPrimitiveConstructor constructor(private val jsConstructor: JsAny) : KClass<T> {
    override val simpleName: String? get() = getJsClassName(jsConstructor)
    override val qualifiedName: String? get() = null

    override fun isInstance(value: Any?): Boolean =
        value is JsExternalBox && instanceOf(value.ref, jsConstructor)

    override fun equals(other: Any?): Boolean =
        other is KExternalClassImpl<*> && jsConstructor == other.jsConstructor

    override fun hashCode(): Int = simpleName.hashCode()

    override fun toString(): String = "class $simpleName"
}