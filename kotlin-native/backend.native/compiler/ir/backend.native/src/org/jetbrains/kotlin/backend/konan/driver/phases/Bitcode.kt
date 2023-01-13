/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.insertAliasToEntryPoint

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 *
 * TODO: Use explicit input (LLVMModule) and output (File)
 *  after static driver removal.
 */

internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase(
        "WriteBitcodeFile",
        "Write bitcode file",
        outputIfNotEnabled = { _, _, _, _ -> }
) { context: NativeGenerationState, _: Unit ->
    val output = context.tempFiles.nativeBinaryFileName
    context.bitcodeFileName = output
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context)
    LLVMWriteBitcodeToFile(context.llvm.module, output)
}