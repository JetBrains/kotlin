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

#include "KAssert.h"
#include "Exceptions.h"
#include "ExecFormat.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "SourceInfo.h"
#include "Types.h"
#include "Utils.h"
#include "ObjCExceptions.h"

namespace {

// RuntimeUtils.kt
extern "C" void ReportUnhandledException(KRef throwable);
extern "C" void ExceptionReporterLaunchpad(KRef reporter, KRef throwable);

KRef currentUnhandledExceptionHook = nullptr;
int32_t currentUnhandledExceptionHookLock = 0;
int32_t currentUnhandledExceptionHookCookie = 0;

#if USE_GCC_UNWIND
struct Backtrace {
  Backtrace(int count, int skip) : index(0), skipCount(skip) {
    uint32_t size = count - skipCount;
    if (size < 0) {
      size = 0;
    }
    auto result = AllocArrayInstance(theNativePtrArrayTypeInfo, size, arrayHolder.slot());
    // TODO: throw cached OOME?
    RuntimeCheck(result != nullptr, "Cannot create backtrace array");
  }

  void setNextElement(_Unwind_Ptr element) {
    Kotlin_NativePtrArray_set(obj(), index++, (KNativePtr) element);
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
  backtrace->setNextElement(address);

  return _URC_NO_REASON;
}
#endif

THREAD_LOCAL_VARIABLE bool disallowSourceInfo = false;

SourceInfo getSourceInfo(KConstRef stackTrace, int index) {
  return disallowSourceInfo
      ? SourceInfo { .fileName = nullptr, .lineNumber = -1, .column = -1 }
      : Kotlin_getSourceInfo(*PrimitiveArrayAddressOfElementAt<KNativePtr>(stackTrace->array(), index));
}

}  // namespace

extern "C" {

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
NO_INLINE OBJ_GETTER0(Kotlin_getCurrentStackTrace) {
#if OMIT_BACKTRACE
  return AllocArrayInstance(theNativePtrArrayTypeInfo, 0, OBJ_RESULT);
#else
  // Skips first 2 elements as irrelevant: this function and primary Throwable constructor.
  constexpr int kSkipFrames = 2;
#if USE_GCC_UNWIND
  int depth = 0;
  _Unwind_Backtrace(depthCountCallback, &depth);
  Backtrace result(depth, kSkipFrames);
  if (result.obj()->array()->count_ > 0) {
    _Unwind_Backtrace(unwindCallback, &result);
  }
  RETURN_OBJ(result.obj());
#else
  const int maxSize = 32;
  void* buffer[maxSize];

  int size = backtrace(buffer, maxSize);
  if (size < kSkipFrames)
      return AllocArrayInstance(theNativePtrArrayTypeInfo, 0, OBJ_RESULT);

  ObjHolder resultHolder;
  ObjHeader* result = AllocArrayInstance(theNativePtrArrayTypeInfo, size - kSkipFrames, resultHolder.slot());
  for (int index = kSkipFrames; index < size; ++index) {
    Kotlin_NativePtrArray_set(result, index - kSkipFrames, buffer[index]);
  }
  RETURN_OBJ(result);
#endif
#endif  // !OMIT_BACKTRACE
}

OBJ_GETTER(GetStackTraceStrings, KConstRef stackTrace) {
#if OMIT_BACKTRACE
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, 1, OBJ_RESULT);
  ObjHolder holder;
  CreateStringFromCString("<UNIMPLEMENTED>", holder.slot());
  UpdateHeapRef(ArrayAddressOfElementAt(result->array(), 0), holder.obj());
  return result;
#else
  uint32_t size = stackTrace->array()->count_;
  ObjHolder resultHolder;
  ObjHeader* strings = AllocArrayInstance(theArrayTypeInfo, size, resultHolder.slot());
#if USE_GCC_UNWIND
  for (int index = 0; index < size; ++index) {
    KNativePtr address = Kotlin_NativePtrArray_get(stackTrace, index);
    char symbol[512];
    if (!AddressToSymbol((const void*) address, symbol, sizeof(symbol))) {
      // Make empty string:
      symbol[0] = '\0';
    }
    char line[512];
    konan::snprintf(line, sizeof(line) - 1, "%s (%p)", symbol, (void*)(intptr_t)address);
    ObjHolder holder;
    CreateStringFromCString(line, holder.slot());
    UpdateHeapRef(ArrayAddressOfElementAt(strings->array(), index), holder.obj());
  }
#else
  if (size > 0) {
    char **symbols = backtrace_symbols(PrimitiveArrayAddressOfElementAt<KNativePtr>(stackTrace->array(), 0), size);
    RuntimeCheck(symbols != nullptr, "Not enough memory to retrieve the stacktrace");

    for (int index = 0; index < size; ++index) {
      auto sourceInfo = getSourceInfo(stackTrace, index);
      const char* symbol = symbols[index];
      const char* result;
      char line[1024];
      if (sourceInfo.fileName != nullptr) {
        if (sourceInfo.lineNumber != -1) {
          konan::snprintf(line, sizeof(line) - 1, "%s (%s:%d:%d)",
                          symbol, sourceInfo.fileName, sourceInfo.lineNumber, sourceInfo.column);
        } else {
          konan::snprintf(line, sizeof(line) - 1, "%s (%s:<unknown>)", symbol, sourceInfo.fileName);
        }
        result = line;
      } else {
        result = symbol;
      }
      ObjHolder holder;
      CreateStringFromCString(result, holder.slot());
      UpdateHeapRef(ArrayAddressOfElementAt(strings->array(), index), holder.obj());
    }
    // Not konan::free. Used to free memory allocated in backtrace_symbols where malloc is used.
    free(symbols);
  }
#endif
  RETURN_OBJ(strings);
#endif  // !OMIT_BACKTRACE
}

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");
#if KONAN_NO_EXCEPTIONS
  PrintThrowable(exception);
  RuntimeCheck(false, "Exceptions unsupported");
#else
  throw ExceptionObjHolder(exception);
#endif
}

OBJ_GETTER(Kotlin_setUnhandledExceptionHook, KRef hook) {
  RETURN_RESULT_OF(SwapHeapRefLocked,
    &currentUnhandledExceptionHook, currentUnhandledExceptionHook, hook, &currentUnhandledExceptionHookLock,
    &currentUnhandledExceptionHookCookie);
}

void OnUnhandledException(KRef throwable) {
  ObjHolder handlerHolder;
  auto* handler = SwapHeapRefLocked(&currentUnhandledExceptionHook, currentUnhandledExceptionHook, nullptr,
     &currentUnhandledExceptionHookLock,  &currentUnhandledExceptionHookCookie, handlerHolder.slot());
  if (handler == nullptr) {
    ReportUnhandledException(throwable);
  } else {
    ExceptionReporterLaunchpad(handler, throwable);
  }
}

#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG
static bool terminating = false;
static SimpleMutex terminatingMutex;
#endif

RUNTIME_NORETURN void TerminateWithUnhandledException(KRef throwable) {
  OnUnhandledException(throwable);

#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG
  {
    LockGuard<SimpleMutex> lock(terminatingMutex);
    if (!terminating) {
      ReportBacktraceToIosCrashLog(throwable);
    }
  }
#endif

  konan::abort();
}

// Some libstdc++-based targets has limited support for std::current_exception and other C++11 functions.
// This restriction can be lifted later when toolchains will be updated.
#if KONAN_HAS_CXX11_EXCEPTION_FUNCTIONS

static void (*oldTerminateHandler)() = nullptr;

static void callOldTerminateHandler() {
#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG
  {
    LockGuard<SimpleMutex> lock(terminatingMutex);
    terminating = true;
  }
#endif

  RuntimeCheck(oldTerminateHandler != nullptr, "Underlying exception handler is not set.");
  oldTerminateHandler();
}

static void KonanTerminateHandler() {
  auto currentException = std::current_exception();
  if (!currentException) {
    // No current exception.
    callOldTerminateHandler();
  } else {
    try {
      std::rethrow_exception(currentException);
    } catch (ExceptionObjHolder& e) {
      TerminateWithUnhandledException(e.obj());
    } catch (...) {
      // Not a Kotlin exception.
      callOldTerminateHandler();
    }
  }
}

static SimpleMutex konanTerminateHandlerInitializationMutex;

void SetKonanTerminateHandler() {
  if (oldTerminateHandler != nullptr) return; // Already initialized.

  LockGuard<SimpleMutex> lockGuard(konanTerminateHandlerInitializationMutex);

  if (oldTerminateHandler != nullptr) return; // Already initialized.

  oldTerminateHandler = std::set_terminate(&KonanTerminateHandler);
}

#else // KONAN_OBJC_INTEROP

void SetKonanTerminateHandler() {
  // Nothing to do.
}

#endif // KONAN_OBJC_INTEROP

} // extern "C"

void DisallowSourceInfo() {
  disallowSourceInfo = true;
}
