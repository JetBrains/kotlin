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

#include <exception>

#if KONAN_NO_EXCEPTIONS
#define OMIT_BACKTRACE 1
#endif
#ifndef OMIT_BACKTRACE
#if USE_GCC_UNWIND
// GCC unwinder for backtrace.
#include <unwind.h>
#else
// Glibc backtrace() function.
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
#include "Utils.h"

namespace {

// TODO: it seems to be very common case; does C++ std library provide something like this?
class AutoFree {
 private:
  void* mem_;

 public:
  AutoFree(void* mem): mem_(mem) {}

  ~AutoFree() {
    konan::free(mem_);
  }
};

#if USE_GCC_UNWIND
struct Backtrace {
  Backtrace(int count, int skip) : index(0), skipCount(skip) {
    auto result = AllocArrayInstance(
        theArrayTypeInfo, count - skipCount, arrayHolder.slot());
    // TODO: throw cached OOME?
    RuntimeCheck(result != nullptr, "Cannot create backtrace array");
  }

  void setNextElement(const char* element) {
    auto result = CreateStringFromCString(
      element, ArrayAddressOfElementAt(obj()->array(), index++));
    // TODO: throw cached OOME?
    RuntimeCheck(result != nullptr, "Cannot create backtrace array element");
  }

  ObjHeader* obj() { return arrayHolder.obj(); }

  int index;
  int skipCount;
  ObjHolder arrayHolder;
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
  konan::snprintf(line, sizeof(line) - 1, "%s (%p)",
    symbol, (void*)(intptr_t)address);
  backtrace->setNextElement(line);
  return _URC_NO_REASON;
}
#endif

}  // namespace

extern "C" {

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
OBJ_GETTER0(GetCurrentStackTrace) {
#if OMIT_BACKTRACE
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, 1, OBJ_RESULT);
  ArrayHeader* array = result->array();
  CreateStringFromCString("<UNIMPLEMENTED>", ArrayAddressOfElementAt(array, 0));
  return result;
#else
  // Skips first 3 elements as irrelevant.
  constexpr int kSkipFrames = 3;
#if USE_GCC_UNWIND
  int depth = 0;
  _Unwind_Backtrace(depthCountCallback, &depth);
  if (depth < kSkipFrames)
      return AllocArrayInstance(theArrayTypeInfo, 0, OBJ_RESULT);
  Backtrace result(depth, kSkipFrames);
  _Unwind_Backtrace(unwindCallback, &result);
  RETURN_OBJ(result.obj());
#else
  const int maxSize = 32;
  void* buffer[maxSize];

  int size = backtrace(buffer, maxSize);
  char** symbols = backtrace_symbols(buffer, size);
  RuntimeCheck(symbols != nullptr, "Not enough memory to retrieve the stacktrace");
  if (size < kSkipFrames)
      return AllocArrayInstance(theArrayTypeInfo, 0, OBJ_RESULT);
  AutoFree autoFree(symbols);
  ObjHolder resultHolder;
  ObjHeader* result = AllocArrayInstance(
      theArrayTypeInfo, size - kSkipFrames, resultHolder.slot());
  ArrayHeader* array = result->array();
  for (int index = kSkipFrames; index < size; ++index) {
    CreateStringFromCString(
      symbols[index], ArrayAddressOfElementAt(array, index - kSkipFrames));
  }
  RETURN_OBJ(result);
#endif
#endif  // !OMIT_BACKTRACE
}

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");
#if KONAN_NO_EXCEPTIONS
  PrintThrowable(exception);
  RuntimeCheck(false, "Exceptions unsupported");
#else
  throw ObjHolder(exception);
#endif
}

#if KONAN_OBJC_INTEROP

void ReportUnhandledException(KRef e);

static void (*oldTerminateHandler)() = nullptr;

static void KonanTerminateHandler() {
  auto currentException = std::current_exception();
  if (!currentException) {
    // No current exception.
    oldTerminateHandler();
  } else {
    try {
      std::rethrow_exception(currentException);
    } catch (ObjHolder& e) {
      ReportUnhandledException(e.obj());
      konan::abort();
    } catch (...) {
      // Not a Kotlin exception.
      oldTerminateHandler();
    }
  }
}

static SimpleMutex konanTerminateHandlerInitializationMutex;

void SetKonanTerminateHandler() {
  if (oldTerminateHandler != nullptr) return; // Already initialized.

  LockGuard<SimpleMutex> lockGuard(konanTerminateHandlerInitializationMutex);

  if (oldTerminateHandler != nullptr) return; // Already initialized.

  oldTerminateHandler = std::get_terminate(); // If termination happens between `set_terminate` and assignment.
  oldTerminateHandler = std::set_terminate(&KonanTerminateHandler);
}

#else // KONAN_OBJC_INTEROP

void SetKonanTerminateHandler() {
  // Nothing to do.
}

#endif // KONAN_OBJC_INTEROP

} // extern "C"