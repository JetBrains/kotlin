/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.reflect.KClass

@UsedFromCompilerGeneratedCode
internal class KClassImpl<T : Any> @WasmPrimitiveConstructor constructor(internal val rtti: kotlin.wasm.internal.reftypes.structref) : KClass<T> {
    override val simpleName: String?
        get() = if (isAnonymousClass(rtti)) null else getSimpleName(rtti)

    override val qualifiedName: String?
        get() = if (isAnonymousClass(rtti) || isLocalClass(rtti)) null else getQualifiedName(rtti)

    override fun isInstance(value: Any?): Boolean {
        if (value !is Any) return false

        val rtti = rtti
        var current: kotlin.wasm.internal.reftypes.structref? = wasmGetObjectRtti(value)
        while (current != null) {
            if (wasm_ref_eq(rtti, current)) return true
            current = wasmGetRttiSuperClass(current)
        }
        return false
    }

    override fun equals(other: Any?): Boolean =
        (other !== null) && ((this === other) || (other is KClassImpl<*> && wasm_ref_eq(rtti, other.rtti)))

    override fun hashCode(): Int = getQualifiedName(rtti).hashCode()

    override fun toString(): String = "class ${getQualifiedName(rtti)}"
}