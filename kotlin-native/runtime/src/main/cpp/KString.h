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

#ifndef RUNTIME_KSTRING_H
#define RUNTIME_KSTRING_H

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

#endif // RUNTIME_KSTRING_H
