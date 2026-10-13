/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.LlvmBackendContext
import org.jetbrains.kotlin.konan.TempFiles
import java.nio.file.Path

/**
 * Compiles an LLVM module into an object file.
 */
internal interface BitcodeCompiler {
    fun makeObjectFile(llvmModule: LLVMModuleRef, outputFilePath: Path)
}

/**
 * Chooses how object files are emitted: in-process with the target machine of [llvmBackendContext]
 * if [NativeSecondStageCompilationConfig.inProcessObjEmission] is set, with clang otherwise.
 *
 * The result borrows [llvmBackendContext] and [tempFiles], so it must not outlive them.
 */
internal fun NativeBackendPhaseContext.createBitcodeCompiler(
        llvmBackendContext: LlvmBackendContext,
        tempFiles: TempFiles,
        llvmModuleName: String,
): BitcodeCompiler {
    return if (config.inProcessObjEmission) {
        LlvmBitcodeCompiler(llvmBackendContext)
    } else {
        ClangBitcodeCompiler(this, tempFiles, llvmModuleName)
    }
}
