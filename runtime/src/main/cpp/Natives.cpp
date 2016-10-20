#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// TODO: those must be compiler intrinsics afterwards.
KByte Kotlin_ByteArray_get(const ArrayHeader* obj, int32_t index) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

void Kotlin_ByteArray_set(ArrayHeader* obj, int32_t index, KByte value) {
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  *ByteArrayAddressOfElementAt(obj, index) = value;
}

KChar Kotlin_String_get(const ArrayHeader* obj, int32_t index) {
  // TODO: support full UTF-8.
  if (static_cast<uint32_t>(index) >= obj->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *ByteArrayAddressOfElementAt(obj, index);
}

ArrayHeader* Kotlin_String_fromUtf8Array(const ArrayHeader* array) {
  RuntimeAssert(array->type_info_ == theByteArrayTypeInfo, "Must get a byte array");
  uint32_t length = ArraySizeBytes(array);
  // TODO: support full UTF-8.
  ArrayHeader* result = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(result, 0),
      ByteArrayAddressOfElementAt(array, 0),
      length);
  return result;
}

}
