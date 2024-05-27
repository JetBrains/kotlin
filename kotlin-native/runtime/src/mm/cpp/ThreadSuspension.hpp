/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_SUSPENSION_H
#define RUNTIME_MM_THREAD_SUSPENSION_H

#include <atomic>

#include "Memory.h"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadData;

namespace internal {

using SuspensionReason = const char*;
extern std::atomic<SuspensionReason> gSuspensionReauestReason;

} // namespace internal

inline bool IsThreadSuspensionRequested() noexcept {
    // Must use seq_cst ordering for synchronization with GC
    // in native->runnable transition.
    return internal::gSuspensionReauestReason.load();
}

class ThreadSuspensionData : private Pinned {
private:
    class MutatorPauseHandle : private Pinned {
    public:
        explicit MutatorPauseHandle(const char* reason, ThreadData& threadData) noexcept;
        ~MutatorPauseHandle() noexcept;
        void resume() noexcept;
    private:
        const char* reason_;
        ThreadData& threadData_;
        uint64_t pauseStartTimeMicros_;
        bool resumed = false;
    };

public:
    explicit ThreadSuspensionData(ThreadState initialState, mm::ThreadData& threadData) noexcept : state_(initialState), threadData_(threadData) {}

    ~ThreadSuspensionData() = default;

    ThreadState state() noexcept { return state_; }

    ThreadState setState(ThreadState newState) noexcept;
    ThreadState setStateNoSafePoint(ThreadState newState) noexcept { return state_.exchange(newState, std::memory_order_acq_rel); }

    bool suspendedOrNative() noexcept { return state() == kotlin::ThreadState::kNative; }

    void suspendIfRequested() noexcept;

    void requestThreadsSuspension(const char* reason) noexcept;

    /**
     * Signals that the thread would not mutate a heap during a relatively long time.
     * For example while waiting for or participating in the GC.
     * Effectively sets the thread's state to `kNative`.
     *
     * The pause is considered completed upon destruction of a returned pause-handle object.
     *
     * NOTE: the safe point actions will NOT be automatically executed after the pause.
     */
    [[nodiscard]] MutatorPauseHandle pauseMutationInScope(const char* reason) noexcept;

private:
    std::atomic<ThreadState> state_;
    mm::ThreadData& threadData_;
};

/**
 * Sends a request to suspend all threads registered in ThreadRegistry except threads that are in the Native state.
 * Blocks until all such threads are suspended. Threads that are in the Native state on the moment
 * of this call will be suspended on exit from the Native state.
 *
 * Returns false if some other thread tries to suspended the threads at the moment.
 */
bool TryRequestThreadsSuspension(const char* reason) noexcept;
void RequestThreadsSuspension(const char* reason) noexcept;

void WaitForThreadsSuspension() noexcept;

inline bool SuspendThreads(internal::SuspensionReason reason) noexcept {
    if (!TryRequestThreadsSuspension(reason)) {
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
