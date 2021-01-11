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
#include <unistd.h>

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
#include "Mutex.hpp"
#include "Natives.h"
#include "KString.h"
#include "SourceInfo.h"
#include "Types.h"
#include "Utils.hpp"
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

#if !OMIT_BACKTRACE && !USE_GCC_UNWIND
SourceInfo getSourceInfo(KConstRef stackTrace, int index) {
  return disallowSourceInfo
      ? SourceInfo { .fileName = nullptr, .lineNumber = -1, .column = -1 }
      : Kotlin_getSourceInfo(*PrimitiveArrayAddressOfElementAt<KNativePtr>(stackTrace->array(), index));
}
#endif

}  // namespace

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
  for (uint32_t index = 0; index < size; ++index) {
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

    for (uint32_t index = 0; index < size; ++index) {
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

namespace {

class {
    /**
     * Timeout 5 sec for concurrent (second) terminate attempt to give a chance the first one to finish.
     * If the terminate handler hangs for 5 sec it is probably fatally broken, so let's do abnormal _Exit in that case.
     */
    unsigned int timeoutSec = 5;
    int terminatingFlag = 0;
  public:
    template <class Fun> RUNTIME_NORETURN void operator()(Fun block) {
      if (compareAndSet(&terminatingFlag, 0, 1)) {
        block();
        // block() is supposed to be NORETURN, otherwise go to normal abort()
        konan::abort();
      } else {
        sleep(timeoutSec);
        // We come here when another terminate handler hangs for 5 sec, that looks fatally broken. Go to forced exit now.
      }
      _Exit(EXIT_FAILURE); // force exit
    }
} concurrentTerminateWrapper;

//! Process exception hook (if any) or just printStackTrace + write crash log
void processUnhandledKotlinException(KRef throwable) {
  OnUnhandledException(throwable);
#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG
  ReportBacktraceToIosCrashLog(throwable);
#endif
}

} // namespace

RUNTIME_NORETURN void TerminateWithUnhandledException(KRef throwable) {
  concurrentTerminateWrapper([=]() {
      processUnhandledKotlinException(throwable);
    konan::abort();
  });
}

// Some libstdc++-based targets has limited support for std::current_exception and other C++11 functions.
// This restriction can be lifted later when toolchains will be updated.
#if KONAN_HAS_CXX11_EXCEPTION_FUNCTIONS

namespace {
// Copy, move and assign would be safe, but not much useful, so let's delete all (rule of 5)
class TerminateHandler : private kotlin::Pinned {

  // In fact, it's safe to call my_handler directly from outside: it will do the job and then invoke original handler,
  // even if it has not been initialized yet. So one may want to make it public and/or not the class member
  RUNTIME_NORETURN static void kotlinHandler() {
    concurrentTerminateWrapper([]() {
      if (auto currentException = std::current_exception()) {
        try {
          std::rethrow_exception(currentException);
        } catch (ExceptionObjHolder& e) {
          processUnhandledKotlinException(e.obj());
          konan::abort();
        } catch (...) {
          // Not a Kotlin exception - call default handler
          instance().queuedHandler_();
        }
      }
      // Come here in case of direct terminate() call or unknown exception - go to default terminate handler.
      instance().queuedHandler_();
    });
  }

  using QH = __attribute__((noreturn)) void(*)();
  QH queuedHandler_;

  /// Use machinery like Meyers singleton to provide thread safety
  TerminateHandler()
    : queuedHandler_((QH)std::set_terminate(kotlinHandler)) {}

  static TerminateHandler& instance() {
    static TerminateHandler singleton [[clang::no_destroy]];
    return singleton;
  }

  // Dtor might be in use to restore original handler. However, consequent install
  // will not reconstruct handler anyway, so let's keep dtor deleted to avoid confusion.
  ~TerminateHandler() = delete;
public:
  /// First call will do the job, all consequent will do nothing.
  static void install() {
    instance(); // Use side effect of warming up
  }
};
} // anon namespace

// Use one public function to limit access to the class declaration
void SetKonanTerminateHandler() {
  TerminateHandler::install();
}

#else // KONAN_OBJC_INTEROP

void SetKonanTerminateHandler() {
  // Nothing to do.
}

#endif // KONAN_OBJC_INTEROP

void DisallowSourceInfo() {
  disallowSourceInfo = true;
}
