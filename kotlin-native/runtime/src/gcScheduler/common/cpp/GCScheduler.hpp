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

namespace kotlin::mm {
class ThreadData;
}

namespace kotlin::gcScheduler {

class GCScheduler : private Pinned {
public:
    class Impl;

    class ThreadData : private Pinned {
    public:
        class Impl;

        ThreadData(GCScheduler&, mm::ThreadData&) noexcept;
        ~ThreadData();

        Impl& impl() noexcept { return *impl_; }

        void safePoint() noexcept;

    private:
        std_support::unique_ptr<Impl> impl_;
    };

    GCScheduler() noexcept;
    ~GCScheduler();

    Impl& impl() noexcept { return *impl_; }

    GCSchedulerConfig& config() noexcept { return config_; }

    // Called by different mutator threads.
    void setAllocatedBytes(size_t bytes) noexcept;

    // Can be called by any thread.
    void schedule() noexcept;

    // Can be called by any thread.
    void scheduleAndWaitFinished() noexcept;

    // Can be called by any thread.
    void scheduleAndWaitFinalized() noexcept;

    // Always called by the GC thread.
    void onGCStart() noexcept;

    // Called by the GC thread only.
    void onGCFinish(int64_t epoch, size_t aliveBytes) noexcept;

private:
    GCSchedulerConfig config_;
    std_support::unique_ptr<Impl> impl_;
};

} // namespace kotlin::gcScheduler
