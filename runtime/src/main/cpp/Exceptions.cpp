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
#ifndef OMIT_BACKTRACE
#include <execinfo.h>
#endif
#include <stdlib.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Types.h"

class KotlinException {
 public:

  KRef exception_;

  KotlinException(KRef exception) : exception_(exception) {
      ::AddRef(exception_->container());
  };

  ~KotlinException() {
      ::Release(exception_->container());
  };
};

// TODO: it seems to be very common case; does C++ std library provide something like this?
class AutoFree {
 private:
  void* mem_;

 public:
  AutoFree(void* mem): mem_(mem) {}

  ~AutoFree() {
    free(mem_);
  }
};

#ifdef __cplusplus
extern "C" {
#endif

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
OBJ_GETTER0(GetCurrentStackTrace) {
#if OMIT_BACKTRACE
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, 1, OBJ_RESULT);
  ArrayHeader* array = result->array();
  CreateStringFromCString("<UNIMPLEMENTED>", ArrayAddressOfElementAt(array, 0));
  return result;
#else
  const int maxSize = 32;
  void* buffer[maxSize];

  int size = backtrace(buffer, maxSize);
  char** symbols = backtrace_symbols(buffer, size);
  RuntimeAssert(symbols != nullptr, "Not enough memory to retrieve the stacktrace");

  AutoFree autoFree(symbols);
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, size, OBJ_RESULT);
  ArrayHeader* array = result->array();
  for (int index = 0; index < size; ++index) {
    CreateStringFromCString(
      symbols[index], ArrayAddressOfElementAt(array, index));
  }
  return result;
#endif
}

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");
  throw KotlinException(exception);
}


#ifdef __cplusplus
} // extern "C"
#endif
