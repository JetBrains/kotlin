/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMCodeGenOptLevel
import llvm.LLVMCodeModel
import llvm.LLVMKotlinInstructionSelector
import llvm.LLVMRelocMode
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.konan.target.Architecture.ARM64
import org.jetbrains.kotlin.konan.target.KonanTarget

enum class LlvmOptimizationLevel(val value: Int) {
    NONE(0),
    DEFAULT(1),
    AGGRESSIVE(3)
}

enum class LlvmSizeLevel(val value: Int) {
    NONE(0),
    DEFAULT(1),
    AGGRESSIVE(2)
}

/**
 * Instruction selector used by the target machine for machine code generation.
 *
 * Only matters for target machines that emit object code. Note that if [GLOBAL_ISEL] is not available,
 * then [SELECTION_DAG] will be the fallback option.
 */
enum class LlvmInstructionSelector(val llvmValue: LLVMKotlinInstructionSelector) {
    SELECTION_DAG(LLVMKotlinInstructionSelector.LLVMKotlinInstructionSelectorSelectionDAG),
    GLOBAL_ISEL(LLVMKotlinInstructionSelector.LLVMKotlinInstructionSelectorGlobalISel),
}

/**
 * Resolved description of the target the LLVM backend generates code for.
 * Shared by the generated module, target-aware IR optimization and machine code generation.
 * Pure data: does not own an LLVM context, module or target machine.
 */
data class LlvmTargetSpec(
        val targetTriple: String,
        val cpuModel: String,
        val cpuFeatures: String,
        val relocMode: LLVMRelocMode,
        val codeModel: LLVMCodeModel = LLVMCodeModelDefault,
)

/**
 * [LlvmTargetSpec] plus optimization policy for one LLVM backend pipeline invocation.
 * Optimization policy may differ between invocations (e.g. runtime vs. output module),
 * while the target specification stays the same within a compilation.
 */
data class LLvmBackendConfig(
        val targetSpec: LlvmTargetSpec,
        val optimizationLevel: LlvmOptimizationLevel,
        val sizeLevel: LlvmSizeLevel,
        val codegenOptimizationLevel: LLVMCodeGenOptLevel,
        val instructionSelector: LlvmInstructionSelector,
)

private val NativeBackendPhaseContext.llvmCodeGenOptLevel: LLVMCodeGenOptLevel
    get() = when {
        shouldOptimize() -> LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive
        shouldContainDebugInfo() -> LLVMCodeGenOptLevel.LLVMCodeGenLevelNone
        else -> LLVMCodeGenOptLevel.LLVMCodeGenLevelDefault
    }

private val NativeBackendPhaseContext.llvmOptimizationLevel: LlvmOptimizationLevel
    get() = when {
        shouldOptimize() -> LlvmOptimizationLevel.AGGRESSIVE
        shouldContainDebugInfo() -> LlvmOptimizationLevel.NONE
        else -> LlvmOptimizationLevel.DEFAULT
    }

// Only optimize for size, when required by configuration. LlvmSizeLevel.AGGRESSIVE will translate to "Oz"
private val NativeBackendPhaseContext.llvmSizeLevel: LlvmSizeLevel
    get() = if (config.smallBinary) LlvmSizeLevel.AGGRESSIVE else LlvmSizeLevel.NONE

private val KonanTarget.supportsGlobalISel: Boolean
    get() = family.isAppleFamily && architecture == ARM64

/**
 * We select the `GlobalISel` only for Apple targets on ARM64.
 */
private val NativeBackendPhaseContext.llvmInstructionSelector: LlvmInstructionSelector
    get() = when {
        !shouldOptimize() && config.target.supportsGlobalISel -> LlvmInstructionSelector.GLOBAL_ISEL
        else -> LlvmInstructionSelector.SELECTION_DAG
    }

/**
 * Creates [LLvmBackendConfig] for the LLVM module that is compiled to the output artifact (e.g., program, framework, ...).
 */
internal fun createLlvmBackendConfigForOutputModuleFrom(context: NativeBackendPhaseContext): LLvmBackendConfig =
        LLvmBackendConfig(
                targetSpec = context.config.llvmTargetSpec,
                optimizationLevel = context.llvmOptimizationLevel,
                sizeLevel = context.llvmSizeLevel,
                codegenOptimizationLevel = context.llvmCodeGenOptLevel,
                instructionSelector = context.llvmInstructionSelector,
        )

/**
 * Creates [LLvmBackendConfig] for pre-optimizing the runtime. Only used with
 * [org.jetbrains.kotlin.config.nativeBinaryOptions.RuntimeLinkageStrategy.Optimize], the default for debug builds.
 *
 * The runtime's IR is optimized aggressively even in debug builds: its bitcode is already built with `-O2`
 * by the distribution, and the compilation mode is about the user's code.
 *
 * Note that this is independent of debug info in the runtime: the distribution may be built with
 * `kotlin.native.isNativeRuntimeDebugInfoEnabled=true` (adds `-gdwarf-2`), and the compiler keeps that debug info
 * when `-Xbinary=stripDebugInfoFromNativeLibs=false` is passed (see [shouldUseDebugInfoFromNativeLibs]).
 * In that case the runtime stays debuggable, but as optimized code.
 *
 * The target machine created only runs IR passes. The runtime is then linked into the output module and compiled
 * to machine code with it, using the output module's codegen level and instruction selector.
 */
internal fun createLlvmBackendConfigForRuntimeFrom(context: NativeBackendPhaseContext): LLvmBackendConfig =
        createLlvmBackendConfigForOutputModuleFrom(context).copy(
                optimizationLevel = LlvmOptimizationLevel.AGGRESSIVE,
        )
