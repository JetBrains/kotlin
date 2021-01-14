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

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <limits>
#include <type_traits>

#include "KAssert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// Any.kt
KBoolean Kotlin_Any_equals(KConstRef thiz, KConstRef other) {
  return thiz == other;
}

KInt Kotlin_Any_hashCode(KConstRef thiz) {
  // Here we will use different mechanism for stable hashcode, using meta-objects
  // if moving collector will be used.
  return reinterpret_cast<uintptr_t>(thiz);
}

OBJ_GETTER(Kotlin_getStackTraceStrings, KConstRef stackTrace) {
  RETURN_RESULT_OF(GetStackTraceStrings, stackTrace);
}

// TODO: consider handling it with compiler magic instead.
OBJ_GETTER0(Kotlin_native_internal_undefined) {
  RETURN_OBJ(nullptr);
}

void* Kotlin_interop_malloc(KLong size, KInt align) {
  if (size < 0 || static_cast<std::make_unsigned_t<decltype(size)>>(size) > std::numeric_limits<size_t>::max()) {
    return nullptr;
  }
  RuntimeAssert(align > 0, "Unsupported alignment");
  RuntimeAssert((align & (align - 1)) == 0, "Alignment must be power of two");

  void* result = konan::calloc_aligned(1, size, align);
  if ((reinterpret_cast<uintptr_t>(result) & (align - 1)) != 0) {
    // Unaligned!
    RuntimeAssert(false, "unsupported alignment");
  }

  return result;
}

void Kotlin_interop_free(void* ptr) {
  konan::free(ptr);
}

void Kotlin_system_exitProcess(KInt status) {
  konan::exit(status);
}

const void* Kotlin_Any_getTypeInfo(KConstRef obj) {
  return obj->type_info();
}

void Kotlin_CPointer_CopyMemory(KNativePtr to, KNativePtr from, KInt count) {
  memcpy(to, from, count);
}

}  // extern "C"
