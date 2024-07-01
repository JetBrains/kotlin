/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.*
import org.jetbrains.kotlin.backend.konan.BitcodePostProcessingContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.getBasicBlocks
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.getInstructions

private fun filterLoads(block: LLVMBasicBlockRef, variable: LLVMValueRef) = getInstructions(block)
        .mapNotNull { LLVMIsALoadInst(it) }
        .filter { inst ->
            LLVMGetOperand(inst, 0)?.let { LLVMIsAGlobalVariable(it) } == variable
        }

private fun process(function: LLVMValueRef, currentThreadTLV: LLVMValueRef) {
    val entry = LLVMGetEntryBasicBlock(function) ?: return
    val load = filterLoads(entry, currentThreadTLV).firstOrNull() ?: return
    getBasicBlocks(function)
            .flatMap { filterLoads(it, currentThreadTLV) }
            .filter { it != load }
            .toList() // to force evaluating of all sequences above, because removing something during iteration is bad idea
            .forEach {
                LLVMReplaceAllUsesWith(it, load)
                LLVMInstructionEraseFromParent(it)
            }
}

internal fun removeMultipleThreadDataLoads(context: BitcodePostProcessingContext) {
    val currentThreadTLV = context.llvm.runtimeAnnotationMap["current_thread_tlv"]?.singleOrNull() ?: return

    getFunctions(context.llvm.module)
            .filter { it.name?.startsWith("kfun:") == true }
            .filter { it.isDefinition() }
            .forEach { process(it, currentThreadTLV) }
}