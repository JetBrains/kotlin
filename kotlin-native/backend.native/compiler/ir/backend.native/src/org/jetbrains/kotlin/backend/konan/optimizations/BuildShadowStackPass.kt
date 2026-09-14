/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.LLVMKotlinBuildShadowStack
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.config.nativeBinaryOptions.ShadowStackSlotClearing

internal class BuildShadowStackPass {
    fun runOnModule(module: LLVMModuleRef, clearDeadSlots: ShadowStackSlotClearing) {
        LLVMKotlinBuildShadowStack(module, when (clearDeadSlots) {
            ShadowStackSlotClearing.OFF -> 0
            ShadowStackSlotClearing.EARLY -> 1
            ShadowStackSlotClearing.LATE -> 2
        })
    }
}
