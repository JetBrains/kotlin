/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.LLVMKotlinBuildShadowStack
import llvm.LLVMModuleRef

internal class BuildShadowStackPass {
    fun runOnModule(module: LLVMModuleRef, clearDeadSlots: Boolean) {
        LLVMKotlinBuildShadowStack(module, if (clearDeadSlots) 1 else 0)
    }
}
