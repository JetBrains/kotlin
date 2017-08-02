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

#include "Assert.h"
#include "Memory.h"
#include "Natives.h"
#include "Porting.h"
#include "Types.h"

#ifndef KONAN_NO_DEBUG_API

extern "C" OBJ_GETTER(KonanObjectToUtf8Array, KRef object);

namespace {

char debugBuffer[4096];

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
  buffer[toCopy + 1] = '\0';
  return toCopy + 1;
}

RUNTIME_USED KInt Konan_DebugPrint(KRef obj) {
  KInt size = Konan_DebugObjectToUtf8Array(obj, Konan_DebugBuffer(), Konan_DebugBufferSize());
  if (size > 1)
    konan::consoleWriteUtf8(Konan_DebugBuffer(), size - 1);
  return 0;
}

RUNTIME_USED int Konan_DebugGetFieldType(KRef obj, int index) {
  if (obj == nullptr || index < 0) return Konan_RuntimeType::INVALID;
  auto typeInfo = obj->type_info();
  if (typeInfo->instanceSize_ < 0) {
    // Arrays.
    if (index >= obj->array()->count_) return Konan_RuntimeType::INVALID;
    if (typeInfo == theArrayTypeInfo) {
      return Konan_RuntimeType::OBJECT;
    }
    // TODO: support other array types.
    return Konan_RuntimeType::INVALID;
  }

  // TODO: support primitive type fields as well!
  if (index >= typeInfo->objOffsetsCount_) return Konan_RuntimeType::INVALID;
  return Konan_RuntimeType::OBJECT;
}

RUNTIME_USED void* Konan_DebugGetFieldAddress(KRef obj, int index) {
  if (obj == nullptr || index < 0) return nullptr;
  auto typeInfo = obj->type_info();
  if (typeInfo->instanceSize_ < 0) {
    // Arrays.
    if (index >= obj->array()->count_) return nullptr;
    if (typeInfo == theArrayTypeInfo) {
      return ArrayAddressOfElementAt(obj->array(), index);
    }
    // TODO: support other array types.
    return nullptr;
  }
  // TODO: support primitive type fields as well!
  if (index >= typeInfo->objOffsetsCount_) return nullptr;
  return reinterpret_cast<uint8_t*>(obj + 1) + typeInfo->objOffsets_[index];
}

}  // extern "C"

#endif // !KONAN_NO_DEBUG_API
