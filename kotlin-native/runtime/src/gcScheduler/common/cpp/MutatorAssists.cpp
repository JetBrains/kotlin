/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MutatorAssists.hpp"

#include "CallsChecker.hpp"
#include "KAssert.h"
#include "Logging.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

void gcScheduler::internal::MutatorAssists::ThreadData::safePoint() noexcept {
    Epoch epoch = owner_.assistsEpoch_.load(std::memory_order_acquire);
    auto noNeedToWait = [this, epoch] { return owner_.completedEpoch_.load(std::memory_order_acquire) >= epoch; };
    if (noNeedToWait()) return;
    auto pauseHandle = thread_.suspensionData().pauseMutationInScope("assisting GC");
    startedWaiting_.store(epoch * 2, std::memory_order_release);
    {
        std::unique_lock guard(owner_.m_);
        RuntimeLogDebug({kTagGC}, "Thread is assisting for epoch %" PRId64, epoch);
        owner_.cv_.wait(guard, noNeedToWait);
        RuntimeLogDebug({kTagGC}, "Thread has assisted for epoch %" PRId64, epoch);
    }
    startedWaiting_.store(epoch * 2 + 1, std::memory_order_release);
    // Not doing a safe point. We're a safe point.
}

bool gcScheduler::internal::MutatorAssists::ThreadData::completedEpoch(Epoch epoch) const noexcept {
    auto [waitingEpoch, isWaiting] = startedWaiting(std::memory_order_acquire);
    if (waitingEpoch > epoch)
        // Waiting for an epoch bigger than `epoch` => `epoch` is done here.
        return true;
    return !isWaiting;
}

void gcScheduler::internal::MutatorAssists::requestAssists(Epoch epoch) noexcept {
    RuntimeLogDebug({kTagGC}, "Requesting assists for epoch %" PRId64, epoch);
    CallsCheckerIgnoreGuard guard;
    std::unique_lock lockGuard(m_);
    if (assistsEpoch_.load(std::memory_order_relaxed) >= epoch) {
        return;
    }
    assistsEpoch_.store(epoch, std::memory_order_release);
    if (completedEpoch_.load(std::memory_order_relaxed) >= epoch) {
        return;
    }

    RuntimeLogDebug({kTagGC}, "Enabling assists for epoch %" PRId64, epoch);
    if (!safePointActivator_) {
        safePointActivator_ = mm::SafePointActivator();
    }
}

void gcScheduler::internal::MutatorAssists::markEpochCompleted(Epoch epoch) noexcept {
    RuntimeLogDebug({kTagGC}, "Disabling assists for epoch %" PRId64, epoch);
    {
        std::unique_lock guard(m_);
        auto previousEpoch = completedEpoch_.exchange(epoch, std::memory_order_release);
        RuntimeAssert(
                previousEpoch == epoch - 1, "Epochs must be increasing by 1. Previous: %" PRId64 ". Setting: %" PRId64, previousEpoch,
                epoch);
        if (epoch >= assistsEpoch_.load(std::memory_order_relaxed)) {
            safePointActivator_ = std::nullopt;
        }
    }
    cv_.notify_all();
}
