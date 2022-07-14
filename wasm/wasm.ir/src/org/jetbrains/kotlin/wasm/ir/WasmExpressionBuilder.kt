/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

abstract class WasmExpressionBuilder {
    abstract fun buildInstr(op: WasmOp, vararg immediates: WasmImmediate)
    abstract var numberOfNestedBlocks: Int

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

    @Suppress("UNUSED_PARAMETER")
    inline fun buildBlock(label: String?, resultType: WasmType? = null, body: (Int) -> Unit) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.BLOCK, WasmImmediate.BlockType.Value(resultType))
        body(numberOfNestedBlocks)
        buildEnd()
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun buildLoop(label: String?, resultType: WasmType? = null, body: (Int) -> Unit) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.LOOP, WasmImmediate.BlockType.Value(resultType))
        body(numberOfNestedBlocks)
        buildEnd()
    }

    @Suppress("UNUSED_PARAMETER")
    fun buildIf(label: String?, resultType: WasmType? = null) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.IF, WasmImmediate.BlockType.Value(resultType))
    }

    fun buildElse() {
        buildInstr(WasmOp.ELSE)
    }

    fun buildBlock(resultType: WasmType? = null): Int {
        buildInstr(WasmOp.BLOCK, WasmImmediate.BlockType.Value(resultType))
        numberOfNestedBlocks++
        return numberOfNestedBlocks
    }

    fun buildEnd() {
        numberOfNestedBlocks--
        buildInstr(WasmOp.END)
    }


    fun buildBrInstr(brOp: WasmOp, absoluteBlockLevel: Int) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        buildInstr(brOp, WasmImmediate.LabelIdx(relativeLevel))
    }

    fun buildBrInstr(brOp: WasmOp, absoluteBlockLevel: Int, symbol: WasmSymbolReadOnly<WasmTypeDeclaration>) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        buildInstr(brOp, WasmImmediate.LabelIdx(relativeLevel), WasmImmediate.TypeIdx(symbol))
    }

    fun buildBr(absoluteBlockLevel: Int) {
        buildBrInstr(WasmOp.BR, absoluteBlockLevel)
    }

    fun buildThrow(tagIdx: Int) {
        buildInstr(WasmOp.THROW, WasmImmediate.TagIdx(tagIdx))
    }

    @Suppress("UNUSED_PARAMETER")
    fun buildTry(label: String?, resultType: WasmType? = null) {
        numberOfNestedBlocks++
        buildInstr(WasmOp.TRY, WasmImmediate.BlockType.Value(resultType))
    }

    fun buildCatch(tagIdx: Int) {
        buildInstr(WasmOp.CATCH, WasmImmediate.TagIdx(tagIdx))
    }

    fun buildBrIf(absoluteBlockLevel: Int) {
        buildBrInstr(WasmOp.BR_IF, absoluteBlockLevel)
    }

    fun buildCall(symbol: WasmSymbol<WasmFunction>) {
        buildInstr(WasmOp.CALL, WasmImmediate.FuncIdx(symbol))
    }

    fun buildCallIndirect(
        symbol: WasmSymbol<WasmFunctionType>,
        tableIdx: WasmSymbolReadOnly<Int> = WasmSymbol(0),
    ) {
        buildInstr(
            WasmOp.CALL_INDIRECT,
            WasmImmediate.TypeIdx(symbol),
            WasmImmediate.TableIdx(tableIdx)
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

    fun buildStructGet(struct: WasmSymbol<WasmTypeDeclaration>, fieldId: WasmSymbol<Int>) {
        buildInstr(
            WasmOp.STRUCT_GET,
            WasmImmediate.GcType(struct),
            WasmImmediate.StructFieldIdx(fieldId)
        )
    }

    fun buildStructNew(struct: WasmSymbol<WasmTypeDeclaration>) {
        buildInstr(WasmOp.STRUCT_NEW, WasmImmediate.GcType(struct))
    }

    fun buildStructSet(struct: WasmSymbol<WasmTypeDeclaration>, fieldId: WasmSymbol<Int>) {
        buildInstr(
            WasmOp.STRUCT_SET,
            WasmImmediate.GcType(struct),
            WasmImmediate.StructFieldIdx(fieldId)
        )
    }

    fun buildRefCastStatic(toType: WasmSymbolReadOnly<WasmTypeDeclaration>) {
        buildInstr(WasmOp.REF_CAST_STATIC, WasmImmediate.TypeIdx(toType))
    }

    fun buildRefTestStatic(toType: WasmSymbolReadOnly<WasmTypeDeclaration>) {
        buildInstr(WasmOp.REF_TEST_STATIC, WasmImmediate.TypeIdx(toType))
    }

    fun buildRefNull(type: WasmHeapType) {
        buildInstr(WasmOp.REF_NULL, WasmImmediate.HeapType(WasmRefType(type)))
    }

    fun buildDrop() {
        buildInstr(WasmOp.DROP)
    }
}

