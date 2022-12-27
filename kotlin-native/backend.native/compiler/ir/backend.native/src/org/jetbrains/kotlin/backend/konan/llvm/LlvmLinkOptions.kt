/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMAddNamedMetadataOperand

fun embedLlvmLinkOptions(llvmModuleCompilation: LlvmModuleCompilation, options: List<List<String>>) {
    val llvmContext = llvmModuleCompilation.llvmContext
    val module = llvmModuleCompilation.module
    options.forEach {
        val node = node(llvmContext, *it.map { it.mdString(llvmContext) }.toTypedArray())
        LLVMAddNamedMetadataOperand(module, "llvm.linker.options", node)
    }
}