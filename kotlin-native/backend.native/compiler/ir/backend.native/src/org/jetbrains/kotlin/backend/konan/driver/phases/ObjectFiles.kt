/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.BitcodePostProcessingContext
import org.jetbrains.kotlin.backend.konan.ClangBitcodeCompiler
import org.jetbrains.kotlin.backend.konan.LlvmBitcodeCompiler
import java.nio.file.Path

internal sealed interface ObjectFilesPhaseInput {
    // Where the object file will be written into the file-system.
    val objectOutputPath: Path

    data class ClangBased(val bitcodePath: Path, override val objectOutputPath: Path) : ObjectFilesPhaseInput
    data class InProcess(val llvmModule: LLVMModuleRef, override val objectOutputPath: Path) : ObjectFilesPhaseInput
}

internal val ObjectFilesPhase = createSimpleNamedCompilerPhase<BitcodePostProcessingContext, ObjectFilesPhaseInput>(
        name = "ObjectFiles",
) { context, input ->
    when (input) {
        is ObjectFilesPhaseInput.ClangBased -> ClangBitcodeCompiler(context).makeObjectFile(input.bitcodePath, input.objectOutputPath)
        is ObjectFilesPhaseInput.InProcess -> LlvmBitcodeCompiler(context).makeObjectFile(input.llvmModule, input.objectOutputPath)
    }
}
