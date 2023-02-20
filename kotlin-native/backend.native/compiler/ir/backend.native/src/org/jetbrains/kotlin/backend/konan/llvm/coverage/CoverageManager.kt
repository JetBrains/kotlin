/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.llvm.coverage

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.target.supportsCodeCoverage
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * "Umbrella" class of all the of the code coverage related logic.
 */
internal class CoverageManager(val generationState: NativeGenerationState) {
    private val context = generationState.context
    private val config = generationState.config

    private val shouldCoverSources: Boolean =
            config.shouldCoverSources

    private val librariesToCover: Set<String> =
            config.resolve.coveredLibraries.map { it.libraryName }.toSet()

    private val llvmProfileFilenameGlobal = "__llvm_profile_filename"

    val enabled: Boolean =
            shouldCoverSources || librariesToCover.isNotEmpty()

    init {
        if (enabled && !checkRestrictions()) {
            generationState.reportCompilationError("Coverage is not supported for ${config.target}.")
        }
    }

    private fun checkRestrictions(): Boolean  {
        val isKindAllowed = config.produce.involvesBitcodeGeneration
        val target = config.target
        val isTargetAllowed = target.supportsCodeCoverage()
        return isKindAllowed && isTargetAllowed
    }

    private val filesRegionsInfo = mutableListOf<FileRegionInfo>()

    private fun getFunctionRegions(irFunction: IrFunction) =
            filesRegionsInfo.flatMap { it.functions }.firstOrNull { it.function == irFunction }

    private val coveredModules: Set<ModuleDescriptor> by lazy {
        val coveredSources = if (shouldCoverSources) {
            context.sourcesModules
        } else {
            emptySet()
        }
        val coveredLibs = context.irModules.filter { it.key in librariesToCover }.values
                .map { it.descriptor }.toSet()
        coveredLibs + coveredSources
    }

    private fun fileCoverageFilter(file: IrFile) =
            file.packageFragmentDescriptor.module in coveredModules

    /**
     * Walk [irModuleFragment] subtree and collect [FileRegionInfo] for files that are part of [coveredModules].
     */
    fun collectRegions(irModuleFragment: IrModuleFragment) {
        if (enabled) {
            val regions = CoverageRegionCollector(this::fileCoverageFilter).collectFunctionRegions(irModuleFragment)
            filesRegionsInfo += regions
        }
    }

    /**
     * @return [LLVMCoverageInstrumentation] instance if [irFunction] should be covered.
     */
    fun tryGetInstrumentation(irFunction: IrFunction?, callSitePlacer: (function: LLVMValueRef, args: List<LLVMValueRef>) -> Unit) =
            if (enabled && irFunction != null) {
                getFunctionRegions(irFunction)?.let { LLVMCoverageInstrumentation(generationState, it, callSitePlacer) }
            } else {
                null
            }

    /**
     * Add __llvm_coverage_mapping to the LLVM module.
     */
    fun writeRegionInfo() {
        if (enabled) {
            LLVMCoverageWriter(generationState, filesRegionsInfo).write()
        }
    }

    /**
     * Add passes that should be executed after main LLVM optimization pipeline.
     */
    fun addLateLlvmPasses(passManager: LLVMPassManagerRef, outputFileName: String) {
        if (enabled) {
            // It's a late pass since DCE can kill __llvm_profile_filename global.
            LLVMAddInstrProfPass(passManager, outputFileName)
        }
    }

    /**
     * Since we performing instruction profiling before internalization and global dce
     * __llvm_profile_filename need to be added to exported symbols.
     */
    fun addExportedSymbols(): List<String> =
        if (enabled) {
             listOf(llvmProfileFilenameGlobal)
        } else {
            emptyList()
        }

    companion object {
        fun isCoverageEnabled(config: KonanConfig): Boolean =
            config.shouldCoverSources && config.resolve.coveredLibraries.isNotEmpty()
    }
}

internal fun runCoveragePass(generationState: NativeGenerationState, outputFileName: String) {
    val passManager = LLVMCreatePassManager()!!
    LLVMKotlinAddTargetLibraryInfoWrapperPass(passManager, generationState.llvm.targetTriple)
    generationState.coverage.addLateLlvmPasses(passManager, outputFileName)
    LLVMRunPassManager(passManager, generationState.llvm.module)
    LLVMDisposePassManager(passManager)
}