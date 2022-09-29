/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.konan.TempFiles

internal data class ObjectFilesInput(
        val bitcodeFile: BitcodeFile,
        val tempFiles: TempFiles,
)

internal val ObjectFilesPhase = object : SimpleNamedCompilerPhase<PhaseContext, ObjectFilesInput, List<ObjectFile>>(
        "ObjectFiles", "Compile bitcode to object files"
) {
    override fun outputIfNotEnabled(context: PhaseContext, input: ObjectFilesInput): List<ObjectFile> {
        return emptyList()
    }

    override fun phaseBody(context: PhaseContext, input: ObjectFilesInput): List<ObjectFile> {
        return BitcodeCompiler(context, input.tempFiles).makeObjectFiles(input.bitcodeFile)
    }
}

internal data class LinkerPhaseInput(
        val objectFiles: List<ObjectFile>,
        val llvm: Llvm,
        val llvmModuleSpecification: LlvmModuleSpecification,
        val needsProfileLibrary: Boolean,
        val outputFile: String,
        val outputFiles: OutputFiles,
        val tempFiles: TempFiles,
)

internal val LinkerPhase = object : SimpleNamedCompilerPhase<PhaseContext, LinkerPhaseInput, Unit>(
        "Linker", "Object files linker"
) {
    override fun outputIfNotEnabled(context: PhaseContext, input: LinkerPhaseInput) {

    }

    override fun phaseBody(context: PhaseContext, input: LinkerPhaseInput) {
        Linker(context, input).link()
    }
}