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

// Returns current stacktrace as Array<String>.
OBJ_GETTER0(GetCurrentStackTrace);

// Throws arbitrary exception.
void ThrowException(KRef exception);

void SetKonanTerminateHandler();

// The functions below are implemented in Kotlin (at package konan.internal).

// Throws null pointer exception. Context is evaluated from caller's address.
void RUNTIME_NORETURN ThrowNullPointerException();
// Throws array index out of bounds exception.
// Context is evaluated from caller's address.
void RUNTIME_NORETURN ThrowArrayIndexOutOfBoundsException();
// Throws class cast exception.
void RUNTIME_NORETURN ThrowClassCastException();
// Throws arithmetic exception.
void RUNTIME_NORETURN ThrowArithmeticException();
// Throws number format exception.
void RUNTIME_NORETURN ThrowNumberFormatException();
// Throws out of memory error.
void RUNTIME_NORETURN ThrowOutOfMemoryError();
// Throws not implemented error.
void RUNTIME_NORETURN ThrowNotImplementedError();
// Throws illegal character conversion exception (used in UTF8/UTF16 conversions).
void RUNTIME_NORETURN ThrowIllegalCharacterConversionException();
void RUNTIME_NORETURN ThrowIllegalArgumentException();
void RUNTIME_NORETURN ThrowInvalidMutabilityException(KConstRef where);
// Prints out mesage of Throwable.
void PrintThrowable(KRef);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_NAMES_H
