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
KBoolean Kotlin_Any_equals(KConstRef thiz, KConstRef other) {
  return thiz == other;
}

KInt Kotlin_Any_hashCode(KConstRef thiz) {
  // Here we will use different mechanism for stable hashcode, using meta-objects
  // if moving collector will be used.
  return reinterpret_cast<uintptr_t>(thiz);
}

OBJ_GETTER(Kotlin_Any_toString, KConstRef thiz) {
  // TODO: make it more sensible, such as address and type info.
  return nullptr;
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

OBJ_GETTER0(Kotlin_io_Console_readLine) {
  char data[2048];
  if (!fgets(data, sizeof(data) - 1, stdin)) {
    return nullptr;
  }
  int32_t length = strlen(data);
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      data, length);
  RETURN_OBJ(result->obj());
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

OBJ_GETTER(Kotlin_String_fromUtf8Array, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theByteArrayTypeInfo, "Must use a byte array");
  if (start < 0 || size < 0 || start + size > array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }

  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }

  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, size).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, start),
      size);
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_fromCharArray, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theCharArrayTypeInfo, "Must use a byte array");
  if (start < 0 || size < 0 || start + size > array->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }

  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }

  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, size).GetPlace();
  for (KInt index = 0; index < size; ++index) {
    *ByteArrayAddressOfElementAt(result, index) =
        *PrimitiveArrayAddressOfElementAt<KChar>(array, start + index);
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toCharArray, KString string) {
  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(
      theCharArrayTypeInfo, string->count_).GetPlace();
  for (int index = 0; index < string->count_; ++index) {
    *PrimitiveArrayAddressOfElementAt<KChar>(result, index) =
        *ByteArrayAddressOfElementAt(string, index);
  }
  RETURN_OBJ(result->obj());
}


OBJ_GETTER(Kotlin_String_plusImpl, KString thiz, KString other) {
  // TODO: support UTF-8
  RuntimeAssert(thiz != nullptr, "this cannot be null");
  RuntimeAssert(other != nullptr, "other cannot be null");
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
  RETURN_OBJ(result->obj());
}

KBoolean Kotlin_String_equals(KString thiz, KConstRef other) {
  if (other == nullptr || other->type_info() != theStringTypeInfo) return false;
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return true;
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

OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex) {
  if (startIndex < 0 || endIndex >= thiz->count_ || startIndex > endIndex) {
    // TODO: is it correct exception?
    ThrowArrayIndexOutOfBoundsException();
  }
  if (startIndex == endIndex) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  // TODO: support UTF-8.
  KInt length = endIndex - startIndex;
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(ByteArrayAddressOfElementAt(result, 0),
         ByteArrayAddressOfElementAt(thiz, startIndex),
         length);
  RETURN_OBJ(result->obj());
}

OBJ_GETTER0(Kotlin_getCurrentStackTrace) {
  RETURN_RESULT_OF0(GetCurrentStackTrace);
}

// TODO: consider handling it with compiler magic instead.
OBJ_GETTER0(Kotlin_konan_internal_undefined) {
  RETURN_OBJ(nullptr);
}

}  // extern "C"
