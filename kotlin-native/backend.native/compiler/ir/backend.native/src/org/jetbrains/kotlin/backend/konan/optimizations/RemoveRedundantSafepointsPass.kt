/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.*
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.llvm.getBasicBlocks
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.getInstructions
import org.jetbrains.kotlin.backend.konan.llvm.name

/**
 * Removes all Kotlin_mm_safePointFunctionPrologue from basic block except the first one.
 *
 * Currently, this pass is useful only for watchos_arm32, ios_arm32 targets because Kotlin_mm_* functions are marked
 * as noinline there.
 */
class RemoveRedundantSafepointsPass(
        private val loggingContext: LoggingContext
) {
    var totalPrologueSafepointsCount = 0
    var removedPrologueSafepointsCount = 0

    fun runOnFunction(function: LLVMValueRef) {
        getBasicBlocks(function).forEach { bb ->
            val unnecessaryPrologueSafepointCallsites = getInstructions(bb)
                    .filter { isPrologueSafepointCallsite(it) }
                    .onEach { totalPrologueSafepointsCount += 1 }
                    .drop(1)
                    .toList()
            unnecessaryPrologueSafepointCallsites.forEach {
                LLVMInstructionEraseFromParent(it)
                removedPrologueSafepointsCount += 1
            }
        }
    }

    private fun isPrologueSafepointCallsite(insn: LLVMValueRef): Boolean =
            (LLVMIsACallInst(insn) != null || LLVMIsAInvokeInst(insn) != null)
                    && LLVMGetCalledValue(insn)?.name == prologueSafepointFunctionName

    fun runOnModule(module: LLVMModuleRef) {
        totalPrologueSafepointsCount = 0
        removedPrologueSafepointsCount = 0
        getFunctions(module)
                .filter { it.name?.startsWith("kfun:") == true }
                .filterNot { LLVMIsDeclaration(it) == 1 }
                .forEach(this::runOnFunction)
        loggingContext.log {
            """
               Total prologue safepoints: $totalPrologueSafepointsCount
               Removed prologue safepoints: $removedPrologueSafepointsCount
            """.trimIndent()
        }
    }

    companion object {
        private const val prologueSafepointFunctionName = "Kotlin_mm_safePointFunctionPrologue"
    }
}