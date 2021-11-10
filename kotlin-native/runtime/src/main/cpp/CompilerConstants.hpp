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

// These are defined by setRuntimeConstGlobals in IrToBitcode.kt
extern "C" const int32_t KonanNeedDebugInfo;
extern "C" const int32_t Kotlin_runtimeAssertsMode;
extern "C" const char* const Kotlin_runtimeLogs;
class SourceInfo;
using Kotlin_getSourceInfo_FunctionType = int(*)(void * /*addr*/, SourceInfo* /*result*/, int /*result_size*/);
extern "C" const Kotlin_getSourceInfo_FunctionType Kotlin_getSourceInfo_Function;

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
    kOnSafepoints = 2
};

DestroyRuntimeMode destroyRuntimeMode() noexcept;

bool gcAggressive() noexcept;

ALWAYS_INLINE inline bool shouldContainDebugInfo() noexcept {
    return KonanNeedDebugInfo != 0;
}

ALWAYS_INLINE inline RuntimeAssertsMode runtimeAssertsMode() noexcept {
    return static_cast<RuntimeAssertsMode>(Kotlin_runtimeAssertsMode);
}

WorkerExceptionHandling workerExceptionHandling() noexcept;

ALWAYS_INLINE inline std::string_view runtimeLogs() noexcept {
    return Kotlin_runtimeLogs == nullptr ? std::string_view() : std::string_view(Kotlin_runtimeLogs);
}

bool freezingEnabled() noexcept;

ALWAYS_INLINE inline int getSourceInfo(void* addr, SourceInfo *result, int result_size) {
    if (Kotlin_getSourceInfo_Function == nullptr) {
        return 0;
    } else {
        return Kotlin_getSourceInfo_Function(addr, result, result_size);
    }
}

compiler::GCSchedulerType getGCSchedulerType() noexcept;

#ifdef KONAN_ANDROID
bool printToAndroidLogcat() noexcept;
#endif

} // namespace compiler
} // namespace kotlin

#endif // RUNTIME_COMPILER_CONSTANTS_H
