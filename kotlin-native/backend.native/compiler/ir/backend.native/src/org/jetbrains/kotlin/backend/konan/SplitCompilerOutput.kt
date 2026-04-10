/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import llvm.LLVMAddGlobal
import llvm.LLVMArrayType
import llvm.LLVMConstArray
import llvm.LLVMConstInt
import llvm.LLVMContextRef
import llvm.LLVMCreateMemoryBufferWithContentsOfFile
import llvm.LLVMDisposeMemoryBuffer
import llvm.LLVMInt32TypeInContext
import llvm.LLVMLinkage
import llvm.LLVMMemoryBufferRefVar
import llvm.LLVMModuleCreateWithNameInContext
import llvm.LLVMModuleRef
import llvm.LLVMModuleRefVar
import llvm.LLVMParseBitcodeInContext2
import llvm.LLVMSetGlobalConstant
import llvm.LLVMSetInitializer
import llvm.LLVMSetLinkage
import llvm.LLVMTypeRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.llvm.RuntimeConstantNames
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModule
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModulesConfig
import org.jetbrains.kotlin.config.nativeBinaryOptions.CCallMode
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import kotlin.collections.orEmpty

// Those values are used from the runtime to check if hot-reload module is enabled,
// and in what mode. When updating this enum class, please take a look at `CompilerConstants.hpp`
private enum class HotReloadOrigin(val id: Int) {
    NONE(0),
    PROGRAM(1),
    FRAMEWORK(2);

    companion object {
        fun of(config: NativeSecondStageCompilationConfig): HotReloadOrigin {
            if (!config.hotReloadEnabled) return NONE
            return when (config.produce) {
                CompilerOutputKind.PROGRAM -> PROGRAM
                CompilerOutputKind.FRAMEWORK -> FRAMEWORK
                else -> NONE
            }
        }
    }
}

internal fun collectHostModulesForProgramHotReload(
        generationState: NativeGenerationState,
        llvmContext: LLVMContextRef,
        runtimeLogs: Map<LoggingTag, LoggingLevel>? = null
): List<LLVMModuleRef> {
    val config = generationState.config
    val runtimeModulesConfig = RuntimeModulesConfig(config)

    val bitcodeFiles = buildList {
        if (runtimeModulesConfig.containsHotReloadRuntime) {
            add(runtimeModulesConfig.absolutePathFor(RuntimeModule.HOT_RELOAD))
            add(runtimeModulesConfig.absolutePathFor(RuntimeModule.HOT_RELOAD_LAUNCHER))
        }
    }

    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        parseBitcodeFile(generationState, generationState.messageCollector, llvmContext, bitcodeFile)
    }

    return parseBitcodeFiles(bitcodeFiles) + config.overrideRuntimeConstants(llvmContext, runtimeLogs)
}

private fun LLVMModuleRef.addStrongLinkageConstant(name: String, type: LLVMTypeRef, initializer: LLVMValueRef) {
    LLVMAddGlobal(this, type, name)!!.apply {
        LLVMSetInitializer(this, initializer)
        LLVMSetGlobalConstant(this, 1)
        LLVMSetLinkage(this, LLVMLinkage.LLVMExternalLinkage)
    }
}

/**
 * Generates a minimal LLVM module containing only the runtime constants with strong linkage.
 */
internal fun NativeSecondStageCompilationConfig.overrideRuntimeConstants(
        llvmContext: LLVMContextRef,
        runtimeLogs: Map<LoggingTag, LoggingLevel>?
): LLVMModuleRef {

    // Override weak symbols from the cache with a stronger one
    val module = LLVMModuleCreateWithNameInContext("strong_constants", llvmContext)!!
    val int32Type = LLVMInt32TypeInContext(llvmContext)!!

    val hotReloadOrigin = HotReloadOrigin.of(this@overrideRuntimeConstants)
    module.addStrongLinkageConstant(RuntimeConstantNames.HOT_RELOAD, int32Type, LLVMConstInt(int32Type, hotReloadOrigin.id.toLong(), 0)!!)

    if (runtimeLogs != null) {
        val logLevels = LoggingTag.entries.sortedBy { it.ord }.map { tag ->
            val level = runtimeLogs[tag] ?: LoggingLevel.None
            LLVMConstInt(int32Type, level.ord.toLong(), 0)
        }
        val arrayType = LLVMArrayType(int32Type, logLevels.size)!!
        val arrayConstant = LLVMConstArray(int32Type, logLevels.toCValues(), logLevels.size)!!
        module.addStrongLinkageConstant(RuntimeConstantNames.RUNTIME_LOGS, arrayType, arrayConstant)
    }

    return module
}

/**
 * Links library bitcode (interop stubs) into the bootstrap module for hot reload.
 *
 * This function links ONLY the library bitcode containing interop stubs (knifunptr_*, etc.)
 * without the C++ runtime. The runtime comes from libstdlib-cache.a at link time.
 *
 * This is needed because platform library interop generates function pointer wrappers
 * (knifunptr_*) that are stored as bitcode in the klib. User code using platform APIs
 * calls these wrappers, so they must be included in bootstrap.o.
 *
 * @param generationState The native generation state
 * @param generatedBitcodeFiles Additional bitcode files from cexport
 */
internal fun linkLibraryBitcodeForHotReload(
        generationState: NativeGenerationState,
        generatedBitcodeFiles: List<java.io.File>
) {
    val config = generationState.config

    // Access llvm FIRST to allow it to add dependencies during initialization,
    // BEFORE we access bitcodeToLink which seals the dependencies tracker.
    @Suppress("UNUSED_VARIABLE")
    val additionalProducedBitcodeFiles = generationState.llvm.additionalProducedBitcodeFiles

    // Get library bitcode from dependencies tracker
    // This contains interop stubs (knifunptr_*) from platform libraries
    val bitcodeLibraries = generationState.dependenciesTracker.bitcodeToLink
            .filterNot { it.isCInteropLibrary() && config.cCallMode == CCallMode.Direct }
            .filterNot { it.isNativeStdlib } // Skip stdlib bitcode (it's in stdlib-cache.a)
            .flatMap { it.bitcode(config.target)?.bitcodeFilePaths.orEmpty() }
            .filter { it.isBitcode }

    // Collect all bitcode files to link into bootstrap (interop stubs only, no runtime)
    val bitcodeFilesToLink = buildList<String> {
        // NOTE: We intentionally skip:
        // - RuntimeModule.LAUNCHER (goes to host)
        // - Runtime modules (MM, GC, alloc), they're in libstdlib-cache.a
        // - config.nativeLibraries, user native libraries (may need to revisit)
        addAll(generatedBitcodeFiles.map { it.absoluteFile.normalize().path })
        addAll(additionalProducedBitcodeFiles)
        addAll(bitcodeLibraries)
    }

    if (bitcodeFilesToLink.isEmpty()) {
        generationState.log { "No library bitcode to link into bootstrap" }
        return
    }

    // Parse and link each bitcode file into the bootstrap module
    val bootstrapModule = generationState.llvmModule
    bitcodeFilesToLink.forEach { bitcodeFile ->
        parseAndLinkBitcodeFile(generationState, bootstrapModule, bitcodeFile)
    }
}