/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"

#include <condition_variable>
#include <thread>
#include <mutex>

namespace {

bool isSuspendedOrNative(kotlin::mm::ThreadData& thread) noexcept {
    auto& suspensionData = thread.suspensionData();
    return suspensionData.suspended() || suspensionData.state() == kotlin::ThreadState::kNative;
}

template<typename F>
bool allThreads(F predicate) noexcept {
    auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
    auto* currentThread = threadRegistry.CurrentThreadData();
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

std::atomic<bool> gSuspensionRequested = false;
THREAD_LOCAL_VARIABLE bool gSuspensionRequestedByCurrentThread = false;
std::mutex gSuspensionMutex;
std::condition_variable gSuspendsionCondVar;

} // namespace

NO_EXTERNAL_CALLS_CHECK bool kotlin::mm::ThreadSuspensionData::suspendIfRequested() noexcept {
    if (IsThreadSuspensionRequested()) {
        return suspendIfRequestedSlowPath();
    }
    return false;
}

NO_EXTERNAL_CALLS_CHECK bool kotlin::mm::ThreadSuspensionData::suspendIfRequestedSlowPath() noexcept {
    std::unique_lock lock(gSuspensionMutex);
    if (IsThreadSuspensionRequested()) {
        AutoReset scopedAssign(&suspended_, true);
        gSuspendsionCondVar.wait(lock, []() { return !IsThreadSuspensionRequested(); });
        return true;
    }
    return false;
}

bool kotlin::mm::IsThreadSuspensionRequested() noexcept {
    // TODO: Consider using a more relaxed memory order.
    return gSuspensionRequested.load();
}

bool kotlin::mm::SuspendThreads() noexcept {
    RuntimeAssert(gSuspensionRequestedByCurrentThread == false, "Current thread already suspended threads.");
    {
        std::unique_lock lock(gSuspensionMutex);
        bool actual = false;
        gSuspensionRequested.compare_exchange_strong(actual, true);
        if (actual) {
            return false;
        }
    }
    gSuspensionRequestedByCurrentThread = true;

    // Spin wating for threads to suspend. Ignore Native threads.
    while(!allThreads(isSuspendedOrNative)) {
        yield();
    }

    return true;
}

void kotlin::mm::ResumeThreads() noexcept {
    // From the std::condition_variable docs:
    // Even if the shared variable is atomic, it must be modified under
    // the mutex in order to correctly publish the modification to the waiting thread.
    // https://en.cppreference.com/w/cpp/thread/condition_variable
    {
        std::unique_lock lock(gSuspensionMutex);
        gSuspensionRequested = false;
    }
    gSuspensionRequestedByCurrentThread = false;
    gSuspendsionCondVar.notify_all();
}
