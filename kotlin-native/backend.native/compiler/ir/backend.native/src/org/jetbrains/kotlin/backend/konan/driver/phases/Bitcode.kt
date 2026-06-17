/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMCodeGenFileType
import llvm.LLVMCodeGenOptLevel
import llvm.LLVMCodeModel
import llvm.LLVMConsumeError
import llvm.LLVMCreatePassBuilderOptions
import llvm.LLVMCreateTargetMachine
import llvm.LLVMDisposePassBuilderOptions
import llvm.LLVMDisposeTargetMachine
import llvm.LLVMDumpModule
import llvm.LLVMGetErrorMessage
import llvm.LLVMGetTargetFromTriple
import llvm.LLVMGetValueName
import llvm.LLVMIsDeclaration
import llvm.LLVMModuleRef
import llvm.LLVMRelocMode
import llvm.LLVMRunPasses
import llvm.LLVMSetValueName
import llvm.LLVMStripModuleDebugInfo
import llvm.LLVMTargetMachineEmitToFile
import llvm.LLVMTargetRefVar
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.ALL
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.NO
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.STRONG
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.YES
import org.jetbrains.kotlin.backend.konan.driver.BasicNativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.LlvmIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultLlvmModuleActions
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionAttribute
import org.jetbrains.kotlin.backend.konan.llvm.addLlvmFunctionEnumAttribute
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.getGlobals
import org.jetbrains.kotlin.backend.konan.llvm.name
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import llvm.LLVMSetFunctionCallConv
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.config.nativeBinaryOptions.SanitizerKind
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File
import kotlin.sequences.forEach


internal data class WriteBitcodeFileInput(
        override val llvmModule: LLVMModuleRef,
        val outputFile: File,
) : LlvmIrHolder

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 */
internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, WriteBitcodeFileInput>(
        "WriteBitcodeFile",
        postactions = getDefaultLlvmModuleActions(),
) { context, (llvmModule, outputFile) ->
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context, llvmModule)
    LLVMWriteBitcodeToFile(llvmModule, outputFile.canonicalPath)
}

internal data class CompileModuleToPtxInput(
        val llvmModule: LLVMModuleRef,
        val outputFile: File,
)

/**
 * Mark each public host-declared function in a `@CudaCompile` file as an NVPTX kernel entry
 * by setting its LLVM calling convention to `PTX_Kernel` (value 71). The NVPTX asm printer
 * checks the calling convention to decide between `.entry` (kernel, host-launchable) and
 * `.func` (device helper, callable from other PTX); private helpers in the same file keep
 * the default convention and stay as `.func`.
 *
 * "Host-declared" means either a top-level function or a user-defined member of a top-level
 * stateless `object`; matches the set built by `IrFile.collectKernelHostDeclarations`, so the
 * names emitted by [AssignCudaKernelExportNamesPhase] and the kernel-entry marking here stay
 * in lockstep.
 *
 * Why calling convention rather than `!nvvm.annotations = !{!{ptr @fn, !"kernel", i32 1}}`:
 * the annotation form depends on a NVPTX-specific lowering pass that `llc`'s default
 * pipeline includes but `LLVMTargetMachineEmitToFile` does not. The calling-convention
 * mark is honored by the asm printer directly, without that pre-pass.
 */
internal val AnnotateCudaKernelsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "AnnotateCudaKernels",
) { generationState, irModule ->
    irModule.files
            .filter { it.hasAnnotation(KonanFqNames.cudaCompile) }
            .flatMap { it.collectKernelHostDeclarations() }
            .filter { it.visibility.isPublicAPI }
            .forEach { kernelFn ->
                val llvmFn = generationState.llvmDeclarations.forFunctionOrNull(kernelFn)?.asCallback()
                        ?: return@forEach
                LLVMSetFunctionCallConv(llvmFn, NVPTX_KERNEL_CALL_CONV)
            }
}

// LLVM's `LLVMPTXKernelCallConv` value. Encoded directly so we don't depend on the K/N
// llvm cinterop exposing the enum constant.
private const val NVPTX_KERNEL_CALL_CONV: Int = 71

// PTX symbols must match `[a-zA-Z_$][a-zA-Z_$0-9]*` — Kotlin's mangled names contain `:`,
// `#`, `<`, `>`, `;`, `,`, `(`, `)`, `.` which all trip `LLVM ERROR: Symbol name with
// unsupported characters` in the NVPTX AsmPrinter. Replace each illegal char with `_`; the
// transform is deterministic so def/use sites within the same LLVM module map to the same
// renamed symbol via the value-ref chain.
private fun sanitizePtxSymbolName(name: String): String = buildString(name.length) {
    name.forEachIndexed { i, c ->
        when {
            c.isLetterOrDigit() && c.code < 128 -> append(c)
            c == '_' || c == '$' -> append(c)
            else -> append('_')
        }
    }
    if (isNotEmpty() && this[0].isDigit()) insert(0, '_')
}

/**
 * Rename all functions and globals in a `Runtime.Kind.CudaDevice` LLVM module to PTX-legal
 * identifiers before bitcode write. PTX symbols must match `[a-zA-Z_$][a-zA-Z_$0-9]*`;
 * Kotlin's mangled names contain `:`, `#`, `<`, `>`, `;`, `,`, `(`, `)`, `.` which all trip
 * `LLVM ERROR: Symbol name with unsupported characters` in the NVPTX AsmPrinter.
 * `LLVMSetValueName` updates every use site automatically (LLVM stores references as value
 * pointers, not strings), so call sites stay consistent without a separate pass.
 */
internal val SanitizeDeviceSymbolsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, LLVMModuleRef>(
        name = "SanitizeDeviceSymbols",
) { _, module ->
    fun renameIfNeeded(value: kotlinx.cinterop.CPointer<llvm.LLVMOpaqueValue>) {
        val oldName = LLVMGetValueName(value)?.toKString().orEmpty()
        if (oldName.isEmpty()) return
        // LLVM intrinsics (`llvm.*`) must keep their original name — they're recognized by
        // the backend by name and lowered to target-specific instructions (e.g., NVPTX maps
        // `llvm.nvvm.read.ptx.sreg.tid.x` to a read of the `%tid.x` special register). Any
        // rename here would turn them into unresolved externs in the PTX output.
        if (oldName.startsWith("llvm.")) return
        val newName = sanitizePtxSymbolName(oldName)
        if (newName != oldName) LLVMSetValueName(value, newName)
    }
    getFunctions(module).forEach { renameIfNeeded(it) }
    getGlobals(module).forEach { renameIfNeeded(it) }
}

/**
 * Optimize and strip dead LLVM IR from a `Runtime.Kind.CudaDevice` module before NVPTX
 * emission.
 *
 * K/N's host LLVM optimization pipeline (`runBitcodePostProcessing`) is intentionally
 * skipped for device fragments because it's wired against the host triple and (via
 * `LTOOptimizationPipeline`) prepends a bare `internalize` that wipes the kernels along
 * with everything else. Without that pipeline a lot of obviously-redundant IR survives all
 * the way through NVPTX ISel:
 *
 *  - K/N's IR-to-LLVM emits `select cond, ptr, null` patterns from the null-normalization
 *    around `NativePtr` / `NativePointed` boxing — NVPTX lowers them as a branch-and-set,
 *    so every device-pointer load/store ends up wrapped in five extra PTX instructions.
 *  - Orphan `unreachable:` blocks (dead arms of throw→unreachable rewrites) keep references
 *    to unbox stubs that NVPTX prints as `.extern .func` at the top of the PTX.
 *
 * `LLVMStripModuleDebugInfo` runs first to drop all DWARF debug metadata (`!dbg`
 * attachments, `DICompileUnit`/`DISubprogram`/etc., `llvm.dbg.*` intrinsics). The device
 * fragment inherits the host module's debug-info configuration, but DWARF in PTX is only
 * useful for `cuda-gdb` source-level debugging, and even when that's wanted, the
 * host-side debug toggle isn't a reliable signal for device debuggability. Stripping
 * unconditionally avoids a ~10x bloat in the emitted `.ptx` (most of it `.debug_*`
 * `.b8` byte-emit sections) on host-debug builds.
 *
 * Running LLVM's `default<O2>` covers the per-module cleanup we actually want (InstCombine
 * folds `select cond, x, null` to `x`, SimplifyCFG drops the orphan blocks, the other
 * passes are no-ops on a clean device module). `lto<O2>` adds the whole-module IPO
 * cleanups (IPSCCP, eliminate-available-externally, GlobalsAA-driven simplification)
 * without the `internalize` that `LTOOptimizationPipeline` wedges in on the host path —
 * here we hand LLVM the bare pipeline string so the wrapper is bypassed entirely.
 * `strip-dead-prototypes` + `globaldce` then drop any remaining unreferenced declarations.
 *
 * Note: `globaldce` doesn't drop functions that still have external linkage (e.g. a
 * `const val` getter whose value is fully inlined into the kernels). Removing those would
 * need an explicit `internalize` with a preserve-list of kernel symbols — left out here
 * because distinguishing kernels from genuine device-private helpers (which today also
 * have external linkage at this point in the pipeline) is delicate, and accidentally
 * internalizing a kernel would have `globaldce` remove it and produce empty PTX.
 */
internal val StripDeadDeviceIrPhase = createSimpleNamedCompilerPhase<NativeGenerationState, LLVMModuleRef>(
        name = "StripDeadDeviceIr",
) { _, module ->
    LLVMStripModuleDebugInfo(module)
    val options = LLVMCreatePassBuilderOptions()!!
    try {
        val err = LLVMRunPasses(
                module,
                "default<O2>,lto<O2>,strip-dead-prototypes,globaldce",
                null,
                options,
        )
        if (err != null) {
            val message = LLVMGetErrorMessage(err)?.toKString().orEmpty()
            LLVMConsumeError(err)
            error("LLVMRunPasses failed for device IR cleanup: $message")
        }
    } finally {
        LLVMDisposePassBuilderOptions(options)
    }
}

// sm_50 (Maxwell) is the lowest virtual architecture that supports the NVVM intrinsics
// used by `kotlin.native.cuda` and still runs on essentially every CUDA-capable card from
// the last decade. Future configuration could pull this from a per-binary option.
private const val NVPTX_DEFAULT_CPU = "sm_50"

// PTX ISA 6.3 is needed for `.alias` directives, which LLVM's NVPTX backend emits for
// some symbol-aliasing patterns we get out of K/N's host codegen. Without this attribute
// llc defaults to an older PTX version and dies with
// `LLVM ERROR: .alias requires PTX version >= 6.3 and sm_30`. 6.3 ships with CUDA 10.0+,
// which is well below any realistically-supported driver on the test target.
private const val NVPTX_PTX_VERSION = "ptx63"

/**
 * Emit a `.ptx` text artifact for a `Runtime.Kind.CudaDevice` fragment by invoking LLVM's
 * NVPTX backend in-process via `LLVMTargetMachineEmitToFile`. Requires:
 *  - `LLVMNVPTX{CodeGen,Desc,Info}` linked into `libllvmstubs.dylib`
 *    (see `llvmInterop/build.gradle.kts`'s `nvptxLibs`);
 *  - `INIT_LLVM_TARGET_WITH_ASM_PRINTER(NVPTX)` registered by `LLVMKotlinInitializeTargets`
 *    in `libllvmext/src/main/cpp/CAPIExtensions.cpp`.
 */
internal val CompileModuleToPtxPhase = createSimpleNamedCompilerPhase<NativeGenerationState, CompileModuleToPtxInput>(
        "CompileModuleToPtx",
) { _, input ->
    LlvmOptimizationPipeline.initLLVMOnce()
    val arena = Arena()
    val targetMachine = try {
        val targetVar = arena.alloc<LLVMTargetRefVar>()
        val foundTarget = LLVMGetTargetFromTriple(NVPTX_TARGET_TRIPLE, targetVar.ptr, null) == 0
        check(foundTarget) { "Failed to look up LLVM NVPTX target ($NVPTX_TARGET_TRIPLE)" }
        LLVMCreateTargetMachine(
                targetVar.value,
                NVPTX_TARGET_TRIPLE,
                NVPTX_DEFAULT_CPU,
                "+$NVPTX_PTX_VERSION",
                LLVMCodeGenOptLevel.LLVMCodeGenLevelDefault,
                LLVMRelocMode.LLVMRelocDefault,
                LLVMCodeModel.LLVMCodeModelDefault,
        ) ?: error("LLVMCreateTargetMachine failed for $NVPTX_TARGET_TRIPLE")
    } catch (t: Throwable) {
        arena.clear()
        throw t
    }
    try {
        val errMsgVar = arena.allocPointerTo<kotlinx.cinterop.ByteVar>()
        val rc = LLVMTargetMachineEmitToFile(
                targetMachine,
                input.llvmModule,
                input.outputFile.absolutePath,
                LLVMCodeGenFileType.LLVMAssemblyFile,
                errMsgVar.ptr,
        )
        check(rc == 0) {
            "LLVMTargetMachineEmitToFile failed: ${errMsgVar.value?.toKString().orEmpty()}"
        }
    } finally {
        LLVMDisposeTargetMachine(targetMachine)
        arena.clear()
    }
}

private const val NVPTX_TARGET_TRIPLE = "nvptx64-nvidia-cuda"

internal data class EmitCudaPtxEmbeddingInput(
        val ptxFile: File,
        val cOutFile: File,
        val bitcodeOutFile: File,
)

/**
 * Wrap the device fragment's emitted PTX text into a self-contained `.bc` that defines
 * `const char* getKotlinCudaPtx(void)` returning a NUL-terminated pointer to the PTX bytes.
 * The host fragment's link step splices this `.bc` into the final binary (see
 * `Context.cudaPtxEmbeddingBitcodeFile` and its consumer in `collectLlvmModules`), so the
 * `kotlin.native.cuda.launchKernel` runtime's `@GCUnsafeCall("getKotlinCudaPtx")` resolves
 * without any user-side cinterop or hand-authored C glue.
 */
internal val EmitCudaPtxEmbeddingPhase = createSimpleNamedCompilerPhase<NativeGenerationState, EmitCudaPtxEmbeddingInput>(
        "EmitCudaPtxEmbedding",
) { generationState, input ->
    val ptxText = input.ptxFile.readText()
    input.cOutFile.writeText(buildPtxEmbeddingC(ptxText))
    val clangCommand = generationState.config.clang.clangC(
            input.cOutFile.absoluteFile.normalize().path,
            "-emit-llvm",
            "-c",
            "-o", input.bitcodeOutFile.absoluteFile.normalize().path,
    )
    Command(clangCommand).execute()
    generationState.context.cudaPtxEmbeddingBitcodeFile = input.bitcodeOutFile.absolutePath
}

private fun buildPtxEmbeddingC(ptxText: String): String = buildString {
    appendLine("// Auto-generated by `EmitCudaPtxEmbeddingPhase`.")
    appendLine("// Carries the PTX text produced by the CUDA device fragment so")
    appendLine("// `kotlin.native.cuda.launchKernel` can pass it to `cuModuleLoadData`.")
    appendLine()
    appendLine("static const char kKotlinCudaPtx[] =")
    ptxText.lineSequence().forEach { line ->
        append("    \"")
        line.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(c)
            }
        }
        appendLine("\\n\"")
    }
    appendLine("    ;")
    appendLine()
    appendLine("const char* getKotlinCudaPtx(void) { return kKotlinCudaPtx; }")
}

internal val CheckExternalCallsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CheckExternalCalls",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    checkLlvmModuleExternalCalls(context)
}

/**
 * Rewrites globals for external calls checker after optimizer run.
 */
internal val RewriteExternalCallsCheckerGlobals = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "RewriteExternalCallsCheckerGlobals",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    addFunctionsListSymbolForChecker(context)
}

internal class OptimizationState(
        config: NativeSecondStageCompilationConfig,
        val llvmConfig: LlvmPipelineConfig,
        override val performanceManager: PerformanceManager?,
) : BasicNativeBackendPhaseContext(config)

internal fun optimizationPipelinePass(name: String, pipeline: (LlvmPipelineConfig, PerformanceManager?, LoggingContext) -> LlvmOptimizationPipeline) =
        createSimpleNamedCompilerPhase<OptimizationState, LLVMModuleRef>(
                name = name,
                postactions = getDefaultLlvmModuleActions(),
        ) { context, module ->
            pipeline(context.llvmConfig.copyConfiguringSaveIr(context, name), context.performanceManager, context).use {
                it.execute(module)
            }
        }

internal val MandatoryBitcodeLLVMPostprocessingPhase = optimizationPipelinePass(
        name = "MandatoryBitcodeLLVMPostprocessingPhase",
        pipeline = ::MandatoryOptimizationPipeline,
)

internal val ModuleBitcodeOptimizationPhase = optimizationPipelinePass(
        name = "ModuleBitcodeOptimization",
        pipeline = ::ModuleOptimizationPipeline,
)

internal val LTOBitcodeOptimizationPhase = optimizationPipelinePass(
        name = "LTOBitcodeOptimization",
        pipeline = ::LTOOptimizationPipeline
)

internal val ThreadSanitizerPhase = optimizationPipelinePass(
        name = "ThreadSanitizerPhase",
        pipeline = ::ThreadSanitizerPipeline
)

internal val StackProtectorPhaseInCompiler = createSimpleNamedCompilerPhase<OptimizationState, LLVMModuleRef>(
        name = "StackProtectorPhase",
        postactions = getDefaultLlvmModuleActions(),
        op = { context: OptimizationState, module: LLVMModuleRef ->
            val attribute = when (context.llvmConfig.sspMode) {
                NO -> null
                YES -> LlvmFunctionAttribute.Ssp
                STRONG -> LlvmFunctionAttribute.SspStrong
                ALL -> LlvmFunctionAttribute.SspReq
            }
            attribute?.let { sspAttribute ->
                getFunctions(module)
                        .filter { LLVMIsDeclaration(it) == 0 && it.name != "__clang_call_terminate" }
                        .forEach { addLlvmFunctionEnumAttribute(it, sspAttribute) }
            }
        }
)

internal val StackProtectorPhaseInLLVM = optimizationPipelinePass(
        name = "StackProtectorPhase",
        pipeline = ::StackProtectorPipeline
)

internal val RemoveRedundantSafepointsPhase = createSimpleNamedCompilerPhase<BitcodePostProcessingContext, Unit>(
        name = "RemoveRedundantSafepoints",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = context.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val CStubsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CStubs",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ -> produceCStubs(context) }
)

internal val LinkBitcodeDependenciesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, List<File>>(
        name = "LinkBitcodeDependencies",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, input -> linkBitcodeDependencies(context, input) }
)

internal val VerifyBitcodePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, LLVMModuleRef>(
        name = "VerifyBitcode",
        op = { _, llvmModule -> verifyModule(llvmModule) }
)

internal val PrintBitcodePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, LLVMModuleRef>(
        name = "PrintBitcode",
        op = { _, llvmModule -> LLVMDumpModule(llvmModule) }
)

internal fun <T : BitcodePostProcessingContext> PhaseEngine<T>.runBitcodePostProcessing() {
    val optimizationConfig = createLTOFinalPipelineConfig(
            context,
            context.llvm.targetTriple,
            closedWorld = context.config.isFinalBinary,
            timePasses = context.config.phaseConfig.needProfiling,
    )
    useContext(OptimizationState(context.config, optimizationConfig, context.performanceManager)) {
        val module = this@runBitcodePostProcessing.context.llvmModule
        if (context.config.runLLVMPassesInCompiler) {
            it.runAndMeasurePhase(StackProtectorPhaseInCompiler, module)
        } else {
            it.runAndMeasurePhase(StackProtectorPhaseInLLVM, module)
        }
        it.runAndMeasurePhase(MandatoryBitcodeLLVMPostprocessingPhase, module)
        it.runAndMeasurePhase(ModuleBitcodeOptimizationPhase, module)
        it.runAndMeasurePhase(LTOBitcodeOptimizationPhase, module)
        when (context.config.sanitizer) {
            SanitizerKind.THREAD -> it.runAndMeasurePhase(ThreadSanitizerPhase, module)
            SanitizerKind.ADDRESS -> context.reportCompilationError("Address sanitizer is not supported yet")
            null -> {}
        }
    }
    runAndMeasurePhase(RemoveRedundantSafepointsPhase)
}
