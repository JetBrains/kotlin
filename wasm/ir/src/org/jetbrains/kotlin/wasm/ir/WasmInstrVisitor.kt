/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

interface WasmInstrVisitor<out R, in D> {
    fun visitInstr(x: WasmInstr, data: D): R
    fun visitWasmInstrWithTypedOp(x: WasmInstrWithTypedOp, data: D): R = visitInstr(x, data)

    fun visitUnary(x: WasmUnaryInstr, data: D): R = visitWasmInstrWithTypedOp(x, data)
    fun visitBinary(x: WasmBinaryInstr, data: D): R = visitWasmInstrWithTypedOp(x, data)
    fun visitConstant(x: WasmConstInstr, data: D): R = visitWasmInstrWithTypedOp(x, data)
    fun visitLoad(x: WasmLoad, data: D): R = visitWasmInstrWithTypedOp(x, data)
    fun visitStore(x: WasmStore, data: D): R = visitWasmInstrWithTypedOp(x, data)

    fun visitBranchTarget(x: WasmBranchTarget, data: D): R = visitInstr(x, data)
    fun visitBlock(x: WasmBlock, data: D): R = visitBranchTarget(x, data)
    fun visitLoop(x: WasmLoop, data: D): R = visitBranchTarget(x, data)
    fun visitIf(x: WasmIf, data: D): R = visitBranchTarget(x, data)

    fun visitControlInstr(x: WasmControlInstr, data: D) = visitInstr(x, data)
    fun visitUnreachable(x: WasmUnreachable, data: D): R = visitControlInstr(x, data)
    fun visitNop(x: WasmNop, data: D): R = visitControlInstr(x, data)
    fun visitBr(x: WasmBr, data: D): R = visitControlInstr(x, data)
    fun visitBrIf(x: WasmBrIf, data: D): R = visitControlInstr(x, data)
    fun visitBrTable(x: WasmBrTable, data: D): R = visitControlInstr(x, data)
    fun visitReturn(x: WasmReturn, data: D): R = visitControlInstr(x, data)
    fun visitCall(x: WasmCall, data: D): R = visitControlInstr(x, data)
    fun visitCallIndirect(x: WasmCallIndirect, data: D): R = visitControlInstr(x, data)
    fun visitElse(x: WasmElse, data: D): R = visitControlInstr(x, data)
    fun visitEnd(x: WasmEnd, data: D): R = visitControlInstr(x, data)

    fun visitParametricInstr(x: WasmParametricInstr, data: D): R = visitInstr(x, data)
    fun visitDrop(x: WasmDrop, data: D): R = visitParametricInstr(x, data)
    fun visitSelect(x: WasmSelect, data: D): R = visitParametricInstr(x, data)

    fun visitVariableInstr(x: WasmVariableInstr, data: D) = visitInstr(x, data)
    fun visitGetLocal(x: WasmGetLocal, data: D): R = visitVariableInstr(x, data)
    fun visitSetLocal(x: WasmSetLocal, data: D): R = visitVariableInstr(x, data)
    fun visitLocalTee(x: WasmLocalTee, data: D): R = visitVariableInstr(x, data)
    fun visitGetGlobal(x: WasmGetGlobal, data: D): R = visitVariableInstr(x, data)
    fun visitSetGlobal(x: WasmSetGlobal, data: D): R = visitVariableInstr(x, data)

    fun visitStructBasedInstr(x: WasmStructBasedInstr, data: D): R = visitInstr(x, data)
    fun visitStructGet(x: WasmStructGet, data: D): R = visitStructBasedInstr(x, data)
    fun visitStructNew(x: WasmStructNew, data: D): R = visitStructBasedInstr(x, data)
    fun visitStructSet(x: WasmStructSet, data: D): R = visitStructBasedInstr(x, data)
    fun visitStructNarrow(x: WasmStructNarrow, data: D): R = visitStructBasedInstr(x, data)

    fun visitRefBasedWasmInstr(x: WasmRefBased, data: D): R = visitInstr(x, data)
    fun visitRefNull(x: WasmRefNull, data: D): R = visitRefBasedWasmInstr(x, data)
    fun visitRefIsNull(x: WasmRefIsNull, data: D): R = visitRefBasedWasmInstr(x, data)
    fun visitRefEq(x: WasmRefEq, data: D): R = visitRefBasedWasmInstr(x, data)
}
