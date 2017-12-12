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
void ThrowNullPointerException();
// Throws array index out of bounds exception.
// Context is evaluated from caller's address.
void ThrowArrayIndexOutOfBoundsException();
// Throws class cast exception.
void ThrowClassCastException();
// Throws arithmetic exception.
void ThrowArithmeticException();
// Throws number format exception.
void ThrowNumberFormatException();
// Throws out of memory error.
void ThrowOutOfMemoryError();
// Throws not implemented error.
void ThrowNotImplementedError();
// Throws illegal character conversion exception (used in UTF8/UTF16 conversions).
void ThrowIllegalCharacterConversionException();
// Prints out mesage of Throwable.
void PrintThrowable(KRef);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_NAMES_H
