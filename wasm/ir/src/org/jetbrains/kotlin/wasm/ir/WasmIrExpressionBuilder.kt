/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

class WasmIrExpressionBuilder(
    val expression: MutableList<WasmInstr>
) : WasmExpressionBuilder {
    
    override fun buildUnary(operator: WasmUnaryOp) {
        expression.add(WasmUnaryInstr(operator))
    }

    override fun buildBinary(operator: WasmBinaryOp) {
        expression.add(WasmBinaryInstr(operator))
    }

    override fun buildConstI32(value: Int) {
        expression.add(WasmConstInstr.I32(value))
    }

    override fun buildConstI64(value: Long) {
        expression.add(WasmConstInstr.I64(value))
    }

    override fun buildConstF32(value: Float) {
        expression.add(WasmConstInstr.F32(value))
    }

    override fun buildConstF64(value: Double) {
        expression.add(WasmConstInstr.F64(value))
    }

    override fun buildF32NaN() {
        expression.add(WasmConstInstr.F32NaN)
    }

    override fun buildF64NaN() {
        expression.add(WasmConstInstr.F64NaN)
    }

    override fun buildConstI32Symbol(value: WasmSymbol<Int>) {
        expression.add(WasmConstInstr.I32Symbol(value))
    }

    override fun buildLoad(operator: WasmLoadOp, memoryArgument: WasmMemoryArgument) {
        expression.add(WasmLoad(operator, memoryArgument))
    }

    override fun buildStore(operator: WasmStoreOp, memoryArgument: WasmMemoryArgument) {
        expression.add(WasmStore(operator, memoryArgument))
    }

    override fun buildUnreachable() {
        val last = expression.last()
        if (last != WasmUnreachable && last != WasmReturn) {
            expression.add(WasmUnreachable)
        }
    }

    override fun buildNop() {
        expression.add(WasmNop)
    }

    override fun buildBlock(label: String?, resultType: WasmValueType?) {
        numberOfNestedBlocks++
        expression.add(WasmBlock(label, resultType))
    }

    override fun buildLoop(label: String?, resultType: WasmValueType?) {
        numberOfNestedBlocks++
        expression.add(WasmLoop(label, resultType))
    }

    override fun buildIf(label: String?, resultType: WasmValueType?) {
        numberOfNestedBlocks++
        expression.add(WasmIf(label, resultType))
    }

    override fun buildElse() {
        expression.add(WasmElse)
    }

    override fun buildEnd() {
        numberOfNestedBlocks--
        expression.add(WasmEnd)
    }

    override fun buildBr(absoluteBlockLevel: Int) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        expression.add(WasmBr(relativeLevel))
    }

    override fun buildBrIf(absoluteBlockLevel: Int) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        expression.add(WasmBrIf(relativeLevel))
    }

    override fun buildReturn() {
        expression.add(WasmReturn)
    }

    override fun buildCall(symbol: WasmSymbol<WasmFunction>, type: WasmValueType?) {
        expression.add(WasmCall(symbol, type))
    }

    override fun buildCallIndirect(symbol: WasmSymbol<WasmFunctionType>, type: WasmValueType?) {
        expression.add(WasmCallIndirect(symbol, type))
    }

    override fun buildDrop() {
        expression.add(WasmDrop())
    }

    override fun buildSelect() {
        TODO("Not yet implemented")
    }

    override fun buildGetLocal(local: WasmLocal) {
        expression.add(WasmGetLocal(local))
    }

    override fun buildSetLocal(local: WasmLocal) {
        expression.add(WasmSetLocal(local))
    }

    override fun buildTeeLocal(local: WasmLocal) {
        expression.add(WasmLocalTee(local))
    }

    override fun buildGetGlobal(global: WasmSymbol<WasmGlobal>, type: WasmValueType) {
        expression.add(WasmGetGlobal(global, type))
    }

    override fun buildSetGlobal(global: WasmSymbol<WasmGlobal>) {
        expression.add(WasmSetGlobal(global))
    }

    override fun buildStructGet(structType: WasmSymbol<WasmStructType>, fieldId: WasmSymbol<Int>, fieldType: WasmValueType) {
        expression.add(WasmStructGet(structType, fieldId, fieldType))
    }

    override fun buildStructNew(structType: WasmSymbol<WasmStructType>) {
        expression.add(WasmStructNew(structType))
    }

    override fun buildStructSet(structType: WasmSymbol<WasmStructType>, fieldId: WasmSymbol<Int>) {
        expression.add(WasmStructSet(structType, fieldId))
    }

    override fun buildStructNarrow(fromType: WasmValueType, toType: WasmValueType) {
        expression.add(WasmStructNarrow(fromType, toType))
    }

    override fun buildRefNull() {
        expression.add(WasmRefNull)
    }

    override fun buildRefIsNull() {
        expression.add(WasmRefIsNull())
    }

    override fun buildRefEq() {
        expression.add(WasmRefEq())
    }

    override var numberOfNestedBlocks: Int = 0
        set(value) {
            assert(value >= 0) { "end without matching block" }
            field = value
        }
}