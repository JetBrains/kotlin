/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CompilerConstants.hpp"

#include "Common.h"
#include "SourceInfo.h"

using namespace kotlin;

// These are defined by overrideRuntimeGlobals in IrToBitcode.kt
RUNTIME_WEAK int32_t Kotlin_destroyRuntimeMode = 1;
RUNTIME_WEAK int32_t Kotlin_gcAggressive = 0;
RUNTIME_WEAK int32_t Kotlin_gcSchedulerType = 2;
RUNTIME_WEAK int32_t Kotlin_workerExceptionHandling = 0;
RUNTIME_WEAK int32_t Kotlin_freezingEnabled = 1;
RUNTIME_WEAK const Kotlin_getSourceInfo_FunctionType Kotlin_getSourceInfo_Function = nullptr;
#ifdef KONAN_ANDROID
RUNTIME_WEAK int32_t Kotlin_printToAndroidLogcat = 1;
#endif

ALWAYS_INLINE compiler::DestroyRuntimeMode compiler::destroyRuntimeMode() noexcept {
    return static_cast<compiler::DestroyRuntimeMode>(Kotlin_destroyRuntimeMode);
}

ALWAYS_INLINE bool compiler::gcAggressive() noexcept {
    return Kotlin_gcAggressive != 0;
}

ALWAYS_INLINE compiler::WorkerExceptionHandling compiler::workerExceptionHandling() noexcept {
    return static_cast<compiler::WorkerExceptionHandling>(Kotlin_workerExceptionHandling);
}

ALWAYS_INLINE bool compiler::freezingEnabled() noexcept {
    return Kotlin_freezingEnabled != 0;
}

ALWAYS_INLINE compiler::GCSchedulerType compiler::getGCSchedulerType() noexcept {
    return static_cast<compiler::GCSchedulerType>(Kotlin_gcSchedulerType);
}

#ifdef KONAN_ANDROID
ALWAYS_INLINE bool compiler::printToAndroidLogcat() noexcept {
    return Kotlin_printToAndroidLogcat != 0;
}
#endif
