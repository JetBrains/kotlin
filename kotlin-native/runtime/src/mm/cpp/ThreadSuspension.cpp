/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"

#include <condition_variable>
#include <thread>
#include <mutex>

#include "Logging.hpp"
#include "StackTrace.hpp"

namespace {

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

bool kotlin::mm::isSuspendedOrNative(kotlin::mm::ThreadData& thread) noexcept {
    auto& suspensionData = thread.suspensionData();
    return suspensionData.suspended() || suspensionData.state() == kotlin::ThreadState::kNative;
}

std::atomic<bool> kotlin::mm::internal::gSuspensionRequested = false;

kotlin::ThreadState kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState newState) noexcept {
    ThreadState oldState = state_.exchange(newState);
    if (oldState == ThreadState::kNative && newState == ThreadState::kRunnable) {
        SafePoint(threadData_); // must use already acquired thread data // TODO explain why
    }
    return oldState;
}

NO_EXTERNAL_CALLS_CHECK void kotlin::mm::ThreadSuspensionData::suspendIfRequested() noexcept {
    if (IsThreadSuspensionRequested()) {
        auto suspendStartMs = konan::getTimeMicros();
        threadData_.gc().OnSuspendForGC();
        std::unique_lock lock(gSuspensionMutex);
        auto threadId = konan::currentThreadId();
        RuntimeLogDebug({kTagGC, kTagMM}, "Suspending thread %d", threadId);
        AutoReset scopedAssignSuspended(&suspended_, true);
        gSuspensionCondVar.wait(lock, []() { return !IsThreadSuspensionRequested(); });
        auto suspendEndMs = konan::getTimeMicros();
        RuntimeLogDebug({kTagGC, kTagMM}, "Resuming thread %d after %" PRIu64 " microseconds of suspension",
                        threadId, suspendEndMs - suspendStartMs);
    }
}

NO_EXTERNAL_CALLS_CHECK bool kotlin::mm::RequestThreadsSuspension() noexcept {
    RuntimeAssert(gSuspensionRequestedByCurrentThread == false, "Current thread already suspended threads.");
    {
        std::unique_lock lock(gSuspensionMutex);
        bool safePointSet = mm::TrySetSafePointAction([](mm::ThreadData& threadData) {
            threadData.suspensionData().suspendIfRequested();
        });
        if (!safePointSet) return false;
        RuntimeAssert(!IsThreadSuspensionRequested(), "Suspension must not be requested without altering safe point action");
        internal::gSuspensionRequested = true;
    }
    gSuspensionRequestedByCurrentThread = true;

    return true;
}

void kotlin::mm::WaitForThreadsSuspension() noexcept {
    // Spin waiting for threads to suspend. Ignore Native threads.
    while(!allThreads([] (mm::ThreadData& thread) { return thread.suspensionData().suspendedOrNative(); })) {
        yield();
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
        mm::UnsetSafePointAction();
    }
    gSuspensionRequestedByCurrentThread = false;
    gSuspensionCondVar.notify_all();
}
