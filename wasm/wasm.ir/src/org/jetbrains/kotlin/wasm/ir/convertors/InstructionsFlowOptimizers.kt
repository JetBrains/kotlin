/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

private fun WasmOp.pureStacklessInstruction() = when (this) {
    WasmOp.REF_NULL, WasmOp.I32_CONST, WasmOp.I64_CONST, WasmOp.F32_CONST, WasmOp.F64_CONST, WasmOp.LOCAL_GET, WasmOp.GLOBAL_GET, WasmOp.CALL_PURE -> true
    else -> false
}

private fun WasmOp.isOutCfgNode() = when (this) {
    WasmOp.UNREACHABLE, WasmOp.RETURN, WasmOp.THROW, WasmOp.THROW_REF, WasmOp.RETHROW, WasmOp.BR, WasmOp.BR_TABLE -> true
    else -> false
}

private fun WasmOp.isInCfgNode() = when (this) {
    WasmOp.ELSE, WasmOp.CATCH, WasmOp.CATCH_ALL -> true
    else -> false
}

internal abstract class OptimizeFlow {
    abstract fun push(instruction: WasmInstr)
    abstract fun complete()
}

private abstract class OptimizeFlowBase(protected val output: OptimizeFlow) : OptimizeFlow() {
    final override fun complete() {
        flash()
        output.complete()
    }

    protected open fun flash() {}
}

private class RemoveUnreachableInstructions(output: OptimizeFlow) : OptimizeFlowBase(output) {
    private var eatEverythingUntilLevel: Int? = null
    private var numberOfNestedBlocks = 0

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

    override fun push(instruction: WasmInstr) {
        val op = instruction.operator

        if (op.isBlockStart()) {
            numberOfNestedBlocks++
        } else if (op.isBlockEnd()) {
            numberOfNestedBlocks--
        }

        val currentEatUntil = getCurrentEatLevel(op)
        if (currentEatUntil != null) {
            if (currentEatUntil <= numberOfNestedBlocks) {
                return
            }
        } else {
            if (op.isOutCfgNode()) {
                eatEverythingUntilLevel = numberOfNestedBlocks
            }
        }
        output.push(instruction)
    }
}

private class RemoveInstructionPriorUnreachable(output: OptimizeFlow) : OptimizeFlowBase(output) {
    private var firstInstruction: WasmInstr? = null

    override fun push(instruction: WasmInstr) {
        if (instruction.operator.opcode == WASM_OP_PSEUDO_OPCODE) {
            flash()
            output.push(instruction)
            return
        }

        val first = firstInstruction
        firstInstruction = instruction

        if (first == null) {
            return
        }

        if (instruction.operator == WasmOp.UNREACHABLE && (first.operator.pureStacklessInstruction() || first.operator == WasmOp.NOP)) {
            if (first.operator != WasmOp.NOP) {
                val firstLocation = first.location as? SourceLocation.DefinedLocation
                if (firstLocation != null) {
                    //replace first instruction to NOP
                    output.push(wasmInstrWithLocation(WasmOp.NOP, firstLocation))
                }
            }
        } else {
            output.push(first)
        }
    }

    override fun flash() {
        firstInstruction?.let {
            push(it)
            firstInstruction = null
        }
    }
}

private class RemoveInstructionPriorDrop(output: OptimizeFlow) : OptimizeFlowBase(output) {
    private var firstInstruction: WasmInstr? = null
    private var secondInstruction: WasmInstr? = null

    override fun push(instruction: WasmInstr) {
        if (instruction.operator.opcode == WASM_OP_PSEUDO_OPCODE) {
            flash()
            output.push(instruction)
            return
        }

        val first = firstInstruction
        val second = secondInstruction

        if (first == null) {
            firstInstruction = instruction
            return
        }
        if (second == null) {
            secondInstruction = instruction
            return
        }

        if (second.operator == WasmOp.DROP && first.operator.pureStacklessInstruction()) {
            val firstLocation = first.location as? SourceLocation.DefinedLocation
            if (firstLocation != null) {
                //replace first instruction
                firstInstruction = wasmInstrWithLocation(WasmOp.NOP, firstLocation)
                secondInstruction = instruction
            } else {
                //eat both instructions
                firstInstruction = instruction
                secondInstruction = null
            }
        } else {
            firstInstruction = second
            secondInstruction = instruction
            output.push(first)
        }
    }

    override fun flash() {
        firstInstruction?.let {
            output.push(it)
            firstInstruction = null
        }

        secondInstruction?.let {
            output.push(it)
            secondInstruction = null
        }
    }
}


private class MergeSetAndGetIntoTee(output: OptimizeFlow) : OptimizeFlowBase(output) {
    private var firstInstruction: WasmInstr? = null

    override fun push(instruction: WasmInstr) {
        if (instruction.operator.opcode == WASM_OP_PSEUDO_OPCODE) {
            flash()
            output.push(instruction)
            return
        }

        val first = firstInstruction

        if (first == null) {
            firstInstruction = instruction
            return
        }

        if (first.operator == WasmOp.LOCAL_SET && instruction.operator == WasmOp.LOCAL_GET) {
            check(first.immediatesCount == 1 && instruction.immediatesCount == 1)
            val firstImmediate = first.firstImmediateOrNull()
            val secondImmediate = instruction.firstImmediateOrNull()
            val setNumber = (firstImmediate as? WasmImmediate.LocalIdx)?.value
            val getNumber = (secondImmediate as? WasmImmediate.LocalIdx)?.value
            check(setNumber != null && getNumber != null)

            if (getNumber == setNumber) {
                val location = instruction.location
                firstInstruction = if (location != null) {
                    wasmInstrWithLocation(WasmOp.LOCAL_TEE, location, firstImmediate)
                } else {
                    wasmInstrWithoutLocation(WasmOp.LOCAL_TEE, firstImmediate)
                }
                return
            }
        }

        firstInstruction = instruction
        output.push(first)
    }

    override fun flash() {
        firstInstruction?.let {
            output.push(it)
            firstInstruction = null
        }
    }
}

internal fun createInstructionsFlow(output: OptimizeFlow): OptimizeFlow {
    val mergedWithTee = MergeSetAndGetIntoTee(output)
    val mergedWithUnreachable = RemoveInstructionPriorUnreachable(mergedWithTee)
    val mergedWithDrop = RemoveInstructionPriorDrop(mergedWithUnreachable)
    val removedUnreachableCode = RemoveUnreachableInstructions(mergedWithDrop)
    return removedUnreachableCode
}