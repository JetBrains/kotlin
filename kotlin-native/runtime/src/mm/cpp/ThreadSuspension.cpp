/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"

#include <condition_variable>
#include <thread>
#include <mutex>

#include "CallsChecker.hpp"
#include "Logging.hpp"
#include "SafePoint.hpp"
#include "StackTrace.hpp"

using namespace kotlin;

namespace {

template<typename F>
bool allThreads(F predicate) noexcept {
    auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
    auto* currentThread = (threadRegistry.IsCurrentThreadRegistered()) ? threadRegistry.CurrentThreadData() : nullptr;
    kotlin::mm::ThreadRegistry::Iterable threads = kotlin::mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threads) {
        // Handle if suspension was initiated by the mutator thread.
        if (&thread == currentThread)
            continue;
        if (!predicate(thread)) {
            return false;
        }
    }
    return true;
}

void yield() noexcept {
    std::this_thread::yield();
}

[[clang::no_destroy]] thread_local std::optional<mm::SafePointActivator> gSafePointActivator = std::nullopt;
[[clang::no_destroy]] std::mutex gSuspensionMutex;
[[clang::no_destroy]] std::condition_variable gSuspensionCondVar;

} // namespace

std::atomic<bool> kotlin::mm::internal::gSuspensionRequested = false;

kotlin::ThreadState kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState newState) noexcept {
    ThreadState oldState = state_.exchange(newState);
    if (oldState == ThreadState::kNative && newState == ThreadState::kRunnable) {
        // must use already acquired ThreadData because TLS may be in invalid state e.g. during thread detach
        safePoint(threadData_);
    }
    return oldState;
}

NO_EXTERNAL_CALLS_CHECK void kotlin::mm::ThreadSuspensionData::suspendIfRequested() noexcept {
    if (IsThreadSuspensionRequested()) {
        threadData_.gc().OnSuspendForGC();
        std::unique_lock lock(gSuspensionMutex);
        auto threadId = konan::currentThreadId();
        auto suspendStartMs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC, kTagMM}, "Suspending thread %d", threadId);
        AutoReset scopedAssignSuspended(&suspended_, true);
        gSuspensionCondVar.wait(lock, []() { return !IsThreadSuspensionRequested(); });
        auto suspendEndMs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC, kTagMM}, "Resuming thread %d after %" PRIu64 " microseconds of suspension",
                        threadId, suspendEndMs - suspendStartMs);
    }
}

bool kotlin::mm::RequestThreadsSuspension() noexcept {
    CallsCheckerIgnoreGuard guard;

    RuntimeAssert(gSafePointActivator == std::nullopt, "Current thread already suspended threads.");
    {
        std::unique_lock lock(gSuspensionMutex);
        // Someone else has already suspended threads.
        if (internal::gSuspensionRequested.load(std::memory_order_relaxed)) {
            return false;
        }
        gSafePointActivator = mm::SafePointActivator();
        internal::gSuspensionRequested.store(true);
    }

    return true;
}

void kotlin::mm::WaitForThreadsSuspension() noexcept {
    // Spin waiting for threads to suspend. Ignore Native threads.
    while (!allThreads([](mm::ThreadData& thread) { return thread.suspensionData().suspendedOrNative(); })) {
        yield();
    }
}

void kotlin::mm::ResumeThreads() noexcept {
    RuntimeAssert(gSafePointActivator != std::nullopt, "Current thread must have suspended threads");
    gSafePointActivator = std::nullopt;

    // From the std::condition_variable docs:
    // Even if the shared variable is atomic, it must be modified under
    // the mutex in order to correctly publish the modification to the waiting thread.
    // https://en.cppreference.com/w/cpp/thread/condition_variable
    {
        std::unique_lock lock(gSuspensionMutex);
        internal::gSuspensionRequested.store(false);
    }
    gSuspensionCondVar.notify_all();
}
