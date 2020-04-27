/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

interface WasmExpressionBuilder {
    fun buildUnary(operator: WasmUnaryOp)
    fun buildBinary(operator: WasmBinaryOp)

    fun buildConstI32(value: Int)
    fun buildConstI64(value: Long)
    fun buildConstF32(value: Float)
    fun buildConstF64(value: Double)
    fun buildF32NaN()
    fun buildF64NaN()

    fun buildConstI32Symbol(value: WasmSymbol<Int>)

    fun buildLoad(operator: WasmLoadOp, memoryArgument: WasmMemoryArgument)
    fun buildStore(operator: WasmStoreOp, memoryArgument: WasmMemoryArgument)

    fun buildUnreachable()
    fun buildNop()

    fun buildBlock(label: String?, resultType: WasmValueType? = null)
    fun buildLoop(label: String?, resultType: WasmValueType? = null)
    fun buildIf(label: String?, resultType: WasmValueType? = null)
    fun buildElse()
    fun buildEnd()
    fun buildBr(absoluteBlockLevel: Int)
    fun buildBrIf(absoluteBlockLevel: Int)
    fun buildReturn()
    fun buildCall(symbol: WasmSymbol<WasmFunction>, type: WasmValueType?)
    fun buildCallIndirect(symbol: WasmSymbol<WasmFunctionType>, type: WasmValueType?)
    fun buildDrop()
    fun buildSelect()

    fun buildGetLocal(local: WasmLocal)
    fun buildSetLocal(local: WasmLocal)
    fun buildTeeLocal(local: WasmLocal)

    fun buildGetGlobal(global: WasmSymbol<WasmGlobal>, type: WasmValueType)
    fun buildSetGlobal(global: WasmSymbol<WasmGlobal>)

    fun buildStructGet(structType: WasmSymbol<WasmStructType>, fieldId: WasmSymbol<Int>, fieldType: WasmValueType)
    fun buildStructNew(structType: WasmSymbol<WasmStructType>)
    fun buildStructSet(structType: WasmSymbol<WasmStructType>, fieldId: WasmSymbol<Int>)
    fun buildStructNarrow(fromType: WasmValueType, toType: WasmValueType)

    fun buildRefNull()
    fun buildRefIsNull()
    fun buildRefEq()

    val numberOfNestedBlocks: Int
}

