/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.WasmInstr

internal fun processInstructionsFlow(input: Sequence<WasmInstr>): Sequence<WasmInstr> {
    val macroTableHandled = handleMacroTable(input)
    val macroIfHandled = handleMacroIf(macroTableHandled)
    val removedUnreachableCode = removeUnreachableInstructions(macroIfHandled)
    val mergedWithDrop = removeInstructionPriorDrop(removedUnreachableCode)
    val mergedWithUnreachable = removeInstructionPriorUnreachable(mergedWithDrop)
    val mergedWithTee = mergeSetAndGetIntoTee(mergedWithUnreachable)
    return mergedWithTee
}