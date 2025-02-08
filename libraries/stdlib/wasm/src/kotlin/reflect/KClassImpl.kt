/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

import kotlin.reflect.KClass

internal class KClassImpl<T : Any> @WasmPrimitiveConstructor constructor(internal val rtti: kotlin.wasm.internal.reftypes.structref) : KClass<T> {
    override val simpleName: String get() = getSimpleName()
    override val qualifiedName: String
        get() {
            val typeName = getSimpleName()
            val packageName = getPackageName()
            return if (packageName.isEmpty()) typeName else "$packageName.$typeName"
        }

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

    override fun hashCode(): Int = qualifiedName.hashCode()

    override fun toString(): String = "class $qualifiedName"

    private fun getPackageName(): String = stringLiteral(
        startAddress = wasmGetRttiIntField(2, rtti),
        length = wasmGetRttiIntField(3, rtti),
        poolId = wasmGetRttiIntField(4, rtti),
    )

    private fun getSimpleName(): String = stringLiteral(
        startAddress = wasmGetRttiIntField(5, rtti),
        length = wasmGetRttiIntField(6, rtti),
        poolId = wasmGetRttiIntField(7, rtti),
    )

    internal fun getTypeId(): Long = wasmGetRttiLongField(8, rtti)
}