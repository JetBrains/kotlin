/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

/**
 * Calculate declaration IDs of linked wasm module
 */
fun WasmModule.calculateIds() {
    fun List<WasmNamedModuleField>.calculateIds(startIndex: Int = 0) {
        for ((index, field) in this.withIndex()) {
            field.id = index + startIndex
        }
    }

    functionTypes.calculateIds()
    gcTypes.calculateIds(startIndex = functionTypes.size)
    importedFunctions.calculateIds()
    importedMemories.calculateIds()
    importedTables.calculateIds()
    importedGlobals.calculateIds()
    importedTags.calculateIds()
    elements.calculateIds()

    definedFunctions.calculateIds(startIndex = importedFunctions.size)
    globals.calculateIds(startIndex = importedGlobals.size)
    memories.calculateIds(startIndex = importedMemories.size)
    tables.calculateIds(startIndex = importedTables.size)
    tags.calculateIds(startIndex = importedTags.size)
}

// This is used to perform simple peephole-like optimizations from the WasmExpressionBuilder.
// Takes two adjacent instructions and returns an array with 0, 1 or 2 instructions to replace the original ones.
// Returns null if no action is required.
fun foldWasmInstructions(prev: WasmInstr?, next: WasmInstr): List<WasmInstr>? {
    if (prev == null)
        return null

    // Unreachable is not needed after another unreachable or return
    if (next.operator == WasmOp.UNREACHABLE && prev.operator in listOf(WasmOp.UNREACHABLE, WasmOp.RETURN))
        return listOf(prev)

    if (next.operator == WasmOp.DROP) {
        // simple pure instruction + drop -> nothing
        if (prev.operator == WasmOp.GET_UNIT || prev.operator == WasmOp.REF_NULL)
            return listOf()

        // return + drop -> return
        if (prev.operator == WasmOp.RETURN)
            return listOf(prev)
    }

    return null
}

