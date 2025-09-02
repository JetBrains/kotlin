/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CompilerConstants.hpp"
#include "ThreadData.hpp"
#include "ThreadSuspension.hpp"

#include <atomic>
#include <condition_variable>

#include "CallsChecker.hpp"
#include "Logging.hpp"
#include "Porting.h"
#include "SafePoint.hpp"

using namespace kotlin;

namespace {

[[clang::no_destroy]] thread_local std::optional<mm::SafePointActivator> gSafePointActivator = std::nullopt;
[[clang::no_destroy]] std::mutex gSuspensionRequestMutex;
[[clang::no_destroy]] std::condition_variable gSuspensionCondVar;
uint64_t nextSuspensionRequestId = 0;

} // namespace

std::atomic<uint64_t> mm::internal::gSuspensionRequestId = 0;
std::atomic<mm::internal::SuspensionReason> mm::internal::gSuspensionRequestReason = nullptr;
std::atomic<bool> mm::internal::gSuspensionRequestIsCritical = false;

PERFORMANCE_INLINE mm::ThreadSuspensionData::MutatorPauseHandle::MutatorPauseHandle(const char* reason, mm::ThreadData& threadData) noexcept
    :
    reason_(reason), threadData_(threadData), pauseStartTimeMicros_(konan::getTimeMicros()) {
    auto prevState = threadData_.suspensionData().setStateNoSafePoint(ThreadState::kNative);
    // no special reason, fill free to implement pause from native if needed
    RuntimeAssert(prevState == ThreadState::kRunnable, "Expected runnable state");
    RuntimeLogDebug({logging::Tag::kPause}, "Suspending mutation (%s)", reason_);
}

PERFORMANCE_INLINE mm::ThreadSuspensionData::MutatorPauseHandle::~MutatorPauseHandle() noexcept {
    if (!resumed) resume();
}

PERFORMANCE_INLINE void mm::ThreadSuspensionData::MutatorPauseHandle::resume() noexcept {
    RuntimeAssert(!resumed, "Must not be resumed yet");
    auto prevState = threadData_.suspensionData().setStateNoSafePoint(ThreadState::kRunnable);
    RuntimeAssert(prevState == ThreadState::kNative, "Expected native state");
    auto pauseTimeMicros = konan::getTimeMicros() - pauseStartTimeMicros_;
    RuntimeLogInfo({logging::Tag::kPause}, "Resuming mutation after %" PRIu64 " microseconds of suspension (%s)", pauseTimeMicros, reason_);
    resumed = true;
}

kotlin::ThreadState kotlin::mm::ThreadSuspensionData::setState(kotlin::ThreadState newState, bool critical) noexcept {
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
        safePoint(threadData_, critical, std::memory_order_seq_cst);
    }
    return oldState;
}

NO_EXTERNAL_CALLS_CHECK void kotlin::mm::ThreadSuspensionData::suspendIfRequested(bool critical) noexcept {
    std::unique_lock lock(gSuspensionRequestMutex);
    if (auto suspensionId = IsThreadSuspensionRequested(critical)) {
        suspensionId_.store(suspensionId, std::memory_order_relaxed);
        auto pauseHandle = pauseMutationInScope(internal::gSuspensionRequestReason.load(std::memory_order_relaxed));

        lock.unlock();
        threadData_.gc().OnSuspendForGC();
        lock.lock();
        gSuspensionCondVar.wait(lock, [critical, suspensionId]() { return IsThreadSuspensionRequested(critical) != suspensionId; });

        // Must return to running state under the lock.
        pauseHandle.resume();
        suspensionId_.store(0, std::memory_order_relaxed);
    }
}

uint64_t mm::ThreadSuspensionData::requestThreadsSuspension(const char* reason) noexcept {
    RuntimeAssert(state() == ThreadState::kRunnable, "Requesting thread suspension from the Native state may lead to a deadlock");

    while (true) {
        if (auto id = TryRequestThreadsSuspension(reason)) {
            return id;
        }
        mm::safePoint(threadData_);
    }
}

PERFORMANCE_INLINE mm::ThreadSuspensionData::MutatorPauseHandle mm::ThreadSuspensionData::pauseMutationInScope(
        const char* reason) noexcept {
    return MutatorPauseHandle(reason, threadData_);
}

uint64_t kotlin::mm::RequestThreadsSuspension(internal::SuspensionReason reason) noexcept {
    RuntimeAssert(
            !mm::ThreadRegistry::Instance().IsCurrentThreadRegistered(),
            "Registered thread must properly handle concurrent suspension requests (suspend if requested)");

    while (true) {
        if (auto id = TryRequestThreadsSuspension(reason)) {
            return id;
        }
        std::unique_lock lock(gSuspensionRequestMutex);
        gSuspensionCondVar.wait(lock, []() { return !IsThreadSuspensionRequested(); });
    }
}

uint64_t kotlin::mm::TryRequestThreadsSuspension(internal::SuspensionReason reason) noexcept {
    CallsCheckerIgnoreGuard guard;

    RuntimeAssert(gSafePointActivator == std::nullopt, "Current thread already suspended threads.");
    std::unique_lock lock(gSuspensionRequestMutex);
    // Someone else has already suspended threads.
    if (internal::gSuspensionRequestId.load(std::memory_order_relaxed)) {
        return 0;
    }
    gSafePointActivator = mm::SafePointActivator();
    internal::gSuspensionRequestReason.store(reason, std::memory_order_relaxed);
    RuntimeAssert(!internal::gSuspensionRequestIsCritical.load(std::memory_order_relaxed), "gSuspensionRequestIsCritical is set");
    auto id = ++nextSuspensionRequestId;
    internal::gSuspensionRequestId.store(id);
    return id;
}

void kotlin::mm::WaitForThreadsSuspension(uint64_t id) noexcept {
    auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
    auto* currentThread = (threadRegistry.IsCurrentThreadRegistered()) ? threadRegistry.CurrentThreadData() : nullptr;
    // Spin waiting for threads to suspend. Ignore Native threads.
    threadRegistry.waitAllThreads([currentThread, id](mm::ThreadData& thread) noexcept {
        return &thread == currentThread || thread.suspensionData().suspendedOrNative(id);
    });

    // Enter critical suspension reason
    internal::gSuspensionRequestIsCritical.store(true, std::memory_order_relaxed);

    // Need to wait for the threads again to make sure critical region is respected. Ignore Native threads.
    threadRegistry.waitAllThreads([currentThread, id](mm::ThreadData& thread) noexcept {
        return &thread == currentThread || thread.suspensionData().suspendedOrNative(id);
    });
}

void kotlin::mm::ResumeThreads(uint64_t id) noexcept {
    RuntimeAssert(gSafePointActivator != std::nullopt, "Current thread must have suspended threads");
    gSafePointActivator = std::nullopt;

    // From the std::condition_variable docs:
    // Even if the shared variable is atomic, it must be modified under
    // the mutex in order to correctly publish the modification to the waiting thread.
    // https://en.cppreference.com/w/cpp/thread/condition_variable
    {
        std::unique_lock lock(gSuspensionRequestMutex);
        internal::gSuspensionRequestIsCritical.store(false, std::memory_order_relaxed);
        internal::gSuspensionRequestReason.store(nullptr, std::memory_order_relaxed);
        auto actualId = internal::gSuspensionRequestId.exchange(0);
        RuntimeAssert(id == actualId, "Resumed threads with id %" PRIu64 " expected %" PRIu64, actualId, id);
    }
    gSuspensionCondVar.notify_all();

    auto& threadRegistry = kotlin::mm::ThreadRegistry::Instance();
    auto* currentThread = (threadRegistry.IsCurrentThreadRegistered()) ? threadRegistry.CurrentThreadData() : nullptr;
    // Spin waiting for threads to unsuspend. Ignore Native threads.
    threadRegistry.waitAllThreads([currentThread, id](mm::ThreadData& thread) noexcept {
        if (&thread == currentThread) return true;
        auto& data = thread.suspensionData();
        if (data.state() == kotlin::ThreadState::kRunnable) return true;
        auto actualId = data.suspensionId();
        if (actualId == 0) {
            return true;
        }
        return actualId != id;
    });
}
