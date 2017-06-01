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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#ifndef OMIT_BACKTRACE
#if USE_GCC_UNWIND
// GCC unwinder for backtrace.
#include <unwind.h>
#else
// Glibc backtrace function.
#include <execinfo.h>
#endif
#endif // OMIT_BACKTRACE

#include "Assert.h"
#include "Exceptions.h"
#include "ExecFormat.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Types.h"

namespace {

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

#if USE_GCC_UNWIND
struct Backtrace {
  Backtrace(int count, int skip) : index(0), skipCount(skip), array(nullptr) {
    auto result = AllocArrayInstance(
	theArrayTypeInfo, count - skipCount, &array);
    RuntimeAssert(result != nullptr, "Cannot create backtrace array");
  }

  void setNextElement(const char* element) {
    CreateStringFromCString(
	element, ArrayAddressOfElementAt(array->array(), index++));
  }

  int index;
  int skipCount;
  ObjHeader* array;
};

_Unwind_Reason_Code depthCountCallback(
    struct _Unwind_Context * context, void* arg) {
  int* result = reinterpret_cast<int*>(arg);
  (*result)++;
  return _URC_NO_REASON;
}

_Unwind_Reason_Code unwindCallback(
    struct _Unwind_Context* context, void* arg) {
  Backtrace* backtrace = reinterpret_cast<Backtrace*>(arg);
  if (backtrace->skipCount > 0) {
    backtrace->skipCount--;
    return _URC_NO_REASON;
  }

#if (__MINGW32__ || __MINGW64__)
  _Unwind_Ptr address = _Unwind_GetRegionStart(context);
#else
  _Unwind_Ptr address = _Unwind_GetIP(context);
#endif

  char symbol[512];
  if (!AddressToSymbol((const void*)address, symbol, sizeof(symbol))) {
    // Make empty string:
    symbol[0] = '\0';
  }

  char line[512];
  snprintf(line, sizeof(line) - 1, "%s (%p)",
    symbol, (void*)(intptr_t)address);
  backtrace->setNextElement(line);
  return _URC_NO_REASON;
}
#endif

}  // namespace

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
#if USE_GCC_UNWIND
  int depth = 0;
  _Unwind_Backtrace(depthCountCallback, &depth);
  // Skips first 3 elements as irrelevant.
  Backtrace result(depth, 3);
  _Unwind_Backtrace(unwindCallback, &result);
  RETURN_OBJ(result.array);
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
#endif  // !OMIT_BACKTRACE
}

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
		"Throwing something non-throwable");
  throw KotlinException(exception);
}


#ifdef __cplusplus
} // extern "C"
#endif
