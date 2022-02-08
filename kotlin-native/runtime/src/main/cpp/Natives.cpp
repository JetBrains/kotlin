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
#include "KString.h"
#include "StackTrace.hpp"
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

NO_INLINE OBJ_GETTER0(Kotlin_getCurrentStackTrace) {
    kotlin::StackTrace stackTrace;
    {
        // Don't use `kotlin::CallWithThreadState` to avoid messing up callstack.
        kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
        // Skip this function and primary `Throwable` constructor.
        stackTrace = kotlin::StackTrace<>::current(2);
    }

    ObjHolder resultHolder;
    ObjHeader* result = AllocArrayInstance(theNativePtrArrayTypeInfo, stackTrace.size(), resultHolder.slot());
    for (size_t index = 0; index < stackTrace.size(); ++index) {
        Kotlin_NativePtrArray_set(result, index, stackTrace[index]);
    }
    RETURN_OBJ(result);
}

OBJ_GETTER(Kotlin_getStackTraceStrings, KConstRef stackTrace) {
    const KNativePtr* array = PrimitiveArrayAddressOfElementAt<KNativePtr>(stackTrace->array(), 0);
    size_t size = stackTrace->array()->count_;
    auto stackTraceStrings = kotlin::CallWithThreadState<kotlin::ThreadState::kNative>(kotlin::GetStackTraceStrings, kotlin::std_support::span<void* const>(array, size));
    ObjHolder resultHolder;
    ObjHeader* strings = AllocArrayInstance(theArrayTypeInfo, stackTraceStrings.size(), resultHolder.slot());

    for (size_t index = 0; index < stackTraceStrings.size(); ++index) {
        ObjHolder holder;
        CreateStringFromCString(stackTraceStrings[index].c_str(), holder.slot());
        UpdateHeapRef(ArrayAddressOfElementAt(strings->array(), index), holder.obj());
    }

    RETURN_OBJ(strings);
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
