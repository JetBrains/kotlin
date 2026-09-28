/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMModuleRef
import java.nio.file.Path

/**
 * Uses the in-process LLVM compiler to compile the bitcode contained in a LLVM module.
 */
internal class LlvmBitcodeCompiler : BitcodeCompiler<LLVMModuleRef> {
    override fun makeObjectFile(bitcodeContainer: LLVMModuleRef, outputFilePath: Path) {
        TODO("Not yet implemented")
    }
}