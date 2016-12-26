#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Types.h"

inline void* AddressOfElementAt(ArrayHeader* obj, int32_t index) {
  // Instance size is negative.
  return reinterpret_cast<uint8_t*>(obj + 1) -
      obj->type_info()->instanceSize_ * index;
}

inline const void* AddressOfElementAt(const ArrayHeader* obj, int32_t index) {
  // Instance size is negative.
  return reinterpret_cast<const uint8_t*>(obj + 1) -
      obj->type_info()->instanceSize_ * index;
}

// Optimized versions not accessing type info.
inline KByte* ByteArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KByte*>(obj + 1) + index;
}

inline const KByte* ByteArrayAddressOfElementAt(
    const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KByte*>(obj + 1) + index;
}

// Consider aligning of base to sizeof(T).
template <typename T>
inline T* PrimitiveArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<T*>(obj + 1) + index;
}

template <typename T>
inline const T* PrimitiveArrayAddressOfElementAt(
    const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const T*>(obj + 1) + index;
}

inline KRef* ArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KRef*>(obj + 1) + index;
}

inline const KRef* ArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KRef*>(obj + 1) + index;
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
KChar Kotlin_String_get(KString thiz, KInt index);
OBJ_GETTER(Kotlin_String_fromUtf8Array, KConstRef array, KInt start, KInt size);
OBJ_GETTER(Kotlin_String_fromCharArray, KConstRef array, KInt start, KInt size);
OBJ_GETTER(Kotlin_String_plusImpl, KString thiz, KString other);
KInt Kotlin_String_getStringLength(KString thiz);
OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex);

OBJ_GETTER0(Kotlin_getCurrentStackTrace);

OBJ_GETTER0(Kotlin_konan_internal_undefined);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H
