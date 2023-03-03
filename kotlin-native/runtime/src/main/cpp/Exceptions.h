/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_EXCEPTIONS_H
#define RUNTIME_EXCEPTIONS_H

#include "Types.h"

#ifdef __cplusplus
extern "C" {
#endif

// Throws arbitrary exception.
void RUNTIME_NORETURN ThrowException(KRef exception);

void SetKonanTerminateHandler();

void HandleCurrentExceptionWhenLeavingKotlinCode();

RUNTIME_NOTHROW OBJ_GETTER(Kotlin_getExceptionObject, void* holder);

// The functions below are implemented in Kotlin (at package kotlin.native.internal).

// Throws null pointer exception. Context is evaluated from caller's address.
void RUNTIME_NORETURN ThrowNullPointerException();
// Throws array index out of bounds exception.
// Context is evaluated from caller's address.
void RUNTIME_NORETURN ThrowArrayIndexOutOfBoundsException();
// Throws class cast exception.
void RUNTIME_NORETURN ThrowClassCastException(const ObjHeader* instance, const TypeInfo* type_info);
// Throws arithmetic exception.
void RUNTIME_NORETURN ThrowArithmeticException();
// Throws number format exception.
void RUNTIME_NORETURN ThrowNumberFormatException();
// Throws out of memory error.
void RUNTIME_NORETURN ThrowOutOfMemoryError();
// Throws not implemented error.
void RUNTIME_NORETURN ThrowNotImplementedError();
// Throws character coding exception (used in UTF8/UTF16 conversions).
void RUNTIME_NORETURN ThrowCharacterCodingException();
void RUNTIME_NORETURN ThrowIllegalArgumentException();
void RUNTIME_NORETURN ThrowIllegalStateException();
void RUNTIME_NORETURN ThrowInvalidMutabilityException(KConstRef where);
void RUNTIME_NORETURN ThrowIncorrectDereferenceException();
void RUNTIME_NORETURN ThrowFileFailedToInitializeException(KRef reason);
void RUNTIME_NORETURN ThrowIllegalObjectSharingException(KConstNativePtr typeInfo, KConstNativePtr address);
void RUNTIME_NORETURN ThrowFreezingException(KRef toFreeze, KRef blocker);
// Prints out message of Throwable.
void PrintThrowable(KRef);

#ifdef __cplusplus
} // extern "C"
#endif

namespace kotlin {

void ProcessUnhandledException(KRef exception) noexcept;
void RUNTIME_NORETURN TerminateWithUnhandledException(KRef exception) noexcept;

} // namespace kotlin

#endif // RUNTIME_NAMES_H
