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

// Type for runtime representation of Konan object.
enum Konan_RuntimeType {
  INVALID = 0,
  OBJECT = 1,
  INT8 = 2,
  INT16 = 3,
  INT32 = 4,
  INT64 = 5,
  FLOAT32 = 6,
  FLOAT64 = 7
};

#ifdef __cplusplus
extern "C" {
#endif

// Get memory buffer where debugger can put data in Konan app process.
RUNTIME_USED
char* Konan_DebugBuffer();

// Get size of memory buffer where debugger can put data in Konan app process.
RUNTIME_USED
int Konan_DebugBufferSize();

// Put string representation of an object to the provided buffer.
RUNTIME_USED
int Konan_DebugObjectToUtf8Array(KRef obj, char* buffer, int bufferSize);

// Print to console string representation of an object.
RUNTIME_USED
int Konan_DebugPrint(KRef obj);

// Compute type of field or an array element at the index, or 0, if incorrect,
// see Konan_RuntimeType.
// TODO: currently, only object fields are supported, will be fixed soon.
RUNTIME_USED int Konan_DebugGetFieldType(KRef obj, int index);

// Compute address of field or an array element at the index, or null, if incorrect.
// TODO: currently, only object fields are supported, will be fixed soon.
RUNTIME_USED void* Konan_DebugGetFieldAddress(KRef obj, int index);

#ifdef __cplusplus
}
#endif

#endif  // !KONAN_NO_DEBUG_API

#endif  // RUNTIME_KDEBUG_H
