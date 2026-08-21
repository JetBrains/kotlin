/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.utils.DFS

private fun Sequence<LLVMValueRef>.setVisibilityTo(viz: LLVMVisibility, preserved: Set<LLVMValueRef>, f: (LLVMValueRef) -> Boolean) {
    this.filter(f).minus(preserved).forEach { LLVMSetVisibility(it, viz) }
}

/**
 * Applies hidden visibility to symbols similarly to LLVM's internalize pass:
 * it makes hidden the symbols that are made internal by internalize.
 */
fun LLVMModuleRef.makeSymbolsVisibilityHiddenLikeLlvmInternalizePass() {
    // Note: the implementation below generally follows InternalizePass::internalizeModule,
    // but omits some details for simplicity.

    // TODO: LLVM handles some additional cases.
    val alwaysPreserved = getLlvmUsed(this)
    val gatheredValues = getFunctions(this) + getGlobals(this) + getGlobalAliases(this)

    gatheredValues.setVisibilityTo(LLVMVisibility.LLVMHiddenVisibility, alwaysPreserved) {
        val hasTargetLinkage = when (LLVMGetLinkage(it)) {
            LLVMLinkage.LLVMInternalLinkage, LLVMLinkage.LLVMPrivateLinkage -> false
            else -> true
        }
        hasTargetLinkage && it.isDefinition
    }
}

fun LLVMModuleRef.makeSymbolsVisibilityDefault() {
    val alwaysPreserved = getLlvmUsed(this)
    val gatheredValues = getFunctions(this) + getGlobals(this) + getGlobalAliases(this)
    gatheredValues.setVisibilityTo(LLVMVisibility.LLVMDefaultVisibility, alwaysPreserved) {
        it.isDefinition
    }
}

private fun getLlvmUsed(module: LLVMModuleRef): Set<LLVMValueRef> {
    val llvmUsed = LLVMGetNamedGlobal(module, "llvm.used") ?: return emptySet()
    val llvmUsedValue = LLVMGetInitializer(llvmUsed) ?: return emptySet()

    // Note: llvm.used value is an array of globals, wrapped into bitcasts, GEPs and other instructions;
    // see llvm::collectUsedGlobalVariables.
    // Conservatively extract all involved globals for simplicity:
    return DFS.dfs(
            /* nodes = */ listOf(llvmUsedValue),
            /* neighbors = */ { value -> getOperands(value) },
            object : DFS.CollectingNodeHandler<LLVMValueRef, LLVMValueRef, MutableSet<LLVMValueRef>>(mutableSetOf()) {
                override fun beforeChildren(current: LLVMValueRef): Boolean = when (LLVMGetValueKind(current)) {
                    LLVMValueKind.LLVMGlobalAliasValueKind,
                    LLVMValueKind.LLVMGlobalVariableValueKind,
                    LLVMValueKind.LLVMFunctionValueKind -> {
                        result.add(current)
                        false // Skip children.
                    }

                    else -> true
                }
            }
    )
}
