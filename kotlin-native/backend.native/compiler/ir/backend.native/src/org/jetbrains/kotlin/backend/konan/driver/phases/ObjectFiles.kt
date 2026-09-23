/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.LlvmBasedBitcodeCompiler
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import java.io.File

internal data class ObjectFilesPhaseInput(
        val moduleToCompile: LLVMModuleRef,
        val objectFile: File,
)

internal val ObjectFilesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, ObjectFilesPhaseInput>(
        name = "ObjectFiles",
) { context, input ->
    // ClangBasedBitcodeCompiler(context).makeObjectFile(input.bitcodeFile, input.objectFile)
    LlvmBasedBitcodeCompiler(context).makeObjectFile(input.moduleToCompile, input.objectFile)
}
