/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

class WasmMemoryArgument(val align: Int, val offset: Int)

/**
 * WebAssembly instruction
 * A combination of opcode and immediate arguments.
 */
sealed class WasmInstr {
    abstract val operator: WasmOp
    open val type: WasmValueType? = null
    abstract fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R
}

sealed class WasmInstrWithTypedOp : WasmInstr() {
    abstract override val operator: WasmTypedOp
    override val type: WasmValueType?
        get() = operator.type
}

class WasmUnaryInstr(
    override val operator: WasmUnaryOp
) : WasmInstrWithTypedOp() {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitUnary(this, data)
}

class WasmBinaryInstr(
    override val operator: WasmBinaryOp,
) : WasmInstrWithTypedOp() {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitBinary(this, data)
}

sealed class WasmConstInstr(
    override val operator: WasmConstantOp
) : WasmInstrWithTypedOp() {
    class I32(val value: Int) : WasmConstInstr(WasmConstantOp.I32_CONST)
    class I64(val value: Long) : WasmConstInstr(WasmConstantOp.I64_CONST)
    class F32(val value: Float) : WasmConstInstr(WasmConstantOp.F32_CONST)
    class F64(val value: Double) : WasmConstInstr(WasmConstantOp.F64_CONST)
    object F32NaN : WasmConstInstr(WasmConstantOp.F32_CONST)
    object F64NaN : WasmConstInstr(WasmConstantOp.F64_CONST)
    class I32Symbol(val value: WasmSymbol<Int>) : WasmConstInstr(WasmConstantOp.I32_CONST)

    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitConstant(this, data)
}

class WasmLoad(
    override val operator: WasmLoadOp,
    val memoryArgument: WasmMemoryArgument,
) : WasmInstrWithTypedOp() {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitLoad(this, data)
}

class WasmStore(
    override val operator: WasmStoreOp,
    val memoryArgument: WasmMemoryArgument,
) : WasmInstrWithTypedOp() {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitStore(this, data)
}


sealed class WasmBranchTarget(
    val label: String?,
    override val operator: WasmControlOp
) : WasmInstr()

class WasmBlock(
    label: String?,
    override val type: WasmValueType? = null,
) : WasmBranchTarget(label, WasmControlOp.BLOCK) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)
}

class WasmLoop(
    label: String?,
    override val type: WasmValueType? = null,
) : WasmBranchTarget(label, WasmControlOp.LOOP) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitLoop(this, data)
}

class WasmIf(
    label: String?,
    override val type: WasmValueType?,
) : WasmBranchTarget(label, WasmControlOp.IF) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitIf(this, data)
}

sealed class WasmControlInstr(
    override val operator: WasmControlOp
) : WasmInstr()


object WasmUnreachable : WasmControlInstr(WasmControlOp.UNREACHABLE) {
    override val type = WasmUnreachableType
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitUnreachable(this, data)
}

object WasmNop : WasmControlInstr(WasmControlOp.NOP) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitNop(this, data)
}


object WasmElse : WasmControlInstr(WasmControlOp.ELSE) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitElse(this, data)
}

object WasmEnd : WasmControlInstr(WasmControlOp.END) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitEnd(this, data)
}


class WasmBr(val target: Int) : WasmControlInstr(WasmControlOp.BR) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitBr(this, data)
}

class WasmBrIf(
    val target: Int
) : WasmControlInstr(WasmControlOp.BR_IF) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitBrIf(this, data)
}

class WasmBrTable(
    val index: WasmInstr,
    val targets: List<Int>
) : WasmControlInstr(WasmControlOp.BR_TABLE) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitBrTable(this, data)
}

object WasmReturn : WasmControlInstr(WasmControlOp.RETURN) {
    override val type = WasmUnreachableType
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitReturn(this, data)
}

class WasmCall(
    val symbol: WasmSymbol<WasmFunction>,
    override val type: WasmValueType?
) : WasmControlInstr(WasmControlOp.CALL) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)
}

class WasmCallIndirect(
    val symbol: WasmSymbol<WasmFunctionType>,
    override val type: WasmValueType?
) : WasmControlInstr(WasmControlOp.CALL_INDIRECT) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitCallIndirect(this, data)
}


sealed class WasmParametricInstr(
    override val operator: WasmParametricOp
) : WasmInstr()

class WasmDrop : WasmParametricInstr(WasmParametricOp.DROP) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitDrop(this, data)
}

class WasmSelect(
    override val type: WasmValueType
) : WasmParametricInstr(WasmParametricOp.SELECT) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitSelect(this, data)
}

sealed class WasmVariableInstr(
    override val operator: WasmVariableOp
) : WasmInstr()

class WasmGetLocal(
    val local: WasmLocal
) : WasmVariableInstr(WasmVariableOp.LOCAL_GET) {
    override val type = local.type
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitGetLocal(this, data)
}

class WasmSetLocal(
    val local: WasmLocal,
) : WasmVariableInstr(WasmVariableOp.LOCAL_SET) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitSetLocal(this, data)
}

class WasmLocalTee(
    val local: WasmLocal
) : WasmVariableInstr(WasmVariableOp.LOCAL_TEE) {
    override val type = local.type
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitLocalTee(this, data)
}

class WasmGetGlobal(
    val global: WasmSymbol<WasmGlobal>,
    override val type: WasmValueType
) : WasmVariableInstr(WasmVariableOp.GLOBAL_GET) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitGetGlobal(this, data)
}

class WasmSetGlobal(
    val global: WasmSymbol<WasmGlobal>,
) : WasmVariableInstr(WasmVariableOp.GLOBAL_SET) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitSetGlobal(this, data)
}


sealed class WasmStructBasedInstr(
    override val operator: WasmStructOp
) : WasmInstr()

class WasmStructGet(
    val structName: WasmSymbol<WasmStructType>,
    val fieldId: WasmSymbol<Int>,
    override val type: WasmValueType
) : WasmStructBasedInstr(WasmStructOp.STRUCT_GET) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitStructGet(this, data)
}

class WasmStructNew(
    val structName: WasmSymbol<WasmStructType>,
) : WasmStructBasedInstr(WasmStructOp.STRUCT_NEW) {
    override val type
        get() = WasmStructRef(structName)

    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitStructNew(this, data)
}

class WasmStructSet(
    val structName: WasmSymbol<WasmStructType>,
    val fieldId: WasmSymbol<Int>,
) : WasmStructBasedInstr(WasmStructOp.STRUCT_SET) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitStructSet(this, data)
}

class WasmStructNarrow(
    val fromType: WasmValueType,
    override val type: WasmValueType,
) : WasmStructBasedInstr(WasmStructOp.STRUCT_NARROW) {
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitStructNarrow(this, data)
}


sealed class WasmRefBased(
    override val operator: WasmRefOp
) : WasmInstr()

object WasmRefNull : WasmRefBased(WasmRefOp.REF_NULL) {
    override val type = WasmNullRefType
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitRefNull(this, data)
}

class WasmRefIsNull : WasmRefBased(WasmRefOp.REF_IS_NULL) {
    override val type = WasmI1
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitRefIsNull(this, data)
}

class WasmRefEq : WasmRefBased(WasmRefOp.REF_EQ) {
    override val type = WasmI1
    override fun <R, D> accept(visitor: WasmInstrVisitor<R, D>, data: D): R =
        visitor.visitRefEq(this, data)
}