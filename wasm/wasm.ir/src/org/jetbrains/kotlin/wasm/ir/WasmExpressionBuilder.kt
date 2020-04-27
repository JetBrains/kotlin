/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

interface WasmExpressionBuilder {
    fun buildInstr(op: WasmOp, vararg immediates: WasmImmediate)
    var numberOfNestedBlocks: Int

    fun buildConstI32(value: Int) {
        buildInstr(WasmOp.I32_CONST, WasmImmediate.ConstI32(value))
    }

    fun buildConstI64(value: Long) {
        buildInstr(WasmOp.I64_CONST, WasmImmediate.ConstI64(value))
    }

    fun buildConstF32(value: Float) {
        buildInstr(WasmOp.F32_CONST, WasmImmediate.ConstF32(value.toRawBits().toUInt()))
    }

    fun buildConstF64(value: Double) {
        buildInstr(WasmOp.F64_CONST, WasmImmediate.ConstF64(value.toRawBits().toULong()))
    }

    fun buildConstI32Symbol(value: WasmSymbol<Int>) {
        buildInstr(WasmOp.I32_CONST, WasmImmediate.SymbolI32(value))
    }

    fun buildUnreachable() {
        buildInstr(WasmOp.UNREACHABLE)
    }

    fun buildBlock(label: String?, resultType: WasmType? = null) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.BLOCK, WasmImmediate.BlockType.Value(resultType))
    }

    fun buildLoop(label: String?, resultType: WasmType? = null) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.LOOP, WasmImmediate.BlockType.Value(resultType))
    }

    fun buildIf(label: String?, resultType: WasmType? = null) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.IF, WasmImmediate.BlockType.Value(resultType))
    }

    fun buildElse() {
        buildInstr(WasmOp.ELSE)
    }

    fun buildEnd() {
        numberOfNestedBlocks--
        buildInstr(WasmOp.END)
    }

    fun buildBr(absoluteBlockLevel: Int) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        buildInstr(WasmOp.BR, WasmImmediate.LabelIdx(relativeLevel))
    }

    fun buildBrIf(absoluteBlockLevel: Int) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        buildInstr(WasmOp.BR_IF, WasmImmediate.LabelIdx(relativeLevel))
    }

    fun buildCall(symbol: WasmSymbol<WasmFunction>) {
        buildInstr(WasmOp.CALL, WasmImmediate.FuncIdx(symbol))
    }

    fun buildCallIndirect(symbol: WasmSymbol<WasmFunctionType>) {
        buildInstr(
            WasmOp.CALL_INDIRECT,
            WasmImmediate.TypeIdx(symbol),
            WasmImmediate.TableIdx(0)
        )
    }

    fun buildGetLocal(local: WasmLocal) {
        buildInstr(WasmOp.LOCAL_GET, WasmImmediate.LocalIdx(local))
    }

    fun buildSetLocal(local: WasmLocal) {
        buildInstr(WasmOp.LOCAL_SET, WasmImmediate.LocalIdx(local))
    }

    fun buildGetGlobal(global: WasmSymbol<WasmGlobal>) {
        buildInstr(WasmOp.GLOBAL_GET, WasmImmediate.GlobalIdx(global))
    }

    fun buildSetGlobal(global: WasmSymbol<WasmGlobal>) {
        buildInstr(WasmOp.GLOBAL_SET, WasmImmediate.GlobalIdx(global))
    }

    fun buildStructGet(struct: WasmSymbol<WasmStructDeclaration>, fieldId: WasmSymbol<Int>) {
        buildInstr(
            WasmOp.STRUCT_GET,
            WasmImmediate.StructType(struct),
            WasmImmediate.StructFieldIdx(fieldId)
        )
    }

    fun buildStructNew(struct: WasmSymbol<WasmStructDeclaration>) {
        buildInstr(WasmOp.STRUCT_NEW_WITH_RTT, WasmImmediate.StructType(struct))
    }

    fun buildStructSet(struct: WasmSymbol<WasmStructDeclaration>, fieldId: WasmSymbol<Int>) {
        buildInstr(
            WasmOp.STRUCT_SET,
            WasmImmediate.StructType(struct),
            WasmImmediate.StructFieldIdx(fieldId)
        )
    }


    fun buildRefCast(fromType: WasmType, toType: WasmType) {
        buildInstr(
            WasmOp.REF_CAST,
            WasmImmediate.HeapType(fromType.getHeapType()),
            WasmImmediate.HeapType(toType.getHeapType())
        )
    }

    fun buildRefNull(type: WasmHeapType) {
        buildInstr(WasmOp.REF_NULL, WasmImmediate.HeapType(WasmRefType(type)))
    }

    fun buildRttSub(heapType: WasmType) {
        buildInstr(WasmOp.RTT_SUB, WasmImmediate.HeapType(heapType))
    }

    fun buildRttCanon(heapType: WasmType) {
        buildInstr(WasmOp.RTT_CANON, WasmImmediate.HeapType(heapType))
    }
}

