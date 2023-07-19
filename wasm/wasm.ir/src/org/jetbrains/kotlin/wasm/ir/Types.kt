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

// TODO: Remove this type.
object WasmUnreachableType : WasmType("unreachable", -0x40)
object WasmI32 : WasmType("i32", -0x1)
object WasmI64 : WasmType("i64", -0x2)
object WasmF32 : WasmType("f32", -0x3)
object WasmF64 : WasmType("f64", -0x4)
object WasmV128 : WasmType("v128", -0x5)
object WasmI8 : WasmType("i8", -0x6)
object WasmI16 : WasmType("i16", -0x7)
object WasmFuncRef : WasmType("funcref", -0x10)
object WasmExternRef : WasmType("externref", -0x11)
object WasmAnyRef : WasmType("anyref", -0x12)
object WasmEqRef : WasmType("eqref", -0x13)
object WasmRefNullrefType : WasmType("nullref", -0x1b) // Shorthand for (ref null none)
object WasmRefNullExternrefType : WasmType("nullexternref", -0x17) // Shorthand for (ref null noextern)

data class WasmRefNullType(val heapType: WasmHeapType) : WasmType("ref null", -0x14)
data class WasmRefType(val heapType: WasmHeapType) : WasmType("ref", -0x15)

@Suppress("unused")
object WasmI31Ref : WasmType("i31ref", -0x16)

@Suppress("unused")
object WasmStructRef : WasmType("structref", -0x19)

sealed class WasmHeapType {
    data class Type(val type: WasmSymbolReadOnly<WasmTypeDeclaration>) : WasmHeapType() {
        override fun toString(): String {
            return "Type:$type"
        }
    }

    sealed class Simple(val name: String, val code: Byte) : WasmHeapType() {
        object Func : Simple("func", -0x10)
        object Extern : Simple("extern", -0x11)
        object Any : Simple("any", -0x12)
        object Eq : Simple("eq", -0x13)
        object Struct : Simple("struct", -0x19)
        object None : Simple("none", -0x1b)
        object NoExtern : Simple("noextern", -0x17)

        override fun toString(): String {
            return "Simple:$name(${code.toString(16)})"
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
        is WasmRefNullrefType -> WasmHeapType.Simple.None
        is WasmRefNullExternrefType -> WasmHeapType.Simple.NoExtern
        is WasmEqRef -> WasmHeapType.Simple.Eq
        is WasmAnyRef -> WasmHeapType.Simple.Any
        is WasmFuncRef -> WasmHeapType.Simple.Func
        is WasmExternRef -> WasmHeapType.Simple.Extern
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
