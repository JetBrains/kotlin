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
#include <cstdlib>
#include <stdio.h>
#include <string.h>
#include <stdint.h>

#include <exception>
#include <mutex>
#include <unistd.h>
#include <vector>

#include "KAssert.h"
#include "Exceptions.h"
#include "ExecFormat.h"
#include "Memory.h"
#include <std_support/Atomic.hpp>
#include "concurrent/Mutex.hpp"
#include "Porting.h"
#include "Types.h"
#include "Utils.hpp"
#include "ObjCExceptions.h"

// Defined in RuntimeUtils.kt
extern "C" void Kotlin_runUnhandledExceptionHook(KRef exception);
extern "C" void ReportUnhandledException(KRef exception);

void ThrowException(KRef exception) {
  RuntimeAssert(exception != nullptr && IsInstanceInternal(exception, theThrowableTypeInfo),
                "Throwing something non-throwable");
  ExceptionObjHolder::Throw(exception);
}

void RUNTIME_NORETURN HandleCurrentExceptionWhenLeavingKotlinCode() {
  try {
      std::rethrow_exception(std::current_exception());
  } catch (ExceptionObjHolder& e) {
      std::terminate();  // Terminate when it's a kotlin exception.
  }
}

namespace {

class {
    /**
     * Timeout 5 sec for concurrent (second) terminate attempt to give a chance the first one to finish.
     * If the terminate handler hangs for 5 sec it is probably fatally broken, so let's do abnormal _Exit in that case.
     */
    unsigned int timeoutSec = 5;
    std::recursive_mutex terminationMutex_{};
  public:
    template <class Fun> RUNTIME_NORETURN void operator()(Fun block) {
      kotlin::CallsCheckerIgnoreGuard guard;
      if (terminationMutex_.try_lock()) {
        block();
        // block() is supposed to be NORETURN, otherwise go to normal abort()
        std::abort();
      } else {
        kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        sleep(timeoutSec);
        // We come here when another terminate handler hangs for 5 sec, that looks fatally broken. Go to forced exit now.
      }
      _Exit(EXIT_FAILURE); // force exit
    }
} concurrentTerminateWrapper;

void RUNTIME_NORETURN terminateWithUnhandledException(KRef exception) {
    kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
    concurrentTerminateWrapper([exception]() {
        ReportUnhandledException(exception);

        // Just in case.
        // At this stage we are more interested in the exception than in the thread state checker failure.
        kotlin::CallsCheckerIgnoreGuard ignoreCallChecks;

#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG
        ReportBacktraceToIosCrashLog(exception);
#endif

        kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);

        // Best effort to make sure the reported exception gets actually printed:
        konan::consoleFlush();

        std::abort();
    });
}

void processUnhandledException(KRef exception) noexcept {
    kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
    try {
        Kotlin_runUnhandledExceptionHook(exception);
    } catch (ExceptionObjHolder& e) {
        terminateWithUnhandledException(e.GetExceptionObject());
    }
}

} // namespace

PERFORMANCE_INLINE RUNTIME_NOTHROW OBJ_GETTER(Kotlin_getExceptionObject, void* holder) {
    RETURN_OBJ(static_cast<ExceptionObjHolder*>(holder)->GetExceptionObject());
}

namespace {
    class TerminateHandlersRegistry : kotlin::Pinned {
    public:
        static TerminateHandlersRegistry& instance() {
            [[clang::no_destroy]] static TerminateHandlersRegistry instance;
            return instance;
        }
        
        void installKotlinHandler() {
            pushHandler(kotlinHandler);
        }

    private:
        RUNTIME_NORETURN static void kotlinHandler() {
            if (auto currentException = std::current_exception()) {
                try {
                    std::rethrow_exception(currentException);
                } catch (ExceptionObjHolder& e) {
                    // Both thread states are allowed here because there is no guarantee that
                    // C++ runtime will unwind the stack for an unhandled exception. Thus there
                    // is no guarantee that state switches made on interop borders will be rolled back.

                    // Moreover, a native code can catch an exception thrown by a Kotlin callback,
                    // store it to a global and then re-throw it in another thread which is not attached
                    // to the Kotlin runtime. To handle this case, use the CalledFromNativeGuard.
                    // TODO: Forbid throwing Kotlin exceptions through the interop border to get rid of this case.
                    kotlin::CalledFromNativeGuard guard(/* reentrant = */ true);
                    processUnhandledException(e.GetExceptionObject());
                    terminateWithUnhandledException(e.GetExceptionObject());
                } catch (...) {
                    // Not a Kotlin exception - call default handler
                    kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
                    queuedHandler();
                }
            }
            // Come here in case of direct terminate() call or unknown exception - go to default terminate handler.
            kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
            queuedHandler();
        }

        RUNTIME_NORETURN static void queuedHandler() {
            concurrentTerminateWrapper([]() {
                // Not a Kotlin exception - call default handler
                if (auto handler = instance().popHandler()) {
                    handler();
                }
            });
        }

        TerminateHandlersRegistry() = default;

        void pushHandler(std::terminate_handler newHandler) {
            std::lock_guard lock(handlersMutex_);
            auto previousHandler = std::set_terminate(newHandler);
            previousHandlers_.push_back(previousHandler);
        }

        std::terminate_handler popHandler() {
            std::lock_guard lock(handlersMutex_);
            if (previousHandlers_.empty()) return nullptr;
            auto handler = previousHandlers_.back();
            previousHandlers_.pop_back();
            return handler;
        }

        std::mutex handlersMutex_{};
        std::vector<std::terminate_handler> previousHandlers_{};
    };
} // anon namespace

void SetKonanTerminateHandler() {
    TerminateHandlersRegistry::instance().installKotlinHandler();
}

extern "C" void Kotlin_updateTerminateHandler() {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    SetKonanTerminateHandler();
}

extern "C" void RUNTIME_NORETURN Kotlin_terminateWithUnhandledException(KRef exception) {
    kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
    terminateWithUnhandledException(exception);
}

extern "C" void Kotlin_processUnhandledException(KRef exception) {
    kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
    processUnhandledException(exception);
}

void kotlin::ProcessUnhandledException(KRef exception) noexcept {
    // This may be called from any state, do reentrant state switch to runnable.
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable, /* reentrant = */ true);
    processUnhandledException(exception);
}

void RUNTIME_NORETURN kotlin::TerminateWithUnhandledException(KRef exception) noexcept {
    // This may be called from any state, do reentrant state switch to runnable.
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable, /* reentrant = */ true);
    terminateWithUnhandledException(exception);
}
