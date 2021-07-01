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

#include "KAssert.h"
#include "Exceptions.h"
#include "ExecFormat.h"
#include "Memory.h"
#include "Mutex.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "ObjCExceptions.h"

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstance(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");
#if KONAN_NO_EXCEPTIONS
  PrintThrowable(exception);
  RuntimeCheck(false, "Exceptions unsupported");
#else
  ExceptionObjHolder::Throw(exception);
#endif
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
  // Use the reentrant switch because both states are possible here:
  //  - runnable, if the exception occured in a pure Kotlin thread (except initialization of globals).
  //  - native, if the throwing code was called from ObjC/Swift or if the exception occured during initialization of globals.
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable, /* reentrant = */ true);
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

ALWAYS_INLINE RUNTIME_NOTHROW OBJ_GETTER(Kotlin_getExceptionObject, void* holder) {
#if !KONAN_NO_EXCEPTIONS
    RETURN_OBJ(static_cast<ExceptionObjHolder*>(holder)->GetExceptionObject());
#else
    RETURN_OBJ(nullptr);
#endif
}

#if !KONAN_NO_EXCEPTIONS

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
          processUnhandledKotlinException(e.GetExceptionObject());
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

#else // !KONAN_NO_EXCEPTIONS

void SetKonanTerminateHandler() {
  // Nothing to do.
}

#endif // !KONAN_NO_EXCEPTIONS
