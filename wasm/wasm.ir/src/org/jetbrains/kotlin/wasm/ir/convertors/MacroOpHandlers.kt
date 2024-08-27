/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*

internal fun handleMacroIf(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    var skip = false
    var skipOnElse = false
    return sequence {
        for (instruction in input) {
            when (instruction.operator) {
                WasmOp.MACRO_IF -> {
                    check(!skip && !skipOnElse)
                    val ifParam = (instruction.immediates[0] as WasmImmediate.SymbolI32).value.owner
                    skip = ifParam == 0
                    skipOnElse = !skip
                }
                WasmOp.MACRO_ELSE -> {
                    skip = skipOnElse
                }
                WasmOp.MACRO_END_IF -> {
                    skip = false
                    skipOnElse = false
                }
                else -> {
                    if (!skip) {
                        yield(instruction)
                    }
                }
            }
        }
    }
}

internal fun handleMacroTable(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    var currentTable: Array<List<WasmInstr>?>? = null
    var currentTableRow: MutableList<WasmInstr>? = null

    return sequence {
        for (instruction in input) {
            when (instruction.operator) {
                WasmOp.MACRO_TABLE -> {
                    check(currentTable == null && currentTableRow == null)
                    val tableSize = (instruction.immediates[0] as WasmImmediate.SymbolI32).value.owner
                    currentTable = arrayOfNulls(tableSize)
                }
                WasmOp.MACRO_TABLE_INDEX -> {
                    val indexParam = (instruction.immediates[0] as WasmImmediate.SymbolI32).value.owner
                    currentTableRow = mutableListOf()
                    currentTable!![indexParam] = currentTableRow
                }
                WasmOp.MACRO_TABLE_END -> {
                    currentTable!!.forEach { instructions ->
                        if (instructions == null) {
                            yield(WasmInstrWithoutLocation(WasmOp.REF_NULL, listOf(WasmImmediate.HeapType(WasmRefNullrefType))))
                        } else {
                            yieldAll(instructions)
                        }
                    }
                    currentTableRow = null
                    currentTable = null
                }
                else -> {
                    val tableRow = currentTableRow
                    if (tableRow != null) {
                        tableRow.add(instruction)
                    } else {
                        yield(instruction)
                    }
                }
            }
        }
    }
}