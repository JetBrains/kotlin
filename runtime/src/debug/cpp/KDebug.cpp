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

#include "KDebug.h"

#include <string.h>

#include "KAssert.h"
#include "Memory.h"
#include "Natives.h"
#include "Porting.h"
#include "Types.h"

#ifndef KONAN_NO_DEBUG_API

extern "C" OBJ_GETTER(KonanObjectToUtf8Array, KRef object);

namespace {

char debugBuffer[4096];

constexpr int runtimeTypeSize[] = {
    -1,                  // INVALID
    sizeof(ObjHeader*),  // OBJECT
    1,                   // INT8
    2,                   // INT16
    4,                   // INT32
    8,                   // INT64
    4,                   // FLOAT32
    8,                   // FLOAT64
    sizeof(void*),       // NATIVE_PTR
    1                    // BOOLEAN
};

constexpr int runtimeTypeAlignment[] = {
    -1,                  // INVALID
    alignof(ObjHeader*), // OBJECT
    alignof(int8_t),     // INT8
    alignof(int16_t),    // INT16
    alignof(int32_t),    // INT32
    alignof(int64_t),    // INT64
    alignof(float),      // FLOAT32
    alignof(double),     // FLOAT64
    alignof(void*),      // NATIVE_PTR
    1                    // BOOLEAN
};

}  // namespace

extern "C" {

// Buffer that can be used by debugger for inspections.
RUNTIME_USED char* Konan_DebugBuffer() {
  return debugBuffer;
}

RUNTIME_USED int Konan_DebugBufferSize() {
  return sizeof(debugBuffer);
}

// Auxilary function which can be called by developer/debugger to inspect an object.
RUNTIME_USED KInt Konan_DebugObjectToUtf8Array(KRef obj, char* buffer, KInt bufferSize) {
  ObjHolder stringHolder;
  auto data = KonanObjectToUtf8Array(obj, stringHolder.slot())->array();
  if (data == nullptr) return 0;
  KInt toCopy = data->count_ > bufferSize - 1 ? bufferSize - 1 : data->count_;
  ::memcpy(buffer, ByteArrayAddressOfElementAt(data, 0), toCopy);
  buffer[toCopy] = '\0';
  return toCopy + 1;
}

RUNTIME_USED KInt Konan_DebugPrint(KRef obj) {
  KInt size = Konan_DebugObjectToUtf8Array(obj, Konan_DebugBuffer(), Konan_DebugBufferSize());
  if (size > 1)
    konan::consoleWriteUtf8(Konan_DebugBuffer(), size - 1);
  return 0;
}

RUNTIME_USED int Konan_DebugIsArray(KRef obj) {
  return obj == nullptr || IsArray(obj) ? 1 : 0;
}

RUNTIME_USED int Konan_DebugGetFieldCount(KRef obj) {
  if (obj == nullptr)
    return 0;

  auto typeInfo = obj->type_info();
  auto extendedTypeInfo = typeInfo->extendedInfo_;

  if (extendedTypeInfo == nullptr)
    return 0;

  if (IsArray(obj))
    return obj->array()->count_;

  return extendedTypeInfo->fieldsCount_;
}


RUNTIME_USED int Konan_DebugGetFieldType(KRef obj, int index) {
  if (obj == nullptr || index < 0)
    return Konan_RuntimeType::RT_INVALID;

  auto typeInfo = obj->type_info();
  auto extendedTypeInfo = typeInfo->extendedInfo_;

  if (extendedTypeInfo == nullptr)
    return Konan_RuntimeType::RT_INVALID;

  if (extendedTypeInfo->fieldsCount_ < 0)
    return -extendedTypeInfo->fieldsCount_;

  if (index >= extendedTypeInfo->fieldsCount_)
    return Konan_RuntimeType::RT_INVALID;

  return extendedTypeInfo->fieldTypes_[index];
}

RUNTIME_USED void* Konan_DebugGetFieldAddress(KRef obj, int index) {
  if (obj == nullptr || index < 0)
    return nullptr;

  auto typeInfo = obj->type_info();
  auto extendedTypeInfo = typeInfo->extendedInfo_;

  if (extendedTypeInfo == nullptr)
    return nullptr;

   if (extendedTypeInfo->fieldsCount_ < 0) {
     if (index > obj->array()->count_)
        return nullptr;

      int32_t typeIndex = -extendedTypeInfo->fieldsCount_;
      return reinterpret_cast<uint8_t*>(obj->array())
          + alignUp(sizeof(struct ArrayHeader), runtimeTypeAlignment[typeIndex])
          + index * runtimeTypeSize[typeIndex];
   }

   if (index >= extendedTypeInfo->fieldsCount_)
     return nullptr;

   return reinterpret_cast<uint8_t*>(obj) + extendedTypeInfo->fieldOffsets_[index];
}

// Compute address of field or an array element at the index, or null, if incorrect.
RUNTIME_USED const char* Konan_DebugGetFieldName(KRef obj, int index) {
  if (obj == nullptr || index < 0)
    return nullptr;

  auto typeInfo = obj->type_info();
  auto extendedTypeInfo = typeInfo->extendedInfo_;

  if (extendedTypeInfo == nullptr)
    return nullptr;

  // For arrays, field name makes not much sense.
  if (extendedTypeInfo->fieldsCount_ < 0)
    return "";

  if (index >= extendedTypeInfo->fieldsCount_)
    return nullptr;

  return extendedTypeInfo->fieldNames_[index];
}

}  // extern "C"

#endif // !KONAN_NO_DEBUG_API
