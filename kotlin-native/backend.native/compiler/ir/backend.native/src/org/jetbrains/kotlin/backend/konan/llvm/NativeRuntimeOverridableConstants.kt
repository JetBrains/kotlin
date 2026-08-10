/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

/**
 * Set of overridable variables used to customize runtime behavior.
 *
 * Note that there are two ways of defining variables for runtime usage. The other way can is located within
 * [NativeRuntimeConstants] file.
 *
 * Those variables are **not eligible** for runtime optimizations, but they can be changed after compiling caches.
 * Therefore, use this way of variables when they are rarely accessed.
 *
 * Definitions of those variables within runtime code can be found in `CompilerConstants.cpp` file.
 */
object NativeRuntimeOverridableConstants {
    const val GC_MUTATORS_COOPERATE: String = "Kotlin_gcMutatorsCooperate"
    const val AUX_GC_THREADS: String = "Kotlin_auxGCThreads"
    const val CONCURRENT_MARK_MAX_ITERATIONS: String = "Kotlin_concurrentMarkMaxIterations"
    const val SUSPEND_FUNCTIONS_FROM_ANY_THREAD_FROM_OBJC: String = "Kotlin_suspendFunctionsFromAnyThreadFromObjC"

    const val GET_SOURCE_INFO_LIB_BACKTRACE: String = "Kotlin_getSourceInfo_libbacktrace"
    const val GET_SOURCE_INFO_CORE_SYMBOLICATION: String = "Kotlin_getSourceInfo_core_symbolication"
    const val GET_SOURCE_INFO_FUNCTION: String = "Kotlin_getSourceInfo_Function"

    const val CORE_SYMBOLICATION_USE_ONLY_KOTLIN_IMAGE: String = "Kotlin_CoreSymbolication_useOnlyKotlinImage"
    const val PRINT_TO_ANDROID_LOGCAT: String = "Kotlin_printToAndroidLogcat"
    const val APP_STATE_TRACKING: String = "Kotlin_appStateTracking"

    const val OBJC_DISPOSE_ON_MAIN: String = "Kotlin_objcDisposeOnMain"
    const val OBJC_DSIPOSE_WITH_RUN_LOOP: String = "Kotlin_objcDisposeWithRunLoop"

    const val ENABLE_SAFEPOINT_SIGNPOSTS: String = "Kotlin_enableSafepointSignposts"
    const val SWIFT_EXPORT: String = "Kotlin_swiftExport"
    const val LATIN1_STRINGS: String = "Kotlin_latin1Strings"
    const val MMAP_TAG: String = "Kotlin_mmapTag"

    const val MINIDUMP_LOCATION: String = "Kotlin_minidumpLocation"
    const val MINIDUMP_ON_SIGTERM: String = "Kotlin_minidumpOnSIGTERM"

    const val RUNTIME_LOGS: String = "Kotlin_runtimeLogs"
}
