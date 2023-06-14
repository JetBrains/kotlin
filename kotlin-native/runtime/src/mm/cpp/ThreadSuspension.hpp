/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_SUSPENSION_H
#define RUNTIME_MM_THREAD_SUSPENSION_H

#include <atomic>

#include "Memory.h"

namespace kotlin {
namespace mm {

class ThreadData;

namespace internal {

extern std::atomic<bool> gSuspensionRequested;

} // namespace internal

inline bool IsThreadSuspensionRequested() noexcept {
    // TODO: Consider using a more relaxed memory order.
    return internal::gSuspensionRequested.load();
}

class ThreadSuspensionData : private Pinned {
public:
    explicit ThreadSuspensionData(ThreadState initialState, mm::ThreadData& threadData) noexcept : state_(initialState), threadData_(threadData), suspended_(false) {}

    ~ThreadSuspensionData() = default;

    ThreadState state() noexcept { return state_; }

    ThreadState setState(ThreadState newState) noexcept {
        ThreadState oldState = state_.exchange(newState);
        if (oldState == ThreadState::kNative && newState == ThreadState::kRunnable) {
            suspendIfRequested();
        }
        return oldState;
    }

    bool suspended() noexcept { return suspended_; }

    void suspendIfRequested() noexcept {
        if (IsThreadSuspensionRequested()) {
            suspendIfRequestedSlowPath();
        }
    }

private:
    friend void SuspendIfRequestedSlowPath() noexcept;

    std::atomic<ThreadState> state_;
    mm::ThreadData& threadData_;
    std::atomic<bool> suspended_;
    void suspendIfRequestedSlowPath() noexcept;
};

bool RequestThreadsSuspension() noexcept;
void WaitForThreadsSuspension() noexcept;
void SuspendIfRequestedSlowPath() noexcept;
void SuspendIfRequested() noexcept;

/**
 * Suspends all threads registered in ThreadRegistry except threads that are in the Native state.
 * Blocks until all such threads are suspended. Threads that are in the Native state on the moment
 * of this call will be suspended on exit from the Native state.
 * Returns false if some other thread has suspended the threads.
 */
inline bool SuspendThreads() noexcept {
    if (!RequestThreadsSuspension()) {
        return false;
    }
    WaitForThreadsSuspension();
    return true;
}

/**
 * Resumes all threads registered in ThreadRegistry that were suspended by the SuspendThreads call.
 * Does not wait until all such threads are actually resumed.
 */
void ResumeThreads() noexcept;

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_SUSPENSION_H
