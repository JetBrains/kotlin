/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.utils.DFS

/**
 * Applies hidden visibility to symbols similarly to LLVM's internalize pass:
 * it makes hidden the symbols that are made internal by internalize.
 */
fun makeVisibilityHiddenLikeLlvmInternalizePass(module: LLVMModuleRef) {
    // Note: the implementation below generally follows InternalizePass::internalizeModule,
    // but omits some details for simplicity.

    // TODO: LLVM handles some additional cases.
    val alwaysPreserved = getLlvmUsed(module)

    (getFunctions(module) + getGlobals(module) + getGlobalAliases(module))
            .filter {
                when (LLVMGetLinkage(it)) {
                    LLVMLinkage.LLVMInternalLinkage, LLVMLinkage.LLVMPrivateLinkage -> false
                    else -> true
                }
            }
            .filter { LLVMIsDeclaration(it) == 0 }
            .minus(alwaysPreserved)
            .forEach {
                LLVMSetVisibility(it, LLVMVisibility.LLVMHiddenVisibility)
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
