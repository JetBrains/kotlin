/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef RUNTIME_EXCEPTIONS_H
#define RUNTIME_EXCEPTIONS_H

#include "Types.h"

#ifdef __cplusplus
extern "C" {
#endif

// Throws arbitrary exception.
void ThrowException(KRef exception);

void SetKonanTerminateHandler();

void HandleCurrentExceptionForCInterop();

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
void RUNTIME_NORETURN ThrowFileFailedToInitializeException();
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
