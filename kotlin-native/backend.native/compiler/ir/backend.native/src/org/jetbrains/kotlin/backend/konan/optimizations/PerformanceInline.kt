/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.*
import org.jetbrains.kotlin.backend.konan.BitcodePostProcessingContext
import org.jetbrains.kotlin.backend.konan.llvm.getBasicBlocks
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.getInstructions

internal fun handleInlinePerfAnnot(context: BitcodePostProcessingContext) {
    if (!context.config.smallBinary) {
        val toInline = context.llvm.runtimeAnnotationMap["performance_inline"]?.toSet() ?: return

        getFunctions(context.llvm.module)
                .filterNot { LLVMIsDeclaration(it) == 1 }
                .forEach { inlineCallsTo(it, toInline) }
    }
}

private fun inlineCallsTo(function: LLVMValueRef, toInline: Set<LLVMValueRef>) {
    whileChanged { changed ->
        getBasicBlocks(function)
                .flatMap { collectCalls(it) }
                .filter {
                    val callee = LLVMGetCalledValue(it)
                    callee != null && LLVMIsDeclaration(callee) == 0 && callee in toInline
                }
                .toList() // to force evaluating of all sequences above, because removing something during iteration is bad idea
                .forEach {
                    changed()
                    LLVMInlineCall(it)
                }
    }
}

private fun collectCalls(block: LLVMBasicBlockRef) = getInstructions(block).filter {
    LLVMIsACallInst(it) != null || LLVMIsAInvokeInst(it) != null
}

private fun whileChanged(block: (() -> Unit) -> Unit) {
    var changed = true
    while (changed) {
        changed = false
        block { changed = true }
    }
}
