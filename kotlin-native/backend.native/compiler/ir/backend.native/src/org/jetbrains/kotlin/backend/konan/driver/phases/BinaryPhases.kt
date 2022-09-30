/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.konan.TempFiles

internal interface BinaryPhasesContext : PhaseContext {
    val tempFiles: TempFiles
}

internal class BinaryPhasesContextImpl(
        private val basicPhaseContext: BasicPhaseContext,
        override val tempFiles: TempFiles
) : BinaryPhasesContext, PhaseContext by basicPhaseContext {

    override fun dispose() {
        tempFiles.dispose()
    }
}

internal data class WriteBitcodeInput(val llvmModule: LLVMModuleRef)

internal val WriteBitcodePhase = object : SimpleNamedCompilerPhase<BinaryPhasesContext, WriteBitcodeInput, BitcodeFile>(
        "WriteBitcode", "Save LLVM module as bitcode file"
) {

    override fun outputIfNotEnabled(context: BinaryPhasesContext, input: WriteBitcodeInput): BitcodeFile {
        // TODO:
        error("Phase should not be disabled")
    }

    override fun phaseBody(context: BinaryPhasesContext, input: WriteBitcodeInput): BitcodeFile {
        val output = context.tempFiles.nativeBinaryFileName
        LLVMWriteBitcodeToFile(input.llvmModule, output)
        return output
    }
}

internal data class ObjectFilesInput(
        val bitcodeFile: BitcodeFile,
)

internal val ObjectFilesPhase = object : SimpleNamedCompilerPhase<BinaryPhasesContext, ObjectFilesInput, List<ObjectFile>>(
        "ObjectFiles", "Compile bitcode to object files"
) {
    override fun outputIfNotEnabled(context: BinaryPhasesContext, input: ObjectFilesInput): List<ObjectFile> {
        return emptyList()
    }

    override fun phaseBody(context: BinaryPhasesContext, input: ObjectFilesInput): List<ObjectFile> {
        return BitcodeCompiler(context).makeObjectFiles(input.bitcodeFile)
    }
}

internal data class LinkerPhaseInput(
        val objectFiles: List<ObjectFile>,
        val llvm: Llvm,
        val llvmModuleSpecification: LlvmModuleSpecification,
        val needsProfileLibrary: Boolean,
        val outputFile: String,
        val outputFiles: OutputFiles,
)

internal val LinkerPhase = object : SimpleNamedCompilerPhase<BinaryPhasesContext, LinkerPhaseInput, Unit>(
        "Linker", "Object files linker"
) {
    override fun outputIfNotEnabled(context: BinaryPhasesContext, input: LinkerPhaseInput) {

    }

    override fun phaseBody(context: BinaryPhasesContext, input: LinkerPhaseInput) {
        Linker(context, input).link()
    }
}

internal fun <T: BinaryPhasesContext> PhaseEngine<T>.writeBitcodeFile(
        module: LLVMModuleRef,
): BitcodeFile {
    val input = WriteBitcodeInput(module)
    return this.runPhase(context, WriteBitcodePhase, input)
}

internal fun <T: BinaryPhasesContext> PhaseEngine<T>.produceObjectFiles(
        bitcodeFile: BitcodeFile,
): List<ObjectFile> {
    val input = ObjectFilesInput(bitcodeFile)
    return this.runPhase(context, ObjectFilesPhase, input)
}

internal fun <T: BinaryPhasesContext> PhaseEngine<T>.linkObjectFiles(
        objectFiles: List<ObjectFile>,
        llvm: Llvm,
        llvmModuleSpecification: LlvmModuleSpecification,
        needsProfileLibrary: Boolean,
        outputFile: String,
        outputFiles: OutputFiles,
) {
    val input = LinkerPhaseInput(objectFiles, llvm, llvmModuleSpecification, needsProfileLibrary, outputFile, outputFiles)
    return this.runPhase(context, LinkerPhase, input)
}