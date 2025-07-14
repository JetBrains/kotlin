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
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModule
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModulesConfig
import org.jetbrains.kotlin.config.nativeBinaryOptions.CCallMode
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import kotlin.collections.orEmpty

/**
 * Collects bitcode modules for the hot reload host.
 *
 * When launcherOnly=true (default for hot reload):
 * - Only includes the hot reload launcher module
 * - C++ runtime and stdlib come from libstdlib-cache.a at link time
 * - This avoids duplicate symbols because host.o has no overlap with stdlib-cache.a
 *
 * When launcherOnly=false:
 * - Includes C++ runtime bitcode (GC, MM, allocator, etc.)
 * - Hot reload launcher
 *
 * @param config The Konan configuration
 * @param llvmContext The LLVM context to use for parsing bitcode
 * @param launcherOnly If true, only include launcher (C++ runtime comes from stdlib-cache.a)
 * @return List of parsed LLVM modules
 */
internal fun collectHostModulesForHotReload(
        config: NativeSecondStageCompilationConfig,
        llvmContext: LLVMContextRef,
        launcherOnly: Boolean = false,
        runtimeLogs: Map<LoggingTag, LoggingLevel>? = null
): List<LLVMModuleRef> {
    val runtimeModulesConfig = RuntimeModulesConfig(config)

    fun MutableList<String>.add(module: RuntimeModule) = add(runtimeModulesConfig.absolutePathFor(module))

    val bitcodeFiles = if (launcherOnly) {
        // Include hot reload launcher + hot reload module, C++ runtime and stdlib will come from libstdlib-cache.a at link time
        buildList {
            if (config.produce == CompilerOutputKind.PROGRAM) {
                // Hot reload implementation (HotReloadImpl, JITLink integration)
                if (runtimeModulesConfig.containsHotReloadRuntime) add(RuntimeModule.HOT_RELOAD)
                // Hot reload launcher (Konan_main, Init_and_run_start)
                add(RuntimeModule.HOT_RELOAD_LAUNCHER)
            }
        }
    } else {
        // Include full C++ runtime (for non-hot-reload use cases)
        resolveRuntimeModules(runtimeModulesConfig, config) + buildList {
            if (config.produce == CompilerOutputKind.PROGRAM) {
                add(RuntimeModule.HOT_RELOAD_LAUNCHER)
            }
        }
    }

    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.mapNotNull { bitcodeFile ->
        memScoped {
            val bufRef = alloc<LLVMMemoryBufferRefVar>()
            val errorRef = allocPointerTo<ByteVar>()

            val res = LLVMCreateMemoryBufferWithContentsOfFile(bitcodeFile, bufRef.ptr, errorRef.ptr)
            if (res != 0) {
                // Failed to load bitcode file
                return@mapNotNull null
            }

            val memoryBuffer = bufRef.value
            try {
                val moduleRef = alloc<LLVMModuleRefVar>()
                val parseRes = LLVMParseBitcodeInContext2(llvmContext, memoryBuffer, moduleRef.ptr)
                if (parseRes != 0) {
                    // Failed to parse bitcode file
                    return@mapNotNull null
                }
                moduleRef.value
            } finally {
                LLVMDisposeMemoryBuffer(memoryBuffer)
            }
        }
    }

    val hostModules = parseBitcodeFiles(bitcodeFiles)

    // If runtimeLogs is provided, generate a constants module with strong linkage
    // This overrides the weak Kotlin_runtimeLogs from stdlib-cache.a
    val modulesWithConstants = if (runtimeLogs != null) {
        val constantsModule = generateRuntimeLogsModule(llvmContext, runtimeLogs)
        hostModules + constantsModule
    } else {
        hostModules
    }

    return modulesWithConstants
}

/**
 * Generates a minimal LLVM module containing only the Kotlin_runtimeLogs constant with strong linkage.
 * This overrides the weak symbol from stdlib-cache.a, allowing user-specified logging levels.
 */
private fun generateRuntimeLogsModule(
        llvmContext: LLVMContextRef,
        runtimeLogs: Map<LoggingTag, LoggingLevel>
): LLVMModuleRef {
    val module = LLVMModuleCreateWithNameInContext("runtime_logs", llvmContext)!!

    val int32Type = LLVMInt32TypeInContext(llvmContext)

    // Create the array of log levels (one for each LoggingTag)
    val logLevels = LoggingTag.entries.sortedBy { it.ord }.map { tag ->
        val level = runtimeLogs[tag] ?: LoggingLevel.None
        LLVMConstInt(int32Type, level.ord.toLong(), 0)
    }

    // Create the array type and constant array
    val arrayType = LLVMArrayType(int32Type, logLevels.size)!!
    val arrayConstant = LLVMConstArray(int32Type, logLevels.toCValues(), logLevels.size)!!

    // Create the global variable with STRONG linkage (External)
    // This will override the weak symbol from stdlib-cache.a
    val global = LLVMAddGlobal(module, arrayType, "Kotlin_runtimeLogs")!!
    LLVMSetInitializer(global, arrayConstant)
    LLVMSetGlobalConstant(global, 1)
    LLVMSetLinkage(global, LLVMLinkage.LLVMExternalLinkage)

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
        addAll(generatedBitcodeFiles.map { it.absoluteFile.normalize().path })
        addAll(additionalProducedBitcodeFiles)
        addAll(bitcodeLibraries)
        // NOTE: We intentionally skip:
        // - RuntimeModule.LAUNCHER (goes to host)
        // - Runtime modules (MM, GC, alloc) - they're in libstdlib-cache.a
        // - config.nativeLibraries - user native libraries (may need to revisit)
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