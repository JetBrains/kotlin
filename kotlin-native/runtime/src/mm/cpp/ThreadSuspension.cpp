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
#include "StackTrace.hpp"

namespace {

bool isSuspendedOrNative(kotlin::mm::ThreadData& thread) noexcept {
    auto& suspensionData = thread.suspensionData();
    return suspensionData.suspended() || suspensionData.state() == kotlin::ThreadState::kNative;
}

template<typename F>
bool allThreads(F predicate) noexcept {
    auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
    auto* currentThread = (threadRegistry.IsCurrentThreadRegistered())
            ? threadRegistry.CurrentThreadData()
            : nullptr;
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

THREAD_LOCAL_VARIABLE bool gSuspensionRequestedByCurrentThread = false;
[[clang::no_destroy]] std::mutex gSuspensionMutex;
[[clang::no_destroy]] std::condition_variable gSuspensionCondVar;

} // namespace

std::atomic<bool> kotlin::mm::internal::gSuspensionRequested = false;

void kotlin::mm::ThreadSuspensionData::suspendIfRequestedSlowPath() noexcept {
    CallsCheckerIgnoreGuard guard;

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

    RuntimeAssert(gSuspensionRequestedByCurrentThread == false, "Current thread already suspended threads.");
    {
        std::unique_lock lock(gSuspensionMutex);
        bool actual = false;
        internal::gSuspensionRequested.compare_exchange_strong(actual, true);
        if (actual) {
            return false;
        }
    }
    gSuspensionRequestedByCurrentThread = true;

    return true;
}

void kotlin::mm::WaitForThreadsSuspension() noexcept {
    // Spin waiting for threads to suspend. Ignore Native threads.
    while(!allThreads(isSuspendedOrNative)) {
        yield();
    }
}

NO_INLINE void kotlin::mm::SuspendIfRequestedSlowPath() noexcept {
    mm::ThreadRegistry::Instance().CurrentThreadData()->suspensionData().suspendIfRequestedSlowPath();
}

ALWAYS_INLINE void kotlin::mm::SuspendIfRequested() noexcept {
    if (IsThreadSuspensionRequested()) {
        SuspendIfRequestedSlowPath();
    }
}

void kotlin::mm::ResumeThreads() noexcept {
    // From the std::condition_variable docs:
    // Even if the shared variable is atomic, it must be modified under
    // the mutex in order to correctly publish the modification to the waiting thread.
    // https://en.cppreference.com/w/cpp/thread/condition_variable
    {
        std::unique_lock lock(gSuspensionMutex);
        internal::gSuspensionRequested = false;
    }
    gSuspensionRequestedByCurrentThread = false;
    gSuspensionCondVar.notify_all();
}
