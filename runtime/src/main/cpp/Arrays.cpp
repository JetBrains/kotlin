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

#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

template<typename T>
static inline void copyImpl(KConstRef thiz, KInt fromIndex,
                KRef destination, KInt toIndex, KInt count) {
  const ArrayHeader* array = thiz->array();
  ArrayHeader* destinationArray = destination->array();
  if (count < 0 ||
        fromIndex < 0 || count > array->count_ - fromIndex ||
        toIndex < 0 || count > destinationArray->count_ - toIndex) {
      ThrowArrayIndexOutOfBoundsException();
  }

  memmove(PrimitiveArrayAddressOfElementAt<T>(destinationArray, toIndex),
          PrimitiveArrayAddressOfElementAt<T>(array, fromIndex),
             count * sizeof(T));
}

extern "C" {

// TODO: those must be compiler intrinsics afterwards.

// Array.kt
OBJ_GETTER(Kotlin_Array_get, KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  RETURN_OBJ(*ArrayAddressOfElementAt(array, index));
}

void Kotlin_Array_set(KRef thiz, KInt index, KConstRef value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  UpdateRef(ArrayAddressOfElementAt(array, index), value);
}

KInt Kotlin_Array_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

void Kotlin_Array_fillImpl(KRef thiz, KInt fromIndex, KInt toIndex, KRef value) {
  ArrayHeader* array = thiz->array();
  if (fromIndex < 0 || toIndex < fromIndex || toIndex > array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  for (KInt index = fromIndex; index < toIndex; ++index) {
    UpdateRef(ArrayAddressOfElementAt(array, index), value);
  }
}

void Kotlin_Array_copyImpl(KConstRef thiz, KInt fromIndex,
                           KRef destination, KInt toIndex, KInt count) {
  const ArrayHeader* array = thiz->array();
  ArrayHeader* destinationArray = destination->array();
  if (count < 0 ||
      fromIndex < 0 || count > array->count_ - fromIndex ||
      toIndex < 0 || count > destinationArray->count_ - toIndex) {
    ThrowArrayIndexOutOfBoundsException();
  }
  if (fromIndex >= toIndex) {
    for (int index = 0; index < count; index++) {
      UpdateRef(ArrayAddressOfElementAt(destinationArray, toIndex + index),
                      *ArrayAddressOfElementAt(array, fromIndex + index));
    }
  } else {
    for (int index = count - 1; index >= 0; index--) {
      UpdateRef(ArrayAddressOfElementAt(destinationArray, toIndex + index),
                      *ArrayAddressOfElementAt(array, fromIndex + index));
    }
  }
}

// Arrays.kt
KByte Kotlin_ByteArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(array, index);
}

void Kotlin_ByteArray_set(KRef thiz, KInt index, KByte value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ByteArrayAddressOfElementAt(array, index) = value;
}

KInt Kotlin_ByteArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

KChar Kotlin_CharArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KChar>(array, index);
}

void Kotlin_CharArray_set(KRef thiz, KInt index, KChar value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KChar>(array, index) = value;
}

OBJ_GETTER(Kotlin_CharArray_copyOf, KConstRef thiz, KInt newSize) {
  const ArrayHeader* array = thiz->array();
  ArrayHeader* result = ArrayContainer(
      theCharArrayTypeInfo, newSize).GetPlace();
  KInt toCopy = array->count_ < newSize ?  array->count_ : newSize;
  memcpy(
      PrimitiveArrayAddressOfElementAt<KChar>(result, 0),
      PrimitiveArrayAddressOfElementAt<KChar>(array, 0),
      toCopy * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

KInt Kotlin_CharArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

KShort Kotlin_ShortArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KShort>(array, index);
}

void Kotlin_ShortArray_set(KRef thiz, KInt index, KShort value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KShort>(array, index) = value;
}

KInt Kotlin_ShortArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

KInt Kotlin_IntArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KInt>(array, index);
}

void Kotlin_IntArray_set(KRef thiz, KInt index, KInt value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KInt>(array, index) = value;
}

KInt Kotlin_IntArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

void Kotlin_IntArray_fillImpl(KRef thiz, KInt fromIndex, KInt toIndex, KInt value) {
  ArrayHeader* array = thiz->array();
  if (fromIndex < 0 || toIndex < fromIndex || toIndex >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  for (KInt index = fromIndex; index < toIndex; ++index) {
    *PrimitiveArrayAddressOfElementAt<KInt>(array, index) = value;
  }
}

void Kotlin_ByteArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KByte>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_ShortArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KShort>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_CharArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KChar>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_IntArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KInt>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_LongArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KLong>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_FloatArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KFloat>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_DoubleArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KDouble>(thiz, fromIndex, destination, toIndex, count);
}

void Kotlin_BooleanArray_copyImpl(KConstRef thiz, KInt fromIndex,
                              KRef destination, KInt toIndex, KInt count) {
  copyImpl<KBoolean>(thiz, fromIndex, destination, toIndex, count);
}

KLong Kotlin_LongArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {

        ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KLong>(array, index);
}

void Kotlin_LongArray_set(KRef thiz, KInt index, KLong value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KLong>(array, index) = value;
}

KInt Kotlin_LongArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

KFloat Kotlin_FloatArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KFloat>(array, index);
}

void Kotlin_FloatArray_set(KRef thiz, KInt index, KFloat value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KFloat>(array, index) = value;
}

KInt Kotlin_FloatArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

KDouble Kotlin_DoubleArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KDouble>(array, index);
}

void Kotlin_DoubleArray_set(KRef thiz, KInt index, KDouble value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KDouble>(array, index) = value;
}

KInt Kotlin_DoubleArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

KBoolean Kotlin_BooleanArray_get(KConstRef thiz, KInt index) {
  const ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *PrimitiveArrayAddressOfElementAt<KBoolean>(array, index);
}

void Kotlin_BooleanArray_set(KRef thiz, KInt index, KBoolean value) {
  ArrayHeader* array = thiz->array();
  if (static_cast<uint32_t>(index) >= array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *PrimitiveArrayAddressOfElementAt<KBoolean>(array, index) = value;
}

KInt Kotlin_BooleanArray_getArrayLength(KConstRef thiz) {
  const ArrayHeader* array = thiz->array();
  return array->count_;
}

}  // extern "C"
