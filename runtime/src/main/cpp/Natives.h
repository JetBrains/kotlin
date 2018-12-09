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

#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Types.h"
#include "Exceptions.h"

constexpr size_t alignUp(size_t size, size_t alignment) {
  return (size + alignment - 1) & ~(alignment - 1);
}

template <typename T>
inline T* AddressOfElementAt(ArrayHeader* obj, KInt index) {
  int8_t* body = reinterpret_cast<int8_t*>(obj) + alignUp(sizeof(ArrayHeader), alignof(T));
  return reinterpret_cast<T*>(body) + index;
}

template <typename T>
inline const T* AddressOfElementAt(const ArrayHeader* obj, KInt index) {
  const int8_t* body = reinterpret_cast<const int8_t*>(obj) + alignUp(sizeof(ArrayHeader), alignof(T));
  return reinterpret_cast<const T*>(body) + index;
}

// Optimized versions not accessing type info.
inline KByte* ByteArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KByte>(obj, index);
}

inline const KByte* ByteArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KByte>(obj, index);
}

inline KChar* CharArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KChar>(obj, index);
}

inline const KChar* CharArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KChar>(obj, index);
}

inline KInt* IntArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KInt>(obj, index);
}

inline const KInt* IntArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KInt>(obj, index);
}

// Consider aligning of base to sizeof(T).
template <typename T>
inline T* PrimitiveArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<T>(obj, index);
}

template <typename T>
inline const T* PrimitiveArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<T>(obj, index);
}

inline KRef* ArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KRef>(obj, index);
}

inline const KRef* ArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return AddressOfElementAt<KRef>(obj, index);
}

#ifdef __cplusplus
extern "C" {
#endif

// RuntimeUtils.kt.
OBJ_GETTER0(TheEmptyString);

// Any.kt
KBoolean Kotlin_Any_equals(KConstRef thiz, KConstRef other);
KInt Kotlin_Any_hashCode(KConstRef thiz);
OBJ_GETTER(Kotlin_Any_toString, KConstRef thiz);

// Arrays.kt
// TODO: those must be compiler intrinsics afterwards.
OBJ_GETTER(Kotlin_Array_clone, KConstRef thiz);
OBJ_GETTER(Kotlin_Array_get, KConstRef thiz, KInt index);
void Kotlin_Array_set(KRef thiz, KInt index, KConstRef value);
KInt Kotlin_Array_getArrayLength(KConstRef thiz);

OBJ_GETTER(Kotlin_ByteArray_clone, KConstRef thiz);
KByte Kotlin_ByteArray_get(KConstRef thiz, KInt index);
void Kotlin_ByteArray_set(KRef thiz, KInt index, KByte value);
KInt Kotlin_ByteArray_getArrayLength(KConstRef thiz);

OBJ_GETTER(Kotlin_CharArray_clone, KConstRef thiz);
KChar Kotlin_CharArray_get(KConstRef thiz, KInt index);
void Kotlin_CharArray_set(KRef thiz, KInt index, KChar value);
KInt Kotlin_CharArray_getArrayLength(KConstRef thiz);

OBJ_GETTER(Kotlin_IntArray_clone, KConstRef thiz);
KInt Kotlin_IntArray_get(KConstRef thiz, KInt index);
void Kotlin_IntArray_set(KRef thiz, KInt index, KInt value);
KInt Kotlin_IntArray_getArrayLength(KConstRef thiz);

KLong Kotlin_LongArray_get(KConstRef thiz, KInt index);
void Kotlin_LongArray_set(KRef thiz, KInt index, KLong value);

KNativePtr Kotlin_NativePtrArray_get(KConstRef thiz, KInt index);
void Kotlin_NativePtrArray_set(KRef thiz, KInt index, KNativePtr value);
KInt Kotlin_NativePtrArray_getArrayLength(KConstRef thiz);

// io/Console.kt
void Kotlin_io_Console_print(KString message);
void Kotlin_io_Console_println(KString message);
void Kotlin_io_Console_println0();
OBJ_GETTER0(Kotlin_io_Console_readLine);

// Primitives.kt.
OBJ_GETTER(Kotlin_Int_toString, KInt value);

// String.kt
KInt Kotlin_String_hashCode(KString thiz);
KBoolean Kotlin_String_equals(KString thiz, KConstRef other);
KInt Kotlin_String_compareTo(KString thiz, KString other);
KInt Kotlin_String_compareToIgnoreCase(KString thiz, KConstRef other);
KChar Kotlin_String_get(KString thiz, KInt index);
OBJ_GETTER(Kotlin_String_fromUtf8Array, KConstRef array, KInt start, KInt size);
OBJ_GETTER(Kotlin_String_fromCharArray, KConstRef array, KInt start, KInt size);
OBJ_GETTER(Kotlin_String_plusImpl, KString thiz, KString other);
KInt Kotlin_String_getStringLength(KString thiz);
OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex);

OBJ_GETTER0(Kotlin_getCurrentStackTrace);

OBJ_GETTER(Kotlin_getStackTraceStrings, KConstRef stackTrace);

OBJ_GETTER0(Kotlin_native_internal_undefined);

void Kotlin_native_internal_GC_suspend(KRef);
void Kotlin_native_internal_GC_resume(KRef);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H
