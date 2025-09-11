/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

sealed class WasmType(
    val name: String,
    val code: Byte
) {
    override fun toString(): String = name
}

sealed class WasmReferenceType(name: String, code: Byte) : WasmType(name, code) {
    open fun makeSharedIfShareable(): WasmReferenceType {
        val simpleHeapType = (getHeapType() as? WasmHeapType.Simple) ?: return this
        return if (simpleHeapType.isShareable())
            WasmRefNullType(WasmHeapType.SharedSimple(simpleHeapType))
        else
            this
    }

    /**
     * If the condition is true and the referenced heap type is shareable, returns the reference to a shared version
     * of that heap type. Otherwise, returns this reference type.
     */
    fun maybeShared(condition: Boolean): WasmReferenceType =
        if (condition) makeSharedIfShareable() else this

}

// TODO: Remove this type.
object WasmUnreachableType : WasmType("unreachable", -0x40)
object WasmI32 : WasmType("i32", -0x1)
object WasmI64 : WasmType("i64", -0x2)
object WasmF32 : WasmType("f32", -0x3)
object WasmF64 : WasmType("f64", -0x4)
object WasmV128 : WasmType("v128", -0x5)
object WasmI8 : WasmType("i8", -0x8)
object WasmI16 : WasmType("i16", -0x9)
object WasmFuncRef : WasmReferenceType("funcref", -0x10)
object WasmExternRef : WasmReferenceType("externref", -0x11)
object WasmAnyRef : WasmReferenceType("anyref", -0x12)
object WasmEqRef : WasmReferenceType("eqref", -0x13)
object WasmRefNullrefType : WasmReferenceType("nullref", -0x0F) // Shorthand for (ref null none)
object WasmRefNullExternrefType : WasmReferenceType("nullexternref", -0x0E) // Shorthand for (ref null noextern)

object WasmExnRefType : WasmReferenceType("exnref", -0x17) // Shorthand for (ref null exn)
object WasmNullExnRefType : WasmReferenceType("nullexnref", -0x0c) // Shorthand for (ref null noexn)

data class WasmRefNullType(val heapType: WasmHeapType) : WasmReferenceType("ref null", -0x1D)

data class WasmRefType(val heapType: WasmHeapType) : WasmReferenceType("ref", -0x1C) {
    override fun makeSharedIfShareable(): WasmReferenceType =
        if (heapType is WasmHeapType.Simple)
            WasmRefType(WasmHeapType.SharedSimple(heapType))
        else this
}

object WasmI31Ref : WasmReferenceType("i31ref", -0x14)
object WasmStructRef : WasmReferenceType("structref", -0x15)
object WasmArrayRef : WasmReferenceType("arrayref", -0x16)

sealed class WasmHeapType {
    /**
     * Whether the referenced type may be "shared" in -Xwasm-use-shared-objects mode and V8 "minimal scope" implementation.
     */
    abstract fun isShareable(): Boolean

    data class Type(val type: WasmSymbolReadOnly<WasmTypeDeclaration>) : WasmHeapType() {
        override fun toString(): String {
            return "Type:$type"
        }

        override fun isShareable(): Boolean = true
    }

    sealed class Simple(val name: String, val code: Byte) : WasmHeapType() {
        object Func : Simple("func", -0x10)
        object Extern : Simple("extern", -0x11)
        object Any : Simple("any", -0x12)
        object Eq : Simple("eq", -0x13)
        object Struct : Simple("struct", -0x15)
        object Array : Simple("struct", -0x16)
        object None : Simple("none", -0x0F)
        object NoFunc : Simple("nofunc", -0x0D)
        object NoExtern : Simple("noextern", -0x0E)
        object I31 : Simple("i31", -0x14)

        fun maybeShared(condition: Boolean) =
            if (condition && isShareable()) SharedSimple(this) else this

        override fun isShareable(): Boolean = when (this) {
            // these types are already supported in V8 "minimal scope" implementation
            Any -> true
            Eq -> true
            Struct -> true
            Array -> true
            None -> true
            I31 -> true

            // externs are shareable, although some incoming JS objects are always non-shared (e.g. exceptions)
            Extern, NoExtern -> true

            // these types are not yet supported in V8 "minimal scope" implementation
            Func, NoFunc -> false
        }

        override fun toString(): String {
            return "Simple:$name(${code.toString(16)})"
        }
    }

    class SharedSimple(val type: Simple) : WasmHeapType() {
        override fun isShareable(): Boolean = true

        override fun toString(): String {
            return "Shared($type))"
        }

        companion object {
            val EXTERN = SharedSimple(Simple.Extern)
            val NO_EXTERN = SharedSimple(Simple.NoExtern)
        }
    }
}

sealed class WasmBlockType {
    class Function(val type: WasmFunctionType) : WasmBlockType()
    class Value(val type: WasmType) : WasmBlockType()
}

fun WasmType.getHeapType(): WasmHeapType =
    when (this) {
        is WasmRefType -> heapType
        is WasmRefNullType -> heapType
        WasmRefNullrefType -> WasmHeapType.Simple.None
        WasmRefNullExternrefType -> WasmHeapType.Simple.NoExtern
        WasmEqRef -> WasmHeapType.Simple.Eq
        WasmAnyRef -> WasmHeapType.Simple.Any
        WasmFuncRef -> WasmHeapType.Simple.Func
        WasmExternRef -> WasmHeapType.Simple.Extern
        WasmI31Ref -> WasmHeapType.Simple.I31
        WasmStructRef -> WasmHeapType.Simple.Struct
        WasmArrayRef -> WasmHeapType.Simple.Array
        else -> error("Unknown heap type for type $this")
    }

fun WasmFunctionType.referencesTypeDeclarations(): Boolean =
    parameterTypes.any { it.referencesTypeDeclaration() } or resultTypes.any { it.referencesTypeDeclaration() }

fun WasmType.referencesTypeDeclaration(): Boolean {
    val heapType = when (this) {
        is WasmRefNullType -> getHeapType()
        is WasmRefType -> getHeapType()
        else -> return false
    }
    return heapType is WasmHeapType.Type
}
