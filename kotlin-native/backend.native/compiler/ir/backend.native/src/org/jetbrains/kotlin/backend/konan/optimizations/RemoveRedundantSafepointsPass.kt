/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.*
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.getBasicBlocks
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.getInstructions
import org.jetbrains.kotlin.backend.konan.logMultiple

/**
 * Removes all Kotlin_mm_safePointFunctionPrologue from basic block except the first one.
 * Also, if first basic block in function contains call to Kotlin_mm_safePointFunctionPrologue, all other calls would be removed.
 * Also, calls, which are not removed are inlined (except arm32 apple targets)
 */
internal class RemoveRedundantSafepointsPass(
        private val loggingContext: LoggingContext
) {
    var totalPrologueSafepointsCount = 0
    var removedPrologueSafepointsCount = 0

    fun runOnFunction(function: LLVMValueRef, isSafepointInliningAllowed: Boolean) {
        val firstBlock = LLVMGetFirstBasicBlock(function) ?: return
        val firstBlockHasSafepoint = getInstructions(firstBlock).any { isPrologueSafepointCallsite(it) }
        getBasicBlocks(function).forEach { bb ->
            val removeFirst = firstBlockHasSafepoint && bb != firstBlock
            val prologueSafepointCallsites = getInstructions(bb)
                    .filter { isPrologueSafepointCallsite(it) }
                    .toList()
            totalPrologueSafepointsCount += prologueSafepointCallsites.size
            prologueSafepointCallsites.drop(if (removeFirst) 0 else 1).forEach {
                LLVMInstructionEraseFromParent(it)
                removedPrologueSafepointsCount += 1
            }
            if (!removeFirst && isSafepointInliningAllowed) {
                prologueSafepointCallsites
                        .firstOrNull()
                        ?.apply {
                            if (LLVMIsDeclaration(LLVMGetCalledValue(this)) == 0) {
                                if (LLVMInlineCall(this) == 0) {
                                    loggingContext.logMultiple {
                                        +"Failed to Inline safepoint to ${function.name}"
                                        +llvm2string(function)
                                    }
                                }
                            }
                        }
            }
        }
    }

    private fun isPrologueSafepointCallsite(insn: LLVMValueRef): Boolean =
            (LLVMIsACallInst(insn) != null || LLVMIsAInvokeInst(insn) != null)
                    && LLVMGetCalledValue(insn)?.name == prologueSafepointFunctionName

    fun runOnModule(module: LLVMModuleRef, isSafepointInliningAllowed: Boolean) {
        totalPrologueSafepointsCount = 0
        removedPrologueSafepointsCount = 0
        getFunctions(module)
                .filterNot { LLVMIsDeclaration(it) == 1 }
                .forEach { runOnFunction(it, isSafepointInliningAllowed) }
        loggingContext.logMultiple {
               +"Total prologue safepoints: $totalPrologueSafepointsCount"
               +"Removed prologue safepoints: $removedPrologueSafepointsCount"
        }
    }

    companion object {
        private const val prologueSafepointFunctionName = "Kotlin_mm_safePointFunctionPrologue"
    }
}