/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMKotlinInitializeTargets
import llvm.loadLLVMStubs

/**
 * Initializes LLVM: loads the LLVM stubs and initializes the targets supported by Kotlin/Native.
 */
internal object LlvmEnvironment {

    /**
     * Loads the LLVM stubs and initializes the Kotlin/Native targets. Safe to call repeatedly and from several threads.
     *
     * Also disables LLVM's fast instruction selector for the whole process; this is never undone.
     * Throws if that can't be done (e.g. the option was removed in a newer LLVM).
     */
    fun init(konanHome: String?) {
        loadLLVMStubs(konanHome)
        LLVMKotlinInitializeTargets()
    }
}
