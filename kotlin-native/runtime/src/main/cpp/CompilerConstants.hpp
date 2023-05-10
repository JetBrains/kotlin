/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_COMPILER_CONSTANTS_H
#define RUNTIME_COMPILER_CONSTANTS_H

#include <cstdint>
#if __has_include(<string_view>)
#include <string_view>
#elif __has_include(<experimental/string_view>)
// TODO: Remove when wasm32 is gone.
#include <xlocale.h>
#include <experimental/string_view>
namespace std {
using string_view = std::experimental::string_view;
}
#else
#error "No <string_view>"
#endif

#include "Common.h"

// Prefer to use getter functions below. These constants are exposed to simplify the job of the inliner.

/**
 * There are two ways, how compiler can define variables for runtime usage. This one, and the other one with details in source file.
 *
 * This is one is variables defined by generateRuntimeConstantsModule in IrToBitcode.kt. They are eligible for runtime optimizations,
 * and fixed at the point of compiling caches. So use this way for variables, which are heavily used on performance-critical passes or
 * will significantly increase code size, if not eliminated.
 *
 * Don't forget to adjust cache disabling rules and add value to CompilerGenerated.cpp for tests, when adding a new variable.
 */
extern "C" const int32_t Kotlin_needDebugInfo;
extern "C" const int32_t Kotlin_runtimeAssertsMode;
extern "C" const int32_t Kotlin_disableMmap;
extern "C" const char* const Kotlin_runtimeLogs;
extern "C" const int32_t Kotlin_gcSchedulerType;
extern "C" const int32_t Kotlin_freezingEnabled;
extern "C" const int32_t Kotlin_freezingChecksEnabled;

class SourceInfo;

namespace kotlin {
namespace compiler {

// Must match DestroyRuntimeMode in DestroyRuntimeMode.kt
enum class DestroyRuntimeMode : int32_t {
    kLegacy = 0,
    kOnShutdown = 1,
};

// Must match RuntimeAssertsMode in RuntimeAssertsMode.kt
enum class RuntimeAssertsMode : int32_t {
    kIgnore = 0,
    kLog = 1,
    kPanic = 2,
};

// Must match WorkerExceptionHandling in WorkerExceptionHandling.kt
enum class WorkerExceptionHandling : int32_t {
    kLegacy = 0,
    kUseHook = 1,
};

// Must match GCSchedulerType in GCSchedulerType.kt
enum class GCSchedulerType {
    kDisabled = 0,
    kWithTimer = 1,
    kOnSafepoints = 2,
    kAggressive = 3,
};

// Must match AppStateTracking in AppStateTracking.kt
enum class AppStateTracking {
    kDisabled = 0,
    kEnabled = 1,
};

ALWAYS_INLINE inline bool shouldContainDebugInfo() noexcept {
    return Kotlin_needDebugInfo != 0;
}

ALWAYS_INLINE inline RuntimeAssertsMode runtimeAssertsMode() noexcept {
    return static_cast<RuntimeAssertsMode>(Kotlin_runtimeAssertsMode);
}

ALWAYS_INLINE inline bool runtimeAssertsEnabled() noexcept {
    return runtimeAssertsMode() != RuntimeAssertsMode::kIgnore;
}

ALWAYS_INLINE inline bool disableMmap() noexcept {
    return Kotlin_disableMmap != 0;
}

ALWAYS_INLINE inline std::string_view runtimeLogs() noexcept {
    return Kotlin_runtimeLogs == nullptr ? std::string_view() : std::string_view(Kotlin_runtimeLogs);
}

ALWAYS_INLINE inline bool freezingEnabled() noexcept {
    return Kotlin_freezingEnabled != 0;
}

ALWAYS_INLINE inline bool freezingChecksEnabled() noexcept {
    return Kotlin_freezingChecksEnabled != 0;
}

ALWAYS_INLINE inline GCSchedulerType getGCSchedulerType() noexcept {
    return static_cast<compiler::GCSchedulerType>(Kotlin_gcSchedulerType);
}


WorkerExceptionHandling workerExceptionHandling() noexcept;
DestroyRuntimeMode destroyRuntimeMode() noexcept;
bool gcMarkSingleThreaded() noexcept;
bool suspendFunctionsFromAnyThreadFromObjCEnabled() noexcept;
AppStateTracking appStateTracking() noexcept;
int getSourceInfo(void* addr, SourceInfo *result, int result_size) noexcept;
bool mimallocUseDefaultOptions() noexcept;
bool mimallocUseCompaction() noexcept;
bool objcDisposeOnMain() noexcept;

#ifdef KONAN_ANDROID
bool printToAndroidLogcat() noexcept;
#endif

} // namespace compiler
} // namespace kotlin

#endif // RUNTIME_COMPILER_CONSTANTS_H
