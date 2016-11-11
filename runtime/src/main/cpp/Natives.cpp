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

KString Kotlin_Any_toString(KConstRef thiz) {
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
  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(
      theStringTypeInfo, array->count_).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      ArrayDataSizeBytes(array));
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

KBoolean Kotlin_String_equals(KString thiz, KConstRef other) {
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
