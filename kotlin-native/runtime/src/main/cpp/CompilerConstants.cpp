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
RUNTIME_WEAK int32_t Kotiln_gcAggressive = 0;
RUNTIME_WEAK int32_t Kotlin_workerExceptionHandling = 0;
RUNTIME_WEAK int32_t Kotlin_freezingEnabled = 1;
RUNTIME_WEAK const Kotlin_getSourceInfo_FunctionType Kotlin_getSourceInfo_Function = nullptr;

ALWAYS_INLINE compiler::DestroyRuntimeMode compiler::destroyRuntimeMode() noexcept {
    return static_cast<compiler::DestroyRuntimeMode>(Kotlin_destroyRuntimeMode);
}

ALWAYS_INLINE bool compiler::gcAggressive() noexcept {
    return Kotiln_gcAggressive != 0;
}

ALWAYS_INLINE compiler::WorkerExceptionHandling compiler::workerExceptionHandling() noexcept {
    return static_cast<compiler::WorkerExceptionHandling>(Kotlin_workerExceptionHandling);
}

ALWAYS_INLINE bool compiler::freezingEnabled() noexcept {
    return Kotlin_freezingEnabled != 0;
}
