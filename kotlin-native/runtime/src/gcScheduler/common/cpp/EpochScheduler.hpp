/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <functional>
#include <mutex>
#include <optional>

#include "Utils.hpp"
#include "Logging.hpp"

namespace kotlin::gcScheduler {

class ScheduleReason {
    static constexpr auto kManually = std::numeric_limits<std::size_t>::max();
    static constexpr auto kByTimer = kManually - 1;
    static constexpr auto kBySafePoint = kByTimer - 1;

    explicit ScheduleReason(std::size_t data) noexcept : data_(data) {}
public:
    static auto manually() noexcept { return ScheduleReason{kManually}; }
    static auto byTimer() noexcept { return ScheduleReason{kByTimer}; }
    static auto bySafePoint() noexcept { return ScheduleReason{kBySafePoint}; }
    static auto byAllocation(std::size_t totalAllocatedBytes) noexcept { return ScheduleReason{totalAllocatedBytes}; }

    ALWAYS_INLINE void log() const noexcept {
        constexpr auto tag = logging::Tag::kGCScheduler;
        if (data_ == kManually) {
            RuntimeLogInfo({tag}, "Scheduling GC manually");
        } else if (data_ == kByTimer) {
            RuntimeLogInfo({tag}, "Scheduling GC by timer");
        } else if (data_ == kBySafePoint) {
            RuntimeLogInfo({tag}, "Scheduling GC by safepoint");
        } else {
            RuntimeLogInfo({tag}, "Scheduling GC by allocation: total allocated %zu bytes", data_);
        }
    }
private:
    std::size_t data_;
};

namespace internal {

// Control scheduling new GC epochs.
// TODO: The actual logic is split between this class and `gc::GCState`. The latter
//       should be merged into this one.
class EpochScheduler : private Pinned {
public:
    using Epoch = int64_t;

    explicit EpochScheduler(std::function<Epoch()> scheduleGC) noexcept : scheduleGC_(std::move(scheduleGC)) {}

    // Schedule the next GC epoch unless the GC has a scheduled epoch already.
    // If the GC is currently performing collection, this will schedule the next one.
    // Returns the currently scheduled GC epoch.
    Epoch scheduleNextEpoch(ScheduleReason reason) noexcept;

    // Schedule the next GC epoch unless the GC has a scheduled epoch, or is currently
    // performing collection. If the GC is currently processing finalizers, this will
    // schedule the next GC collection.
    // Returns the currently scheduled GC epoch.
    Epoch scheduleNextEpochIfNotInProgress(ScheduleReason reason) noexcept;

    // Must be called when the GC has finished collection.
    void onGCFinish(Epoch epoch) noexcept;

private:
    std::function<Epoch()> scheduleGC_;
    std::optional<int64_t> scheduledEpoch_;
    std::mutex scheduledEpochMutex_;
};

} // namespace internal
} // namespace kotlin::gcScheduler::internal
