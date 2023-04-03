/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <functional>
#include <utility>

#include "GCSchedulerConfig.hpp"
#include "KAssert.h"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

namespace kotlin::gcScheduler {

namespace test_support {
class GCSchedulerThreadDataTestApi;
}

class GCSchedulerThreadData;

class GCSchedulerData {
public:
    virtual ~GCSchedulerData() = default;

    // Called by different mutator threads.
    virtual void UpdateFromThreadData(GCSchedulerThreadData& threadData) noexcept = 0;

    // The protocol is: after the scheduler schedules the GC, the GC eventually calls `OnPerformFullGC`
    // when the collection has started, followed by `UpdateAliveSetBytes` when the marking has finished.
    // TODO: Consider returning a sort of future from the scheduleGC, and listen to it instead.

    // Always called by the GC thread.
    virtual void OnPerformFullGC() noexcept = 0;

    // Always called by the GC thread.
    virtual void UpdateAliveSetBytes(size_t bytes) noexcept = 0;
};

class GCSchedulerThreadData {
public:
    explicit GCSchedulerThreadData(GCSchedulerConfig& config, std::function<void(GCSchedulerThreadData&)> slowPath) noexcept :
        config_(config), slowPath_(std::move(slowPath)) {
        ClearCountersAndUpdateThresholds();
    }

    // Should be called on encountering a safepoint placed by the allocator.
    // TODO: Should this even be a safepoint (i.e. a place, where we suspend)?
    void OnSafePointAllocation(size_t size) noexcept {
        allocatedBytes_ += size;
        if (allocatedBytes_ < allocatedBytesThreshold_) {
            return;
        }
        OnSafePointSlowPath();
    }

    void OnStoppedForGC() noexcept { ClearCountersAndUpdateThresholds(); }

    size_t allocatedBytes() const noexcept { return allocatedBytes_; }

private:
    friend class test_support::GCSchedulerThreadDataTestApi;

    void OnSafePointSlowPath() noexcept {
        slowPath_(*this);
        ClearCountersAndUpdateThresholds();
    }

    void ClearCountersAndUpdateThresholds() noexcept {
        allocatedBytes_ = 0;

        allocatedBytesThreshold_ = config_.allocationThresholdBytes;
    }

    GCSchedulerConfig& config_;
    std::function<void(GCSchedulerThreadData&)> slowPath_;

    size_t allocatedBytes_ = 0;
    size_t allocatedBytesThreshold_ = 0;
};

class GCScheduler : private Pinned {
public:
    GCScheduler() noexcept;

    GCSchedulerConfig& config() noexcept { return config_; }
    GCSchedulerData& gcData() noexcept { return *gcData_; }

    GCSchedulerThreadData NewThreadData() noexcept {
        return GCSchedulerThreadData(config_, [this](auto& threadData) { gcData_->UpdateFromThreadData(threadData); });
    }

    // Should be called on encountering a safepoint.
    void safePoint() noexcept;

private:
    GCSchedulerConfig config_;
    std_support::unique_ptr<GCSchedulerData> gcData_;
};

} // namespace kotlin::gcScheduler
