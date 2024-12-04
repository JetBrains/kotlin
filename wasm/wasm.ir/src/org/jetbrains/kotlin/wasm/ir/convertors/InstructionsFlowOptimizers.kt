/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

private fun WasmOp.pureStacklessInstruction() = when (this) {
    WasmOp.REF_NULL, WasmOp.I32_CONST, WasmOp.I64_CONST, WasmOp.F32_CONST, WasmOp.F64_CONST, WasmOp.LOCAL_GET, WasmOp.GLOBAL_GET -> true
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

internal fun removeUnreachableInstructions(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    var eatEverythingUntilLevel: Int? = null
    var numberOfNestedBlocks = 0

    fun getCurrentEatLevel(op: WasmOp): Int? {
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

    return sequence {
        for (instruction in input) {
            val op = instruction.operator

            if (op.isBlockStart()) {
                numberOfNestedBlocks++
            } else if (op.isBlockEnd()) {
                numberOfNestedBlocks--
            }

            val currentEatUntil = getCurrentEatLevel(op)
            if (currentEatUntil != null) {
                if (currentEatUntil <= numberOfNestedBlocks) {
                    continue
                }
            } else {
                if (op.isOutCfgNode()) {
                    eatEverythingUntilLevel = numberOfNestedBlocks
                }
            }
            yield(instruction)
        }
    }

}

internal fun removeInstructionPriorUnreachable(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    val inputIterator = input.iterator()
    var firstInstruction: WasmInstr? = null

    return sequence {
        while (inputIterator.hasNext()) {
            val instruction = inputIterator.next()
            if (instruction.operator.opcode == WASM_OP_PSEUDO_OPCODE) {
                yield(instruction)
                continue
            }

            val first = firstInstruction

            if (first == null) {
                firstInstruction = instruction
                continue
            }

            if (instruction.operator == WasmOp.UNREACHABLE && (first.operator.pureStacklessInstruction() || first.operator == WasmOp.NOP)) {
                if (first.operator != WasmOp.NOP) {
                    val firstLocation = first.location as? SourceLocation.Location
                    if (firstLocation != null) {
                        //replace first instruction to NOP
                        yield(WasmInstrWithLocation(WasmOp.NOP, emptyList(), firstLocation))
                    }
                }
            } else {
                yield(first)
            }

            firstInstruction = instruction
        }

        firstInstruction?.let { yield(it) }
    }
}

internal fun removeInstructionPriorDrop(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    val inputIterator = input.iterator()
    var firstInstruction: WasmInstr? = null
    var secondInstruction: WasmInstr? = null

    return sequence {
        while (inputIterator.hasNext()) {
            val instruction = inputIterator.next()
            if (instruction.operator.opcode == WASM_OP_PSEUDO_OPCODE) {
                yield(instruction)
                continue
            }

            val first = firstInstruction
            val second = secondInstruction

            if (first == null) {
                firstInstruction = instruction
                continue
            }
            if (second == null) {
                secondInstruction = instruction
                continue
            }

            if (second.operator == WasmOp.DROP && first.operator.pureStacklessInstruction()) {
                val firstLocation = first.location as? SourceLocation.Location
                if (firstLocation != null) {
                    //replace first instruction
                    firstInstruction = WasmInstrWithLocation(WasmOp.NOP, emptyList(), firstLocation)
                    secondInstruction = instruction
                } else {
                    //eat both instructions
                    firstInstruction = instruction
                    secondInstruction = null
                }
            } else {
                yield(first)
                firstInstruction = second
                secondInstruction = instruction
            }
        }

        firstInstruction?.let { yield(it) }
        secondInstruction?.let { yield(it) }
    }
}

internal fun mergeSetAndGetIntoTee(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    val inputIterator = input.iterator()
    var firstInstruction: WasmInstr? = null

    return sequence {
        while (inputIterator.hasNext()) {
            val instruction = inputIterator.next()

            if (instruction.operator.opcode == WASM_OP_PSEUDO_OPCODE) {
                yield(instruction)
                continue
            }

            val first = firstInstruction

            if (first == null) {
                firstInstruction = instruction
                continue
            }

            if (first.operator == WasmOp.LOCAL_SET && instruction.operator == WasmOp.LOCAL_GET) {
                val setNumber = (first.immediates.firstOrNull() as? WasmImmediate.LocalIdx)?.value
                val getNumber = (instruction.immediates.firstOrNull() as? WasmImmediate.LocalIdx)?.value
                if (getNumber == setNumber) {
                    val location = instruction.location
                    firstInstruction = if (location != null) {
                        WasmInstrWithLocation(WasmOp.LOCAL_TEE, instruction.immediates, location)
                    } else {
                        WasmInstrWithoutLocation(WasmOp.LOCAL_TEE, instruction.immediates)
                    }
                    continue
                }
            }

            yield(first)
            firstInstruction = instruction
        }

        firstInstruction?.let { yield(it) }
    }
}