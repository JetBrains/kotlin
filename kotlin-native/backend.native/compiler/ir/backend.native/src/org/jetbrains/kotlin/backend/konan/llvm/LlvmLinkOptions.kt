/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMAddNamedMetadataOperand
import llvm.LLVMModuleRef

fun embedLlvmLinkOptions(module: LLVMModuleRef, options: List<List<String>>) {
    options.forEach {
        val node = node(*it.map { it.mdString() }.toTypedArray())
        LLVMAddNamedMetadataOperand(module, "llvm.linker.options", node)
    }
}