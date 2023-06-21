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

    // The protocol is: after the scheduler schedules the GC, the GC eventually calls `OnPerformFullGC`
    // when the collection has started, followed by `UpdateAliveSetBytes` when the marking has finished.
    // TODO: Consider returning a sort of future from the scheduleGC, and listen to it instead.

    // Always called by the GC thread.
    virtual void OnPerformFullGC() noexcept = 0;

    // Always called by the GC thread.
    virtual void UpdateAliveSetBytes(size_t bytes) noexcept = 0;

    // Called by different mutator threads.
    virtual void SetAllocatedBytes(size_t bytes) noexcept = 0;
};

class GCScheduler : private Pinned {
public:
    GCScheduler() noexcept;

    GCSchedulerConfig& config() noexcept { return config_; }
    GCSchedulerData& gcData() noexcept { return *gcData_; }

    // Should be called on encountering a safepoint.
    void safePoint() noexcept;

private:
    GCSchedulerConfig config_;
    std_support::unique_ptr<GCSchedulerData> gcData_;
};

} // namespace kotlin::gcScheduler
