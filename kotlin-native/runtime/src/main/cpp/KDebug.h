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

#ifndef RUNTIME_KDEBUG_H
#define RUNTIME_KDEBUG_H

#include "Common.h"
#include "Memory.h"
#include "Types.h"
#include "TypeInfo.h"

#ifndef KONAN_NO_DEBUG_API

#ifdef __cplusplus
extern "C" {
#endif

// PLEASE READ: please do not alter signatures of the existing functions, and when adding
// the new function please do not forget to add new functions into the debug operations list.

// Get memory buffer where debugger can put data in Konan app process.
RUNTIME_USED RUNTIME_WEAK
char* Konan_DebugBuffer();

// Same, but runtime-specific.
RUNTIME_USED RUNTIME_WEAK
char* Konan_DebugBufferWithObject(KRef obj);

// Get size of memory buffer where debugger can put data in Konan app process.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugBufferSize();

// Same, but runtime-specific.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugBufferSizeWithObject(KRef obj);

// Put string representation of an object to the provided buffer.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugObjectToUtf8Array(KRef obj, char* buffer, int32_t bufferSize);

// Print to console string representation of an object.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugPrint(KRef obj);

// Returns 1 if obj refers to an array, string or binary blob and 0 otherwise.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugIsArray(KRef obj);

RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugIsInstance(KRef obj, const TypeInfo* typeInfo);

// Returns number of fields in an objects, or elements in an array.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugGetFieldCount(KRef obj);

// Compute type of field or an array element at the index, or 0, if incorrect,
// see Konan_RuntimeType.
RUNTIME_USED RUNTIME_WEAK
int32_t Konan_DebugGetFieldType(KRef obj, int32_t index);

// Compute address of field or an array element at the index, or null, if incorrect.
RUNTIME_USED RUNTIME_WEAK
void* Konan_DebugGetFieldAddress(KRef obj, int32_t index);

// Compute address of field or an array element at the index, or null, if incorrect.
RUNTIME_USED RUNTIME_WEAK
const char* Konan_DebugGetFieldName(KRef obj, int32_t index);

// Returns name of type.
RUNTIME_USED RUNTIME_WEAK
const char* Konan_DebugGetTypeName(KRef obj);

/**
 * Given an object finds debugger interface operation suitable for manipulation with this object.
 * Important for cases where multiple K/N runtimes coexist in the same address space and debugger
 * doesn't know which debug operation to use on particular instance.
 */
RUNTIME_USED RUNTIME_WEAK
void* Konan_DebugGetOperation(KRef obj, /* Konan_DebugOperation */ int32_t operation);

#ifdef __cplusplus
}
#endif

#endif  // !KONAN_NO_DEBUG_API

#endif  // RUNTIME_KDEBUG_H
