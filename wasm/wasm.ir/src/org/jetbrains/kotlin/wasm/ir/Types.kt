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
object WasmI1 : WasmType("i32", -0x1)
object WasmI64 : WasmType("i64", -0x2)
object WasmF32 : WasmType("f32", -0x3)
object WasmF64 : WasmType("f64", -0x4)
object WasmV128 : WasmType("v128", -0x5)
object WasmI8 : WasmType("i8", -0x6)
object WasmI16 : WasmType("i8", -0x7)
object WasmFuncRef : WasmType("funcref", -0x10)
object WasmExternRef : WasmType("externref", -0x11)
object WasmAnyRef : WasmType("anyref", -0x12)
object WasmEqRef : WasmType("eqref", -0x13)

class WasmRefNullType(val heapType: WasmHeapType) : WasmType("ref null", -0x14)
class WasmRefType(val heapType: WasmHeapType) : WasmType("ref", -0x15)

@Suppress("unused")
object WasmI31Ref : WasmType("i31ref", -0x16)
class WasmRtt(val depth: Int, val heapType: WasmHeapType) : WasmType("rtt", -0x17)

@Suppress("unused")
object WasmExnRef : WasmType("exnref", -0x18)

sealed class WasmHeapType {
    class Type(val type: WasmSymbolReadOnly<WasmTypeDeclaration>) : WasmHeapType() {
        override fun toString(): String {
            return "Type:$type"
        }
    }

    sealed class Simple(val name: String, val code: Byte) : WasmHeapType() {
        object Func : Simple("func", -0x10)
        object Extern : Simple("extern", -0x11)
        object Eq : Simple("eq", -0x13)

        @Suppress("unused")
        object ExnH : Simple("exn", -0x18)

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
        is WasmEqRef -> WasmHeapType.Simple.Eq
        is WasmExternRef -> WasmHeapType.Simple.Extern
        is WasmFuncRef -> WasmHeapType.Simple.Func
        else -> error("Unknown heap type for type $this")
    }
