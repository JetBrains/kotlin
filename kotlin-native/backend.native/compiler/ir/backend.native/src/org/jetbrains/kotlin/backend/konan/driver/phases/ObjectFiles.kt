/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.BitcodeCompiler
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.LlvmIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultLlvmModuleActions
import java.nio.file.Path

internal class ObjectFilesInput(
        override val llvmModule: LLVMModuleRef,
        val bitcodeCompiler: BitcodeCompiler,
        val outputPath: Path,
) : LlvmIrHolder

/**
 * The [ObjectFilesPhase] compiles the given LLVM module into an object file.
 *
 * The LLVM IR is dumped and verified *before* the phase, since emitting the object file may modify the module.
 */
internal val ObjectFilesPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, ObjectFilesInput>(
        name = "ObjectFiles",
        preactions = getDefaultLlvmModuleActions(),
) { _, input ->
    input.bitcodeCompiler.makeObjectFile(input.llvmModule, input.outputPath)
}
