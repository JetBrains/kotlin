/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "EpochScheduler.hpp"

#include <cinttypes>

#include "CallsChecker.hpp"
#include "KAssert.h"

using namespace kotlin;

using Epoch = gcScheduler::internal::EpochScheduler::Epoch;

Epoch gcScheduler::internal::EpochScheduler::scheduleNextEpoch() noexcept {
    // The locks here are always short-lived,
    // so we ignore thread state switching to avoid recursive safe points.
    CallsCheckerIgnoreGuard ignoreGuard;
    std::unique_lock guard(scheduledEpochMutex_);
    auto epoch = scheduleGC_();
    if (auto scheduled = scheduledEpoch_) {
        RuntimeAssert(
                *scheduled <= epoch, "Scheduled epoch %" PRId64 " which is somehow less previously scheduled %" PRId64, epoch, *scheduled);
    }
    scheduledEpoch_ = epoch;
    return epoch;
}

Epoch gcScheduler::internal::EpochScheduler::scheduleNextEpochIfNotInProgress() noexcept {
    // The locks here are always short-lived,
    // so we ignore thread state switching to avoid recursive safe points.
    CallsCheckerIgnoreGuard ignoreGuard;
    std::unique_lock guard(scheduledEpochMutex_);
    if (auto scheduled = scheduledEpoch_) {
        return *scheduled;
    }
    auto epoch = scheduleGC_();
    scheduledEpoch_ = epoch;
    return epoch;
}

void gcScheduler::internal::EpochScheduler::onGCFinish(Epoch epoch) noexcept {
    std::unique_lock guard(scheduledEpochMutex_);
    RuntimeAssert(scheduledEpoch_ != std::nullopt, "GC for epoch %" PRId64 " happened without going through the GC scheduler", epoch);
    auto scheduled = *scheduledEpoch_;
    RuntimeAssert(
            scheduled >= epoch, "GC for epoch %" PRId64 " happened without going through the GC scheduler, which was waiting for %" PRId64,
            epoch, scheduled);
    if (scheduled > epoch)
        // Waiting for one of the next GC epochs to finish.
        return;
    // Current GC epoch is finished.
    scheduledEpoch_ = std::nullopt;
}
