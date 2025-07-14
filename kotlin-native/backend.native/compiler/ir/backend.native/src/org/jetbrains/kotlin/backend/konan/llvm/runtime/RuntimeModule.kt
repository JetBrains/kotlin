/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.runtime

// NOTE: The list of modules is duplicated in kotlin-native/runtime/build.gradle.kts
enum class RuntimeModule(val filename: String) {
    COMPILER_INTERFACE("compiler_interface.bc"),
    MAIN("runtime.bc"),
    DEBUG("debug.bc"),
    MM("mm.bc"),
    ALLOC_COMMON("common_alloc.bc"),
    ALLOC_LEGACY("legacy_alloc.bc"),
    ALLOC_STD("std_alloc.bc"),
    ALLOC_CUSTOM("custom_alloc.bc"),
    GC_COMMON("common_gc.bc"),
    GC_NOOP("noop_gc.bc"),
    GC_STOP_THE_WORLD_MARK_AND_SWEEP("same_thread_ms_gc.bc"),
    GC_PARALLEL_MARK_CONCURRENT_SWEEP("pmcs_gc.bc"),
    GC_CONCURRENT_MARK_AND_SWEEP("concurrent_ms_gc.bc"),
    GC_SCHEDULER_COMMON("common_gcScheduler.bc"),
    GC_SCHEDULER_MANUAL("manual_gcScheduler.bc"),
    GC_SCHEDULER_ADAPTIVE("adaptive_gcScheduler.bc"),
    GC_SCHEDULER_AGGRESSIVE("aggressive_gcScheduler.bc"),
    SOURCE_INFO_CORE_SYMBOLICATION("source_info_core_symbolication.bc"),
    LIBBACKTRACE("libbacktrace.bc"),
    SOURCE_INFO_LIBBACKTRACE("source_info_libbacktrace.bc"),
    EXTERNAL_CALLS_CHECKER_IMPL("impl_externalCallsChecker.bc"),
    EXTERNAL_CALLS_CHECKER_NOOP("noop_externalCallsChecker.bc"),
    LAUNCHER("launcher.bc"),
    OBJC("objc.bc"),
    XCTEST_LAUNCHER("xctest_launcher.bc"),
    EXCEPTIONS_SUPPORT("exceptionsSupport.bc"),
    BREAKPAD("breakpad.bc"),
    CRASH_HANDLER_IMPL("impl_crashHandler.bc"),
    CRASH_HANDLER_NOOP("noop_crashHandler.bc"),
    HOT_RELOAD("hot_reload.bc"),
}