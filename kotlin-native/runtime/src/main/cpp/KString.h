/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Common.h"
#include "Memory.h"
#include "Types.h"
#include "TypeInfo.h"

#ifdef __cplusplus
extern "C" {
#endif

OBJ_GETTER(CreateStringFromCString, const char* cstring);
OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes);
char* CreateCStringFromString(KConstRef kstring);
void DisposeCString(char* cstring);
ObjHeader* CreatePermanentStringFromCString(const char* nullTerminatedUTF8);
void FreePermanentStringForTests(ArrayHeader* header);  // to make ASAN happy, in hostRuntimeTests call FreePermanentStringForTests() after CreatePermanentStringFromCString()

OBJ_GETTER(StringFromUtf8Buffer, const char* start, size_t size);

#ifdef __cplusplus
}
#endif

template <typename T>
int binarySearchRange(const T* array, int arrayLength, T needle) {
  int bottom = 0;
  int top = arrayLength - 1;
  int middle = -1;
  T value = 0;
  while (bottom <= top) {
    middle = (bottom + top) / 2;
    value = array[middle];
    if (needle > value)
      bottom = middle + 1;
    else if (needle == value)
      return middle;
    else
      top = middle - 1;
  }
  return middle - (needle < value ? 1 : 0);
}

namespace kotlin {

std::string to_string(KString kstring);

}
