/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.runtime

import org.jetbrains.kotlin.backend.konan.AllocationMode
import org.jetbrains.kotlin.backend.konan.GC
import org.jetbrains.kotlin.backend.konan.GCSchedulerType
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.supportsCoreSymbolication
import org.jetbrains.kotlin.konan.target.supportsLibBacktrace

class RuntimeModulesConfig(private val config: KonanConfig) {
    internal val containsDebuggingRuntime: Boolean
        get() = config.debug

    internal val runtimeModules: List<RuntimeModule> = buildList {
        if (containsDebuggingRuntime) add(RuntimeModule.DEBUG)
        add(RuntimeModule.MAIN)
        add(RuntimeModule.MM)
        add(RuntimeModule.ALLOC_COMMON)
        add(RuntimeModule.GC_COMMON)
        add(RuntimeModule.GC_SCHEDULER_COMMON)
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
    }

    private val RuntimeModule.absolutePath: String
        get() = File(config.distribution.defaultNatives(config.target)).child(filename).absolutePath

    internal val runtimeNativeLibraries: List<String> = runtimeModules.map { it.absolutePath }

    internal val launcherNativeLibraries: List<String> = listOf(RuntimeModule.LAUNCHER.absolutePath)

    internal val objCNativeLibrary: String = RuntimeModule.OBJC.absolutePath

    internal val xcTestLauncherNativeLibrary: String = RuntimeModule.XCTEST_LAUNCHER.absolutePath

    internal val exceptionsSupportNativeLibrary: String = RuntimeModule.EXCEPTIONS_SUPPORT.absolutePath

    internal val runtimeCompilerInterface: String = config.configuration.get(KonanConfigKeys.RUNTIME_FILE)
            ?: RuntimeModule.COMPILER_INTERFACE.absolutePath

    internal val debuggingRuntimeNativeLibrary: String = RuntimeModule.DEBUG.absolutePath
}