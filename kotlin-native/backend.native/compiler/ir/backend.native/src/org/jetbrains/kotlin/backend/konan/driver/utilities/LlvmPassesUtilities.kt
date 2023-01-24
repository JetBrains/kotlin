/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext

/**
 * Implementation of this interface in phase context, input or output
 * enables LLVM IR validation and dumping
 */
interface LlvmIrHolder {
    val llvmModule: LLVMModuleRef
}