/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"

#include <condition_variable>

#include "CallsChecker.hpp"
#include "Logging.hpp"
#include "Porting.h"
#include "SafePoint.hpp"

using namespace kotlin;

namespace {

[[clang::no_destroy]] thread_local std::optional<mm::SafePointActivator> gSafePointActivator = std::nullopt;
[[clang::no_destroy]] std::mutex gSuspensionMutex;
[[clang::no_destroy]] std::condition_variable gSuspensionCondVar;

} // namespace

std::atomic<mm::internal::SuspensionReason> mm::internal::gSuspensionReauestReason = nullptr;

ALWAYS_INLINE mm::ThreadSuspensionData::MutatorPauseHandle::MutatorPauseHandle(const char* reason, mm::ThreadData& threadData) noexcept
    : reason_(reason), threadData_(threadData), pauseStartTimeMicros_(konan::getTimeMicros())
{
    auto prevState = threadData_.suspensionData().setStateNoSafePoint(ThreadState::kNative);
    // no special reason, fill free to implement pause from native if needed
    RuntimeAssert(prevState == ThreadState::kRunnable, "Expected runnable state");
    RuntimeLogDebug({logging::Tag::kPause}, "Suspending mutation (%s)", reason_);
}

ALWAYS_INLINE mm::ThreadSuspensionData::MutatorPauseHandle::~MutatorPauseHandle() noexcept {
    if (!resumed) resume();
}

ALWAYS_INLINE void mm::ThreadSuspensionData::MutatorPauseHandle::resume() noexcept {
    RuntimeAssert(!resumed, "Must not be resumed yet");
    auto prevState = threadData_.suspensionData().setStateNoSafePoint(ThreadState::kRunnable);
    RuntimeAssert(prevState == ThreadState::kNative, "Expected native state");
    auto pauseTimeMicros = konan::getTimeMicros() - pauseStartTimeMicros_;
    RuntimeLogInfo({logging::Tag::kPause}, "Resuming mutation after %" PRIu64 " microseconds of suspension (%s)", pauseTimeMicros, reason_);
    resumed = true;
}

kotlin::ThreadState kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState newState) noexcept {
    ThreadState oldState = state_.exchange(newState);
    if (oldState == ThreadState::kNative && newState == ThreadState::kRunnable) {
        // Must use already acquired `ThreadData` because TLS may be in invalid state e.g. during thread detach.
        // Also, this must load SP in sequentially consistent order, because GC
        // may have touched this thread's data, and we must synchronize before
        // continuing.
        // GC would have either changed stored SP handler (with seq_cst),
        // or would have changed `internal::gSuspensionRequested` (with seq_cst),
        // so, loading SP here, or checking `internal::gSuspensionRequested` in
        // `suspendIfRequested` is enough.
        safePoint(threadData_, std::memory_order_seq_cst);
    }
    return oldState;
}

NO_EXTERNAL_CALLS_CHECK void kotlin::mm::ThreadSuspensionData::suspendIfRequested() noexcept {
    if (IsThreadSuspensionRequested()) {
        auto pauseHandle = pauseMutationInScope(internal::gSuspensionReauestReason.load(std::memory_order_relaxed));

        threadData_.gc().OnSuspendForGC();
        std::unique_lock lock(gSuspensionMutex);
        gSuspensionCondVar.wait(lock, []() { return !IsThreadSuspensionRequested(); });

        // Must return to running state under the lock.
        pauseHandle.resume();
    }
}

ALWAYS_INLINE mm::ThreadSuspensionData::MutatorPauseHandle mm::ThreadSuspensionData::pauseMutationInScope(const char* reason) noexcept {
    return MutatorPauseHandle(reason, threadData_);
}

bool kotlin::mm::RequestThreadsSuspension(internal::SuspensionReason reason) noexcept {
    CallsCheckerIgnoreGuard guard;

    RuntimeAssert(gSafePointActivator == std::nullopt, "Current thread already suspended threads.");
    {
        std::unique_lock lock(gSuspensionMutex);
        // Someone else has already suspended threads.
        if (internal::gSuspensionReauestReason.load(std::memory_order_relaxed) != nullptr) {
            return false;
        }
        gSafePointActivator = mm::SafePointActivator();
        internal::gSuspensionReauestReason.store(reason);
    }

    return true;
}

void kotlin::mm::WaitForThreadsSuspension() noexcept {
    auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
    auto* currentThread = (threadRegistry.IsCurrentThreadRegistered()) ? threadRegistry.CurrentThreadData() : nullptr;
    // Spin waiting for threads to suspend. Ignore Native threads.
    threadRegistry.waitAllThreads([currentThread](mm::ThreadData& thread) noexcept {
        return &thread == currentThread || thread.suspensionData().suspendedOrNative();
    });
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
        internal::gSuspensionReauestReason.store(nullptr);
    }
    gSuspensionCondVar.notify_all();
}
