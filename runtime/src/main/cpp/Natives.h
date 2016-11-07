#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Memory.h"
#include "Types.h"

typedef uint8_t KBool;
typedef uint8_t KByte;
typedef uint16_t KChar;
// Note that it is signed.
typedef int32_t KInt;

typedef ObjHeader* KRef;

// Optimized versions not accessing type info.
inline KByte* ByteArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KByte*>(obj + 1) + index;
}

inline const KByte* ByteArrayAddressOfElementAt(
    const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KByte*>(obj + 1) + index;
}

inline KChar* CharArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KChar*>(obj + 1) + index;
}

inline const KChar* CharArrayAddressOfElementAt(
    const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KChar*>(obj + 1) + index;
}

inline KInt* IntArrayAddressOfElementAt(ArrayHeader* obj, KInt index) {
  return reinterpret_cast<KInt*>(obj + 1) + index;
}

inline const KInt* IntArrayAddressOfElementAt(const ArrayHeader* obj, KInt index) {
  return reinterpret_cast<const KInt*>(obj + 1) + index;
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

// Any.kt
KBool Kotlin_Any_equals(const ObjHeader* thiz, const ObjHeader* other);
KInt Kotlin_Any_hashCode(const ObjHeader* thiz);
ArrayHeader* Kotlin_Any_toString(const ObjHeader* thiz);

// Arrays.kt
// TODO: those must be compiler intrinsics afterwards.
ArrayHeader* Kotlin_Array_clone(const ArrayHeader* thiz);
KRef Kotlin_Array_get(const ArrayHeader* thiz, KInt index);
void Kotlin_Array_set(ArrayHeader* thiz, KInt index, KRef value);
KInt Kotlin_Array_getArrayLength(const ArrayHeader* thiz);

ArrayHeader* Kotlin_ByteArray_clone(const ArrayHeader* thiz);
KByte Kotlin_ByteArray_get(const ArrayHeader* thiz, KInt index);
void Kotlin_ByteArray_set(ArrayHeader* thiz, KInt index, KByte value);
KInt Kotlin_ByteArray_getArrayLength(const ArrayHeader* thiz);

ArrayHeader* Kotlin_CharArray_clone(const ArrayHeader* thiz);
KChar Kotlin_CharArray_get(const ArrayHeader* thiz, KInt index);
void Kotlin_CharArray_set(ArrayHeader* thiz, KInt index, KChar value);
KInt Kotlin_CharArray_getArrayLength(const ArrayHeader* thiz);

ArrayHeader* Kotlin_IntArray_clone(const ArrayHeader* thiz);
KInt Kotlin_IntArray_get(const ArrayHeader* thiz, KInt index);
void Kotlin_IntArray_set(ArrayHeader* thiz, KInt index, KInt value);
KInt Kotlin_IntArray_getArrayLength(const ArrayHeader* thiz);

// io/Console.kt
void Kotlin_io_Console_print(const ArrayHeader* thiz);
ArrayHeader* Kotlin_io_Console_readLine();

// String.kt
KInt Kotlin_String_hashCode(const ArrayHeader* thiz);
KBool Kotlin_String_equals(const ArrayHeader* thiz, const ObjHeader* other);
KInt Kotlin_String_compareTo(const ArrayHeader* thiz, const ArrayHeader* other);
KChar Kotlin_String_get(const ArrayHeader* thiz, KInt index);
ArrayHeader* Kotlin_String_fromUtf8Array(const ArrayHeader* array);
ArrayHeader* Kotlin_String_plusImpl(
    const ArrayHeader* thiz, const ArrayHeader* other);
KInt Kotlin_String_getStringLength(const ArrayHeader* thiz);
KRef Kotlin_String_subSequence(
    const ArrayHeader* thiz, KInt startIndex, KInt endIndex);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H
