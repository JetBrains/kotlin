#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "Assert.h"
#include "City.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// Any.kt
KBool Kotlin_Any_equals(KConstRef thiz, KConstRef other) {
  return thiz == other;
}

KInt Kotlin_Any_hashCode(KConstRef thiz) {
  // Here we will use different mechanism for stable hashcode, using meta-objects
  // if moving collector will be used.
  return reinterpret_cast<uintptr_t>(thiz);
}

KString Kotlin_Any_toString(KConstRef thiz) {
  return nullptr;
}

// Arrays.kt
// TODO: those must be compiler intrinsics afterwards.
KRef Kotlin_Array_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ArrayAddressOfElementAt(obj, index);
}

void Kotlin_Array_set(ArrayHeader* obj, KInt index, KRef value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_Array_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theArrayTypeInfo, length).GetPlace();
  memcpy(
      ArrayAddressOfElementAt(result, 0),
      ArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_Array_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KByte Kotlin_ByteArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

void Kotlin_ByteArray_set(ArrayHeader* obj, KInt index, KByte value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ByteArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_ByteArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theByteArrayTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_ByteArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KChar Kotlin_CharArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *CharArrayAddressOfElementAt(obj, index);
}

void Kotlin_CharArray_set(ArrayHeader* obj, KInt index, KChar value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *CharArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_CharArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theCharArrayTypeInfo, length).GetPlace();
  memcpy(
      CharArrayAddressOfElementAt(result, 0),
      CharArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_CharArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

KInt Kotlin_IntArray_get(const ArrayHeader* obj, KInt index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *IntArrayAddressOfElementAt(obj, index);
}

void Kotlin_IntArray_set(ArrayHeader* obj, KInt index, KInt value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *IntArrayAddressOfElementAt(obj, index) = value;
}

ArrayHeader* Kotlin_IntArray_clone(const ArrayHeader* array) {
  uint32_t length = ArraySizeBytes(array);
  ArrayHeader* result = ArrayContainer(theIntArrayTypeInfo, length).GetPlace();
  memcpy(
      IntArrayAddressOfElementAt(result, 0),
      IntArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KInt Kotlin_IntArray_getArrayLength(const ArrayHeader* array) {
  return array->count_;
}

// io/Console.kt
void Kotlin_io_Console_print(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  // TODO: system stdout must be aware about UTF-8.
  write(STDOUT_FILENO, ByteArrayAddressOfElementAt(message, 0), message->count_);
}

void Kotlin_io_Console_println(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  // TODO: system stdout must be aware about UTF-8.
  write(STDOUT_FILENO, ByteArrayAddressOfElementAt(message, 0), message->count_);
  Kotlin_io_Console_println0();
}

void Kotlin_io_Console_println0() {
  write(STDOUT_FILENO, "\n", 1);
}

KString Kotlin_io_Console_readLine() {
  char data[2048];
  if (!fgets(data, sizeof(data) - 1, stdin)) {
    return nullptr;
  }
  int32_t length = strlen(data);
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      data, length);
  return result;
}

// String.kt
KInt Kotlin_String_compareTo(KString thiz, KString other) {
  return memcmp(ByteArrayAddressOfElementAt(thiz, 0),
                ByteArrayAddressOfElementAt(other, 0),
                thiz->count_ < other->count_ ? thiz->count_ : other->count_);
}

KChar Kotlin_String_get(KString thiz, KInt index) {
  // TODO: support full UTF-8.
  if (static_cast<uint32_t>(index) >= thiz->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(thiz, index);
}

KInt Kotlin_String_getStringLength(KString thiz) {
  return thiz->count_;
}

KString Kotlin_String_fromUtf8Array(const ArrayHeader* array) {
  RuntimeAssert(array->type_info() == theByteArrayTypeInfo, "Must use a byte array");
  uint32_t length = ArraySizeBytes(array);
  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

KString Kotlin_String_plusImpl(KString thiz, KString other) {
  // TODO: support UTF-8
  RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must be a string");
  RuntimeAssert(other->type_info() == theStringTypeInfo, "Must be a string");
  uint32_t result_length = thiz->count_ + other->count_;
  ArrayHeader* result = ArrayContainer(
      theStringTypeInfo, result_length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(thiz, 0),
      thiz->count_);
  memcpy(
      ByteArrayAddressOfElementAt(result, thiz->count_),
      ByteArrayAddressOfElementAt(other, 0),
      other->count_);
  return result;
}

KBool Kotlin_String_equals(KString thiz, KConstRef other) {
  if (other == nullptr || other->type_info() != theStringTypeInfo) return 0;
  const ArrayHeader* otherString = reinterpret_cast<const ArrayHeader*>(other);
  return thiz->count_ == otherString->count_ &&
      memcmp(ByteArrayAddressOfElementAt(thiz, 0),
             ByteArrayAddressOfElementAt(otherString, 0),
             thiz->count_) == 0;
}

KInt Kotlin_String_hashCode(KString thiz) {
  // TODO: consider caching strings hashes.
  // TODO: maybe use some simpler hashing algorithm?
  return CityHash64(ByteArrayAddressOfElementAt(thiz, 0), thiz->count_);
}

KRef Kotlin_String_subSequence(KString thiz, KInt startIndex, KInt endIndex) {
  RuntimeAssert(false, "Unsupported operation");
  return nullptr;
}

}  // extern "C"
