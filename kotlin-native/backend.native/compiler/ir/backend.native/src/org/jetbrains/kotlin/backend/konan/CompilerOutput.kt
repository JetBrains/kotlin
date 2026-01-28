/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.patchObjCRuntimeModule
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModule
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModulesConfig
import org.jetbrains.kotlin.backend.konan.llvm.runtime.linkRuntimeModules
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.config.nativeBinaryOptions.CCallMode
import org.jetbrains.kotlin.config.nativeBinaryOptions.CInterfaceGenerationMode
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.config.nativeBinaryOptions.GCSchedulerType
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.library.linkerOpts
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.supportsCoreSymbolication
import org.jetbrains.kotlin.konan.target.supportsLibBacktrace
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File

/**
 * Supposed to be true for a single LLVM module within final binary.
 */
val KonanConfig.isFinalBinary: Boolean get() = when (this.produce) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC -> true
    CompilerOutputKind.DYNAMIC_CACHE, CompilerOutputKind.STATIC_CACHE, CompilerOutputKind.HEADER_CACHE,
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
    CompilerOutputKind.FRAMEWORK -> !omitFrameworkBinary
    CompilerOutputKind.TEST_BUNDLE -> true
}

val CompilerOutputKind.isNativeLibrary: Boolean
    get() = this == CompilerOutputKind.DYNAMIC || this == CompilerOutputKind.STATIC

/**
 * Return true if compiler has to generate a C API for dynamic/static library.
 */
val KonanConfig.produceCInterface: Boolean
    get() = this.produce.isNativeLibrary && this.cInterfaceGenerationMode != CInterfaceGenerationMode.NONE

val CompilerOutputKind.involvesBitcodeGeneration: Boolean
    get() = this != CompilerOutputKind.LIBRARY

internal val CacheDeserializationStrategy?.containsKFunctionImpl: Boolean
    get() = this?.contains(KonanFqNames.internalPackageName, "KFunctionImpl.kt") != false

internal val NativeGenerationState.shouldDefineFunctionClasses: Boolean
    get() = producedLlvmModuleContainsStdlib && cacheDeserializationStrategy.containsKFunctionImpl

internal val NativeGenerationState.shouldDefineCachedBoxes: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            cacheDeserializationStrategy?.contains(KonanFqNames.internalPackageName, "Boxing.kt") != false

internal val CacheDeserializationStrategy?.containsRuntime: Boolean
    get() = this?.contains(KonanFqNames.internalPackageName, "Runtime.kt") != false

internal val NativeGenerationState.shouldLinkRuntimeNativeLibraries: Boolean
    get() = producedLlvmModuleContainsStdlib && cacheDeserializationStrategy.containsRuntime

val CompilerOutputKind.isFullCache: Boolean
    get() = this == CompilerOutputKind.STATIC_CACHE || this == CompilerOutputKind.DYNAMIC_CACHE

val CompilerOutputKind.isHeaderCache: Boolean
    get() = this == CompilerOutputKind.HEADER_CACHE

val CompilerOutputKind.isCache: Boolean
    get() = this.isFullCache || this.isHeaderCache

internal fun produceCStubs(generationState: NativeGenerationState) {
    generationState.cStubsManager.compile(
            generationState.config.clang,
            generationState.messageCollector,
            generationState.inVerbosePhase
    ).forEach {
        parseAndLinkBitcodeFile(generationState, generationState.llvm.module, it.absolutePath)
    }
}

private data class LlvmModules(
        val runtimeModules: List<LLVMModuleRef>,
        val additionalModules: List<LLVMModuleRef>
)

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
        config: KonanConfig,
        llvmContext: LLVMContextRef,
        launcherOnly: Boolean = false,
        runtimeLogs: Map<LoggingTag, LoggingLevel>? = null
): List<LLVMModuleRef> {
    val runtimeModulesConfig = RuntimeModulesConfig(config)

    fun MutableList<String>.add(module: RuntimeModule) = add(runtimeModulesConfig.absolutePathFor(module))

    // Collect bitcode files based on mode
    val bitcodeFiles = if (launcherOnly) {
        // Include hot reload launcher + hot reload module
        // C++ runtime and stdlib will come from libstdlib-cache.a at link time
        buildList<String> {
            if (config.produce == CompilerOutputKind.PROGRAM) {
                // Hot reload implementation (HotReloadImpl, JITLink integration)
                if (runtimeModulesConfig.containsHotReloadRuntime) add(RuntimeModule.HOT_RELOAD)
                // Hot reload launcher (Konan_main, Init_and_run_start)
                add(RuntimeModule.HOT_RELOAD_LAUNCHER)
            }
        }
    } else {
        // Include full C++ runtime (for non-hot-reload use cases)
        buildList<String> {
            if (runtimeModulesConfig.containsDebuggingRuntime) add(RuntimeModule.DEBUG)
            if (runtimeModulesConfig.containsHotReloadRuntime) add(RuntimeModule.HOT_RELOAD)
            add(RuntimeModule.MAIN)
            add(RuntimeModule.MM)
            add(RuntimeModule.ALLOC_COMMON)
            add(RuntimeModule.GC_COMMON)
            add(RuntimeModule.GC_SCHEDULER_COMMON)

            if (config.target.family == Family.OSX && config.minidumpLocation != null) {
                add(RuntimeModule.BREAKPAD)
                add(RuntimeModule.CRASH_HANDLER_IMPL)
            } else {
                add(RuntimeModule.CRASH_HANDLER_NOOP)
            }
            when (config.gcSchedulerType) {
                GCSchedulerType.MANUAL -> add(RuntimeModule.GC_SCHEDULER_MANUAL)
                GCSchedulerType.ADAPTIVE -> add(RuntimeModule.GC_SCHEDULER_ADAPTIVE)
                GCSchedulerType.AGGRESSIVE -> add(RuntimeModule.GC_SCHEDULER_AGGRESSIVE)
                GCSchedulerType.DISABLED, GCSchedulerType.WITH_TIMER, GCSchedulerType.ON_SAFE_POINTS -> {
                    throw IllegalStateException("Deprecated options must have already been handled")
                }
            }
            when (config.gc) {
                GC.STOP_THE_WORLD_MARK_AND_SWEEP -> add(RuntimeModule.GC_STOP_THE_WORLD_MARK_AND_SWEEP)
                GC.NOOP -> add(RuntimeModule.GC_NOOP)
                GC.PARALLEL_MARK_CONCURRENT_SWEEP -> add(RuntimeModule.GC_PARALLEL_MARK_CONCURRENT_SWEEP)
                GC.CONCURRENT_MARK_AND_SWEEP -> add(RuntimeModule.GC_CONCURRENT_MARK_AND_SWEEP)
            }
            if (config.target.supportsCoreSymbolication()) {
                add(RuntimeModule.SOURCE_INFO_CORE_SYMBOLICATION)
            }
            if (config.target.supportsLibBacktrace()) {
                add(RuntimeModule.SOURCE_INFO_LIBBACKTRACE)
                add(RuntimeModule.LIBBACKTRACE)
            }
            when (config.allocationMode) {
                AllocationMode.STD -> {
                    add(RuntimeModule.ALLOC_LEGACY)
                    add(RuntimeModule.ALLOC_STD)
                }
                AllocationMode.CUSTOM -> {
                    add(RuntimeModule.ALLOC_CUSTOM)
                }
            }
            when (config.checkStateAtExternalCalls) {
                true -> add(RuntimeModule.EXTERNAL_CALLS_CHECKER_IMPL)
                false -> add(RuntimeModule.EXTERNAL_CALLS_CHECKER_NOOP)
            }
            if (config.produce == CompilerOutputKind.PROGRAM) {
                add(RuntimeModule.HOT_RELOAD_LAUNCHER)
            }
        }
    }

    if (launcherOnly) {
        println("HOT_RELOAD_SPLIT: Host module - Launcher only mode (runtime from stdlib-cache.a)")
    }
    println("HOT_RELOAD_SPLIT: Host module - Bitcode files: ${bitcodeFiles.size}")

    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.mapNotNull { bitcodeFile ->
        memScoped {
            val bufRef = alloc<LLVMMemoryBufferRefVar>()
            val errorRef = allocPointerTo<ByteVar>()

            val res = LLVMCreateMemoryBufferWithContentsOfFile(bitcodeFile, bufRef.ptr, errorRef.ptr)
            if (res != 0) {
                println("HOT_RELOAD_SPLIT: Warning - Failed to load bitcode file: $bitcodeFile - ${errorRef.value?.toKString()}")
                return@mapNotNull null
            }

            val memoryBuffer = bufRef.value
            try {
                val moduleRef = alloc<LLVMModuleRefVar>()
                val parseRes = LLVMParseBitcodeInContext2(llvmContext, memoryBuffer, moduleRef.ptr)
                if (parseRes != 0) {
                    println("HOT_RELOAD_SPLIT: Warning - Failed to parse bitcode file: $bitcodeFile")
                    return@mapNotNull null
                }
                moduleRef.value
            } finally {
                LLVMDisposeMemoryBuffer(memoryBuffer)
            }
        }
    }

    val hostModules = parseBitcodeFiles(bitcodeFiles)

    println("HOT_RELOAD_SPLIT: Host module - Total parsed modules: ${hostModules.size}")

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

    // Create int32 type
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
 * Collects ONLY the C++ runtime bitcode modules for hot reload.
 * This function does NOT access dependenciesTracker.bitcodeToLink, so it won't seal the tracker.
 * Use this when you need to collect runtime modules without affecting the dependencies tracker state.
 *
 * @param generationState The generation state to use for parsing bitcode files
 * @return List of parsed LLVM runtime modules (not linked yet)
 */
internal fun collectRuntimeModulesOnly(generationState: NativeGenerationState): List<LLVMModuleRef> {
    val config = generationState.config
    val runtimeModulesConfig = generationState.runtimeModulesConfig

    fun MutableList<String>.add(module: RuntimeModule) = add(runtimeModulesConfig.absolutePathFor(module))

    // Collect only C++ runtime bitcode files - NO stdlib, NO bitcodeToLink access
    val runtimeBitcodeFiles = buildList<String> {
        if (runtimeModulesConfig.containsDebuggingRuntime) add(RuntimeModule.DEBUG)
        if (runtimeModulesConfig.containsHotReloadRuntime) add(RuntimeModule.HOT_RELOAD)
        add(RuntimeModule.MAIN)
        add(RuntimeModule.MM)
        add(RuntimeModule.ALLOC_COMMON)
        add(RuntimeModule.GC_COMMON)
        add(RuntimeModule.GC_SCHEDULER_COMMON)

        if (config.target.family == Family.OSX && config.minidumpLocation != null) {
            add(RuntimeModule.BREAKPAD)
            add(RuntimeModule.CRASH_HANDLER_IMPL)
        } else {
            add(RuntimeModule.CRASH_HANDLER_NOOP)
        }
        when (config.gcSchedulerType) {
            GCSchedulerType.MANUAL -> add(RuntimeModule.GC_SCHEDULER_MANUAL)
            GCSchedulerType.ADAPTIVE -> add(RuntimeModule.GC_SCHEDULER_ADAPTIVE)
            GCSchedulerType.AGGRESSIVE -> add(RuntimeModule.GC_SCHEDULER_AGGRESSIVE)
            GCSchedulerType.DISABLED, GCSchedulerType.WITH_TIMER, GCSchedulerType.ON_SAFE_POINTS -> {
                throw IllegalStateException("Deprecated options must have already been handled")
            }
        }
        when (config.gc) {
            GC.STOP_THE_WORLD_MARK_AND_SWEEP -> add(RuntimeModule.GC_STOP_THE_WORLD_MARK_AND_SWEEP)
            GC.NOOP -> add(RuntimeModule.GC_NOOP)
            GC.PARALLEL_MARK_CONCURRENT_SWEEP -> add(RuntimeModule.GC_PARALLEL_MARK_CONCURRENT_SWEEP)
            GC.CONCURRENT_MARK_AND_SWEEP -> add(RuntimeModule.GC_CONCURRENT_MARK_AND_SWEEP)
        }
        if (config.target.supportsCoreSymbolication()) {
            add(RuntimeModule.SOURCE_INFO_CORE_SYMBOLICATION)
        }
        if (config.target.supportsLibBacktrace()) {
            add(RuntimeModule.SOURCE_INFO_LIBBACKTRACE)
            add(RuntimeModule.LIBBACKTRACE)
        }
        when (config.allocationMode) {
            AllocationMode.STD -> {
                add(RuntimeModule.ALLOC_LEGACY)
                add(RuntimeModule.ALLOC_STD)
            }
            AllocationMode.CUSTOM -> {
                add(RuntimeModule.ALLOC_CUSTOM)
            }
        }
        when (config.checkStateAtExternalCalls) {
            true -> add(RuntimeModule.EXTERNAL_CALLS_CHECKER_IMPL)
            false -> add(RuntimeModule.EXTERNAL_CALLS_CHECKER_NOOP)
        }
        // Add launcher for PROGRAM output
        // Use hot reload launcher when hot reload split is enabled (contains JITLink integration)
        if (config.produce == CompilerOutputKind.PROGRAM) {
            if (config.hotReloadSplitEnabled) {
                add(RuntimeModule.HOT_RELOAD_LAUNCHER)
            } else {
                add(RuntimeModule.LAUNCHER)
            }
        }
    }

    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        val parsedModule = parseBitcodeFile(generationState, generationState.messageCollector, generationState.llvmContext, bitcodeFile)
        if (!generationState.shouldUseDebugInfoFromNativeLibs()) {
            LLVMStripModuleDebugInfo(parsedModule)
        }
        parsedModule
    }

    val runtimeModules = parseBitcodeFiles(runtimeBitcodeFiles)

    // Add runtime constants module
    val runtimeModulesWithConstants = if (runtimeModules.isNotEmpty()) {
        runtimeModules + generationState.generateRuntimeConstantsModule()
    } else {
        runtimeModules
    }

    // Optimize runtime modules
    return linkRuntimeModules(generationState, runtimeModulesWithConstants)
}

/**
 * Result of collecting LLVM modules for hot reload split compilation.
 * Contains separate module lists for the host executable and user bootstrap object.
 */
internal data class HotReloadSplitModules(
        /**
         * Modules that form the host executable:
         * - Runtime (GC, MM, allocator)
         * - Launcher
         * - Platform support (ObjC, etc.)
         * - Hot reload infrastructure
         */
        val hostModules: List<LLVMModuleRef>,
        /**
         * Modules that form the bootstrap object (user code):
         * - User-written Kotlin code
         * - Generated code (serialization, etc.)
         * - Metadata
         */
        val bootstrapModules: List<LLVMModuleRef>,
)

/**
 * Deserialize, generate, patch all bitcode dependencies and classify them into two sets:
 * - Runtime modules. These may be used as an input for a separate LTO (e.g. for debug builds).
 * - Everything else.
 */
private fun collectLlvmModules(generationState: NativeGenerationState, generatedBitcodeFiles: List<String>): LlvmModules {
    val config = generationState.config
    val runtimeModulesConfig = generationState.runtimeModulesConfig

    // IMPORTANT: Access llvm FIRST to allow it to add dependencies during initialization,
    // BEFORE we access bitcodeToLink which seals the dependencies tracker.
    val additionalProducedBitcodeFiles = generationState.llvm.additionalProducedBitcodeFiles

    val (bitcodePartOfStdlib, bitcodeLibraries) = generationState.dependenciesTracker.bitcodeToLink
            .filterNot { it.isCInteropLibrary() && config.cCallMode == CCallMode.Direct }
            .partition { it.isNativeStdlib && generationState.producedLlvmModuleContainsStdlib }
            .toList()
            .map { libraries ->
                libraries.flatMap { it.bitcode(config.target)?.bitcodeFilePaths.orEmpty() }.filter { it.isBitcode }
            }

    fun MutableList<String>.add(module: RuntimeModule) = add(runtimeModulesConfig.absolutePathFor(module))

    val additionalBitcodeFiles = buildList<String> {
        addAll(config.nativeLibraries)
        if (config.produce == CompilerOutputKind.PROGRAM) {
            add(RuntimeModule.LAUNCHER)
        }
        addAll(generatedBitcodeFiles)
        addAll(additionalProducedBitcodeFiles)
        addAll(bitcodeLibraries)
        if (config.produce == CompilerOutputKind.DYNAMIC_CACHE) {
            add(RuntimeModule.EXCEPTIONS_SUPPORT)
        }
        if (config.produce == CompilerOutputKind.TEST_BUNDLE) {
            add(RuntimeModule.XCTEST_LAUNCHER)
        }
    }

    val runtimeBitcodeFiles = buildList<String> {
        if (runtimeModulesConfig.containsDebuggingRuntime) add(RuntimeModule.DEBUG)
        if (runtimeModulesConfig.containsHotReloadRuntime) add(RuntimeModule.HOT_RELOAD)
        add(RuntimeModule.MAIN)
        add(RuntimeModule.MM)
        add(RuntimeModule.ALLOC_COMMON)
        add(RuntimeModule.GC_COMMON)
        add(RuntimeModule.GC_SCHEDULER_COMMON)

        if (config.target.family == Family.OSX && config.minidumpLocation != null) {
            add(RuntimeModule.BREAKPAD)
            add(RuntimeModule.CRASH_HANDLER_IMPL)
        } else {
            add(RuntimeModule.CRASH_HANDLER_NOOP)
        }
        when (config.gcSchedulerType) {
            GCSchedulerType.MANUAL -> {
                add(RuntimeModule.GC_SCHEDULER_MANUAL)
            }
            GCSchedulerType.ADAPTIVE -> {
                add(RuntimeModule.GC_SCHEDULER_ADAPTIVE)
            }
            GCSchedulerType.AGGRESSIVE -> {
                add(RuntimeModule.GC_SCHEDULER_AGGRESSIVE)
            }
            GCSchedulerType.DISABLED, GCSchedulerType.WITH_TIMER, GCSchedulerType.ON_SAFE_POINTS -> {
                throw IllegalStateException("Deprecated options must have already been handled")
            }
        }
        when (config.gc) {
            GC.STOP_THE_WORLD_MARK_AND_SWEEP -> add(RuntimeModule.GC_STOP_THE_WORLD_MARK_AND_SWEEP)
            GC.NOOP -> add(RuntimeModule.GC_NOOP)
            GC.PARALLEL_MARK_CONCURRENT_SWEEP -> add(RuntimeModule.GC_PARALLEL_MARK_CONCURRENT_SWEEP)
            GC.CONCURRENT_MARK_AND_SWEEP -> add(RuntimeModule.GC_CONCURRENT_MARK_AND_SWEEP)
        }
        if (config.target.supportsCoreSymbolication()) {
            add(RuntimeModule.SOURCE_INFO_CORE_SYMBOLICATION)
        }
        if (config.target.supportsLibBacktrace()) {
            add(RuntimeModule.SOURCE_INFO_LIBBACKTRACE)
            add(RuntimeModule.LIBBACKTRACE)
        }
        when (config.allocationMode) {
            AllocationMode.STD -> {
                add(RuntimeModule.ALLOC_LEGACY)
                add(RuntimeModule.ALLOC_STD)
            }
            AllocationMode.CUSTOM -> {
                add(RuntimeModule.ALLOC_CUSTOM)
            }
        }
        when (config.checkStateAtExternalCalls) {
            true -> add(RuntimeModule.EXTERNAL_CALLS_CHECKER_IMPL)
            false -> add(RuntimeModule.EXTERNAL_CALLS_CHECKER_NOOP)
        }
        // Bitcode parts of stdlib are considered part of the runtime
        addAll(bitcodePartOfStdlib)
    }

    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        val parsedModule = parseBitcodeFile(generationState, generationState.messageCollector, generationState.llvmContext, bitcodeFile)
        if (!generationState.shouldUseDebugInfoFromNativeLibs()) {
            LLVMStripModuleDebugInfo(parsedModule)
        }
        parsedModule
    }

    val runtimeModules = parseBitcodeFiles(
            runtimeBitcodeFiles.takeIf { generationState.shouldLinkRuntimeNativeLibraries }.orEmpty()
    )
    val additionalModules = parseBitcodeFiles(additionalBitcodeFiles)
    return LlvmModules(
            runtimeModules.ifNotEmpty { this + generationState.generateRuntimeConstantsModule() } ?: emptyList(),
            additionalModules + listOfNotNull(patchObjCRuntimeModule(generationState))
    )
}

private fun linkAllDependencies(generationState: NativeGenerationState, generatedBitcodeFiles: List<String>) {
    val (runtimeModules, additionalModules) = collectLlvmModules(generationState, generatedBitcodeFiles)
    // TODO: Possibly slow, maybe to a separate phase?
    val optimizedRuntimeModules = linkRuntimeModules(generationState, runtimeModules)

    val modules = optimizedRuntimeModules + additionalModules

    // When the main module `generationState.llvmModule` is very large it is much faster to
    // link all the auxiliary modules together first before linking with the main module.
    val linkedModules = modules.reduceOrNull { acc, module ->
        val failed = llvmLinkModules2(generationState, acc, module)
        if (failed != 0) {
            error("Failed to link ${module.getName()}")
        }
        return@reduceOrNull acc
    }
    linkedModules?.let {
        val failed = llvmLinkModules2(generationState, generationState.llvmModule, it)
        if (failed != 0) {
            error("Failed to link runtime and additional modules into main module")
        }
    }
}

/**
 * Collects LLVM modules for hot reload split compilation.
 *
 * For hot reload, we rely on the pre-compiled stdlib cache (libstdlib-cache.a) which already
 * contains the C++ runtime (MM, GC, alloc) and Kotlin stdlib. However, the stdlib cache does
 * NOT include the launcher module (since it's only needed for executables, not libraries).
 *
 * Architecture:
 * - libstdlib-cache.a: C++ runtime (MM, GC, alloc) + Kotlin stdlib (TypeInfos, etc.)
 * - host.o: ONLY the launcher module (provides Init_and_run_start, Konan_main)
 * - bootstrap.o: User code + Konan_start entry point (loaded via JITLink)
 *
 * The hot reload launcher links against libstdlib-cache.a + host.o to get all infrastructure,
 * then loads bootstrap.o via JITLink at runtime. JITLink resolves symbols (like theStringTypeInfo)
 * from the host process.
 *
 * @return [HotReloadSplitModules] containing:
 *   - hostModules: Only launcher module (runtime comes from libstdlib-cache.a)
 *   - bootstrapModules: Empty (user code is in generationState.llvmModule, handled separately)
 */
internal fun collectModulesForHotReload(generationState: NativeGenerationState, generatedBitcodeFiles: List<String>): HotReloadSplitModules {
    val config = generationState.config
    val runtimeModulesConfig = generationState.runtimeModulesConfig

    // IMPORTANT: Access llvm FIRST to allow it to add dependencies during initialization,
    // BEFORE we access bitcodeToLink which seals the dependencies tracker.
    @Suppress("UNUSED_VARIABLE")
    val additionalProducedBitcodeFiles = generationState.llvm.additionalProducedBitcodeFiles

    // NOTE: This accesses bitcodeToLink which SEALS the dependencies tracker.
    // We need to do this to properly seal the tracker, even though we don't use the result for host.o.
    @Suppress("UNUSED_VARIABLE")
    val bitcodeLibraries = generationState.dependenciesTracker.bitcodeToLink

    // Only include the hot reload launcher module - everything else is in libstdlib-cache.a
    // The hot reload launcher contains JITLink integration for loading bootstrap.o at runtime
    val launcherBitcodeFiles = buildList<String> {
        if (config.produce == CompilerOutputKind.PROGRAM) {
            add(runtimeModulesConfig.absolutePathFor(RuntimeModule.HOT_RELOAD_LAUNCHER))
        }
    }

    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        val parsedModule = parseBitcodeFile(generationState, generationState.messageCollector, generationState.llvmContext, bitcodeFile)
        if (!generationState.shouldUseDebugInfoFromNativeLibs()) {
            LLVMStripModuleDebugInfo(parsedModule)
        }
        parsedModule
    }

    val launcherModules = parseBitcodeFiles(launcherBitcodeFiles)

    println("HOT_RELOAD_SPLIT: Host modules: ${launcherModules.size} (launcher only, runtime from libstdlib-cache.a)")
    println("HOT_RELOAD_SPLIT: Bootstrap will contain user code + Konan_start")

    return HotReloadSplitModules(
            hostModules = launcherModules, // Only launcher - runtime comes from libstdlib-cache.a
            bootstrapModules = emptyList() // User code is in generationState.llvmModule, handled separately
    )
}

internal fun insertAliasToEntryPoint(context: NativeBackendPhaseContext, module: LLVMModuleRef) {
    val config = context.config
    val nomain = config.configuration.get(KonanConfigKeys.NOMAIN) ?: false
    if (config.produce != CompilerOutputKind.PROGRAM || nomain)
        return
    val entryPointName = config.entryPointName
    val entryPoint = LLVMGetNamedFunction(module, entryPointName)
            ?: error("Module doesn't contain `$entryPointName`")
    val programAddressSpace = LLVMGetProgramAddressSpace(module)
    LLVMAddAlias2(module, getGlobalFunctionType(entryPoint), programAddressSpace, entryPoint, "main")
}

/**
 * Exports the hot reload entry point (Konan_main) for JITLink lookup.
 *
 * Since we don't use @exportForCppRuntime annotation, the function is generated with a mangled name.
 * This function finds the mangled Konan_main and creates an alias "Konan_main" with external linkage
 * and default visibility, making it discoverable by the JITLink loader.
 *
 * @param module The LLVM module containing the hot reload entry point
 */
internal fun exportHotReloadEntryPoint(module: LLVMModuleRef) {
    // Find the function that contains "Konan_main" in its mangled name
    // The mangled name will be something like: kfun:kotlin.native.internal.abi#Konan_main(...){}kotlin.Int
    val konanMainFunction = getFunctions(module).find { func ->
        val name = LLVMGetValueName(func)?.toKString().orEmpty()
        name.contains("Konan_main") && LLVMIsDeclaration(func) == 0
    }

    if (konanMainFunction == null) {
        println("HOT_RELOAD_SPLIT: WARNING - Could not find Konan_main function in module")
        return
    }

    val mangledName = LLVMGetValueName(konanMainFunction)?.toKString().orEmpty()
    println("HOT_RELOAD_SPLIT: Found Konan_main with mangled name: $mangledName")

    // Create an alias "Konan_main" that points to the mangled function
    // This allows JITLink to find the function by the unmangled name
    val programAddressSpace = LLVMGetProgramAddressSpace(module)
    val alias = LLVMAddAlias2(module, getGlobalFunctionType(konanMainFunction), programAddressSpace, konanMainFunction, "Konan_main")

    // Set the alias to have external linkage and default visibility
    // This ensures JITLink can find and resolve the symbol
    LLVMSetLinkage(alias, LLVMLinkage.LLVMExternalLinkage)
    LLVMSetVisibility(alias, LLVMVisibility.LLVMDefaultVisibility)

    println("HOT_RELOAD_SPLIT: Created alias 'Konan_main' -> '$mangledName' with external linkage")
}

internal fun linkBitcodeDependencies(generationState: NativeGenerationState,
                                     generatedBitcodeFiles: List<File>) {
    val config = generationState.config
    val produce = config.produce

    val staticFramework = produce == CompilerOutputKind.FRAMEWORK && config.produceStaticFramework
    val swiftExport = config.swiftExport && produce == CompilerOutputKind.STATIC

    if (staticFramework || swiftExport) {
        embedAppleLinkerOptionsToBitcode(generationState.llvm, config)
    }
    linkAllDependencies(generationState, generatedBitcodeFiles.map { it.absoluteFile.normalize().path })

}

/**
 * Data class representing the two LLVM modules for hot reload split compilation.
 */
internal data class HotReloadLlvmModules(
        /**
         * The host module containing ONLY the launcher.
         * Provides: Init_and_run_start, Konan_main, Konan_run_start
         * Runtime (MM, GC, alloc) comes from libstdlib-cache.a.
         */
        val hostModule: LLVMModuleRef,
        /**
         * The bootstrap module containing user code + Konan_start.
         * This becomes bootstrap.o that gets loaded by JITLink at runtime.
         * Konan_run_start (in launcher) calls Konan_start (in bootstrap.o).
         */
        val bootstrapModule: LLVMModuleRef,
)

/**
 * Links bitcode dependencies for hot reload, producing two separate LLVM modules:
 * 1. Host module - contains ONLY the launcher (Init_and_run_start, Konan_main)
 * 2. Bootstrap module - contains user code + Konan_start
 *
 * Architecture for JITLink-based hot reload:
 * - libstdlib-cache.a provides: C++ runtime (MM, GC, alloc) + Kotlin stdlib (TypeInfos, etc.)
 * - host.o provides: launcher module only (Init_and_run_start, Konan_main)
 * - bootstrap.o contains: user code + Konan_start entry point
 *
 * Link command: clang++ -framework Foundation libstdlib-cache.a host.o main.cpp -o host.kexe
 *
 * At runtime, JITLink loads bootstrap.o and resolves symbols from the host process.
 * The launcher's Konan_run_start calls Konan_start which is provided by bootstrap.o.
 */
internal fun linkBitcodeDependenciesForHotReload(
        generationState: NativeGenerationState,
        generatedBitcodeFiles: List<File>
): HotReloadLlvmModules {
    val config = generationState.config
    val produce = config.produce

    val staticFramework = produce == CompilerOutputKind.FRAMEWORK && config.produceStaticFramework
    val swiftExport = config.swiftExport && produce == CompilerOutputKind.STATIC

    if (staticFramework || swiftExport) {
        embedAppleLinkerOptionsToBitcode(generationState.llvm, config)
    }

    val splitModules = collectModulesForHotReload(
            generationState,
            generatedBitcodeFiles.map { it.absoluteFile.normalize().path }
    )

    // Create host module and link the launcher modules into it
    val hostModule = LLVMModuleCreateWithNameInContext("host", generationState.llvmContext)!!

    // Link launcher modules (hot reload launcher) into host
    splitModules.hostModules.forEach { launcherModule ->
        val failed = llvmLinkModules2(generationState, hostModule, launcherModule)
        if (failed != 0) {
            error("Failed to link launcher module into host")
        }
    }

    // Link the ObjC runtime module (KotlinBase -> OutputBase, etc.) into host
    // This is critical for hot reload because bootstrap.o references OutputBase as external
    val objCModule = patchObjCRuntimeModule(generationState)
    if (objCModule != null) {
        println("HOT_RELOAD_SPLIT: Linking ObjC runtime module into host (OutputBase, OutputBoolean, etc.)")
        val failed = llvmLinkModules2(generationState, hostModule, objCModule)
        if (failed != 0) {
            error("Failed to link ObjC runtime module into host")
        }
    } else {
        println("HOT_RELOAD_SPLIT: WARNING - ObjC runtime module not available (objCExport not initialized?)")
    }

    // The bootstrap module is the user code module (generationState.llvmModule)
    // It contains user code + Konan_main entry point
    val bootstrapModule = generationState.llvmModule

    println("HOT_RELOAD_SPLIT: Host module contains launcher + ObjC export classes (runtime from libstdlib-cache.a)")
    println("HOT_RELOAD_SPLIT: Bootstrap module contains user code + Konan_start")

    return HotReloadLlvmModules(
            hostModule = hostModule,
            bootstrapModule = bootstrapModule
    )
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
        println("HOT_RELOAD_SPLIT: No library bitcode to link into bootstrap")
        return
    }

    println("HOT_RELOAD_SPLIT: Linking ${bitcodeFilesToLink.size} library bitcode files into bootstrap")
    bitcodeFilesToLink.forEachIndexed { index, path ->
        val fileName = path.substringAfterLast('/')
        println("HOT_RELOAD_SPLIT:   [$index] $fileName")
    }

    // Parse and link each bitcode file into the bootstrap module
    val bootstrapModule = generationState.llvmModule
    bitcodeFilesToLink.forEach { bitcodeFile ->
        parseAndLinkBitcodeFile(generationState, bootstrapModule, bitcodeFile)
    }

    println("HOT_RELOAD_SPLIT: Library bitcode linking complete")
}

private fun parseAndLinkBitcodeFile(generationState: NativeGenerationState, llvmModule: LLVMModuleRef, path: String) {
    val parsedModule = parseBitcodeFile(generationState, generationState.messageCollector, generationState.llvmContext, path)
    if (!generationState.shouldUseDebugInfoFromNativeLibs()) {
        LLVMStripModuleDebugInfo(parsedModule)
    }
    val failed = llvmLinkModules2(generationState, llvmModule, parsedModule)
    if (failed != 0) {
        throw Error("failed to link $path")
    }
}

private fun embedAppleLinkerOptionsToBitcode(llvm: CodegenLlvmHelpers, config: KonanConfig) {
    fun findEmbeddableOptions(options: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val iterator = options.iterator()
        loop@while (iterator.hasNext()) {
            val option = iterator.next()
            result += when {
                option.startsWith("-l") -> listOf(option)
                option == "-framework" && iterator.hasNext() -> listOf(option, iterator.next())
                else -> break@loop // Ignore the rest.
            }
        }
        return result
    }

    val optionsToEmbed = findEmbeddableOptions(config.platform.configurables.linkerKonanFlags) +
            llvm.dependenciesTracker.allNativeDependencies.flatMap { findEmbeddableOptions(it.linkerOpts) }

    embedLlvmLinkOptions(llvm.llvmContext, llvm.module, optionsToEmbed)
}
