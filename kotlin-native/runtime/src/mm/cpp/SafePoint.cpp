/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SafePoint.hpp"

#include <atomic>

#include "GCScheduler.hpp"
#include "KAssert.h"
#include "Logging.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

// TODO: Remove after the bootstrap that brings changes in ClangArgs.kt
#ifndef KONAN_SUPPORTS_SIGNPOSTS
#define KONAN_SUPPORTS_SIGNPOSTS KONAN_MACOSX || KONAN_IOS || KONAN_WATCHOS || KONAN_TVOS
#endif

#if KONAN_SUPPORTS_SIGNPOSTS
#include <os/log.h>
#include <os/signpost.h>
#endif

using namespace kotlin;

namespace {

[[clang::no_destroy]] std::mutex safePointActionMutex;
int64_t activeCount = 0;
std::atomic<void (*)(mm::ThreadData&)> safePointAction = nullptr;

#if KONAN_SUPPORTS_SIGNPOSTS

#define SAFEPOINT_SIGNPOST_NAME "Safepoint" // signpost API requires strings be literals

class SafePointSignpostInterval : private Pinned {
public:
    explicit SafePointSignpostInterval(mm::ThreadData& threadData) noexcept : id_(os_signpost_id_make_with_pointer(logObject, &threadData)) {
        os_signpost_interval_begin(logObject, id_, SAFEPOINT_SIGNPOST_NAME, "thread id: %d", threadData.threadId());
    }

    ~SafePointSignpostInterval() {
        os_signpost_interval_end(logObject, id_, SAFEPOINT_SIGNPOST_NAME);
    }

private:
    static os_log_t logObject;
    uint64_t id_;
};

#undef SAFEPOINT_SIGNPOST_NAME

// static
os_log_t SafePointSignpostInterval::logObject = os_log_create("org.kotlinlang.native.runtime", "safepoint");
#else
class SafePointSignpostInterval : private Pinned {
public:
    explicit SafePointSignpostInterval(mm::ThreadData& threadData) noexcept {}
};
#endif

void safePointActionImpl(mm::ThreadData& threadData) noexcept {
    static thread_local bool recursion = false;
    RuntimeAssert(!recursion, "Recursive safepoint");
    AutoReset guard(&recursion, true);

    std::optional<SafePointSignpostInterval> signpost;
    if (compiler::enableSafepointSignposts()) {
        signpost.emplace(threadData);
    }
    threadData.gcScheduler().safePoint();
    threadData.gc().safePoint();
    threadData.suspensionData().suspendIfRequested();
}

ALWAYS_INLINE void slowPathImpl(mm::ThreadData& threadData) noexcept {
    // reread an action to avoid register pollution outside the function
    auto action = safePointAction.load(std::memory_order_seq_cst);
    if (action != nullptr) {
        action(threadData);
    }
}

NO_INLINE void slowPath() noexcept {
    slowPathImpl(*mm::ThreadRegistry::Instance().CurrentThreadData());
}

NO_INLINE void slowPath(mm::ThreadData& threadData) noexcept {
    slowPathImpl(threadData);
}

void incrementActiveCount() noexcept {
    std::unique_lock guard{safePointActionMutex};
    ++activeCount;
    RuntimeAssert(activeCount >= 1, "Unexpected activeCount: %" PRId64, activeCount);
    if (activeCount == 1) {
        RuntimeLogDebug({kTagMM}, "Enabling safe points");
        auto prev = safePointAction.exchange(safePointActionImpl, std::memory_order_seq_cst);
        RuntimeAssert(prev == nullptr, "Action cannot have been set. Was %p", prev);
    }
}

void decrementActiveCount() noexcept {
    std::unique_lock guard{safePointActionMutex};
    --activeCount;
    RuntimeAssert(activeCount >= 0, "Unexpected activeCount: %" PRId64, activeCount);
    if (activeCount == 0) {
        auto prev = safePointAction.exchange(nullptr, std::memory_order_seq_cst);
        RuntimeAssert(prev == safePointActionImpl, "Action must have been %p. Was %p", safePointActionImpl, prev);
        RuntimeLogDebug({kTagMM}, "Disabled safe points");
    }
}

} // namespace

mm::SafePointActivator::SafePointActivator() noexcept : active_(true) {
    incrementActiveCount();
}

mm::SafePointActivator::~SafePointActivator() {
    if (active_) {
        decrementActiveCount();
    }
}

ALWAYS_INLINE void mm::safePoint(std::memory_order fastPathOrder) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    auto action = safePointAction.load(fastPathOrder);
    if (__builtin_expect(action != nullptr, false)) {
        slowPath();
    }
}

ALWAYS_INLINE void mm::safePoint(mm::ThreadData& threadData, std::memory_order fastPathOrder) noexcept {
    AssertThreadState(&threadData, ThreadState::kRunnable);
    auto action = safePointAction.load(fastPathOrder);
    if (__builtin_expect(action != nullptr, false)) {
        slowPath(threadData);
    }
}

bool mm::test_support::safePointsAreActive() noexcept {
    return safePointAction.load(std::memory_order_relaxed) != nullptr;
}

void mm::test_support::setSafePointAction(void (*action)(mm::ThreadData&)) noexcept {
    safePointAction.store(action, std::memory_order_seq_cst);
}
