/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <functional>
#include <mutex>
#include <optional>

#include "Utils.hpp"

namespace kotlin::gcScheduler::internal {

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
    Epoch scheduleNextEpoch() noexcept;

    // Schedule the next GC epoch unless the GC has a scheduled epoch, or is currently
    // performing collection. If the GC is currently processing finalizers, this will
    // schedule the next GC collection.
    // Returns the currently scheduled GC epoch.
    Epoch scheduleNextEpochIfNotInProgress() noexcept;

    // Must be called when the GC has finished collection.
    void onGCFinish(Epoch epoch) noexcept;

private:
    std::function<Epoch()> scheduleGC_;
    std::optional<int64_t> scheduledEpoch_;
    std::mutex scheduledEpochMutex_;
};

} // namespace kotlin::gcScheduler::internal
