/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CompilerConstants.hpp"

#include "Common.h"
#include "SourceInfo.h"

using namespace kotlin;

using Kotlin_getSourceInfo_FunctionType = int(*)(void * /*addr*/, SourceInfo* /*result*/, int /*result_size*/);

/**
 * There are two ways, how compiler can define variables for runtime usage. This one, and the other one with details in header file.
 *
 * This is one is variables defined by overrideRuntimeGlobals in IrToBitcode.kt. They are *not* eligible for runtime optimizations,
 * but can be changed after compiling caches. So use this way for variables, which will be rarely accessed.
 */
RUNTIME_WEAK int32_t Kotlin_destroyRuntimeMode = 1;
RUNTIME_WEAK int32_t Kotlin_gcMarkSingleThreaded = 1;
RUNTIME_WEAK int32_t Kotlin_workerExceptionHandling = 0;
RUNTIME_WEAK int32_t Kotlin_suspendFunctionsFromAnyThreadFromObjC = 0;
RUNTIME_WEAK Kotlin_getSourceInfo_FunctionType Kotlin_getSourceInfo_Function = nullptr;
#ifdef KONAN_ANDROID
RUNTIME_WEAK int32_t Kotlin_printToAndroidLogcat = 1;
#endif
// Keep it 0 even when the compiler defaults to 1: if the overriding mechanism breaks, keeping it disabled is safer.
RUNTIME_WEAK int32_t Kotlin_appStateTracking = 0;
RUNTIME_WEAK int32_t Kotlin_mimallocUseDefaultOptions = 1;
RUNTIME_WEAK int32_t Kotlin_mimallocUseCompaction = 0;
RUNTIME_WEAK int32_t Kotlin_objcDisposeOnMain = 0;

ALWAYS_INLINE compiler::DestroyRuntimeMode compiler::destroyRuntimeMode() noexcept {
    return static_cast<compiler::DestroyRuntimeMode>(Kotlin_destroyRuntimeMode);
}

ALWAYS_INLINE bool compiler::gcMarkSingleThreaded() noexcept {
    return Kotlin_gcMarkSingleThreaded != 0;
}

ALWAYS_INLINE compiler::WorkerExceptionHandling compiler::workerExceptionHandling() noexcept {
    return static_cast<compiler::WorkerExceptionHandling>(Kotlin_workerExceptionHandling);
}


ALWAYS_INLINE bool compiler::suspendFunctionsFromAnyThreadFromObjCEnabled() noexcept {
    return Kotlin_suspendFunctionsFromAnyThreadFromObjC != 0;
}

ALWAYS_INLINE compiler::AppStateTracking compiler::appStateTracking() noexcept {
    return static_cast<compiler::AppStateTracking>(Kotlin_appStateTracking);
}

#ifdef KONAN_ANDROID
ALWAYS_INLINE bool compiler::printToAndroidLogcat() noexcept {
    return Kotlin_printToAndroidLogcat != 0;
}
#endif

ALWAYS_INLINE int compiler::getSourceInfo(void* addr, SourceInfo *result, int result_size) noexcept {
    if (Kotlin_getSourceInfo_Function == nullptr) {
        return 0;
    } else {
        return Kotlin_getSourceInfo_Function(addr, result, result_size);
    }
}

ALWAYS_INLINE bool compiler::mimallocUseDefaultOptions() noexcept {
    return Kotlin_mimallocUseDefaultOptions != 0;
}

ALWAYS_INLINE bool compiler::mimallocUseCompaction() noexcept {
    return Kotlin_mimallocUseCompaction != 0;
}

ALWAYS_INLINE bool compiler::objcDisposeOnMain() noexcept {
    return Kotlin_objcDisposeOnMain != 0;
}
