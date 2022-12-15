/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

private fun WasmOp.isOutCfgNode() = when (this) {
    WasmOp.UNREACHABLE, WasmOp.RETURN, WasmOp.THROW, WasmOp.RETHROW, WasmOp.BR, WasmOp.BR_TABLE -> true
    else -> false
}

private fun WasmOp.isInCfgNode() = when (this) {
    WasmOp.ELSE, WasmOp.CATCH, WasmOp.CATCH_ALL -> true
    else -> false
}

private fun WasmOp.pureStacklessInstruction() = when (this) {
    WasmOp.GET_UNIT, WasmOp.REF_NULL, WasmOp.I32_CONST, WasmOp.I64_CONST, WasmOp.F32_CONST, WasmOp.F64_CONST, WasmOp.LOCAL_GET, WasmOp.GLOBAL_GET -> true
    else -> false
}

/**
 * Read comments in [WasmExpressionBuilder]
 * TODO merge into [WasmExpressionBuilder]
 */
class WasmIrExpressionBuilder(
    val expression: MutableList<WasmInstr>
) : WasmExpressionBuilder() {

    private val lastInstr: WasmInstr?
        get() = expression.lastOrNull()
    private var eatEverythingUntilLevel: Int? = null

    private fun addInstruction(op: WasmOp, location: SourceLocation, immediates: Array<out WasmImmediate>) {
        val newInstruction = WasmInstrWithLocation(op, immediates.toList(), location)
        expression.add(newInstruction)
    }

    private fun getCurrentEatLevel(op: WasmOp): Int? {
        val eatLevel = eatEverythingUntilLevel ?: return null
        if (numberOfNestedBlocks == eatLevel && op.isInCfgNode()) {
            eatEverythingUntilLevel = null
            return null
        }
        if (numberOfNestedBlocks < eatLevel) {
            eatEverythingUntilLevel = null
            return null
        }
        return eatLevel
    }

    override fun buildInstr(op: WasmOp, location: SourceLocation, vararg immediates: WasmImmediate) {
        val currentEatUntil = getCurrentEatLevel(op)
        if (currentEatUntil != null) {
            if (currentEatUntil <= numberOfNestedBlocks) return
        } else {
            if (op.isOutCfgNode()) {
                eatEverythingUntilLevel = numberOfNestedBlocks
                addInstruction(op, location, immediates)
                return
            }
        }

        val lastInstruction = lastInstr
        if (lastInstruction == null) {
            addInstruction(op, location, immediates)
            return
        }
        val lastOperator = lastInstruction.operator

        // droppable instructions + drop/unreachable -> nothing
        if ((op == WasmOp.DROP || op == WasmOp.UNREACHABLE) && lastOperator.pureStacklessInstruction()) {
            expression.removeLast()
            return
        }

        // local set and local get for the same argument -> local tee
        if (lastOperator == WasmOp.LOCAL_SET && op == WasmOp.LOCAL_GET) {
            val localSetNumber = (lastInstruction.immediates.firstOrNull() as? WasmImmediate.LocalIdx)?.value
            if (localSetNumber != null) {
                val localGetNumber = (immediates.firstOrNull() as? WasmImmediate.LocalIdx)?.value
                if (localGetNumber == localSetNumber) {
                    expression.removeLast()
                    addInstruction(WasmOp.LOCAL_TEE, location, immediates)
                    return
                }
            }
        }

        addInstruction(op, location, immediates)
    }

    override var numberOfNestedBlocks: Int = 0
        set(value) {
            assert(value >= 0) { "end without matching block" }
            field = value
        }
}

inline fun buildWasmExpression(body: WasmExpressionBuilder.() -> Unit): MutableList<WasmInstr> {
    val res = mutableListOf<WasmInstr>()
    WasmIrExpressionBuilder(res).body()
    return res
}
