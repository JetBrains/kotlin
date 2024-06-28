/*
 * Copyright 2022-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>

namespace kotlin::alloc {

struct AllocatedSizeTracker {
    class Page {
    public:
        void onPageOverflow(std::size_t allocatedBytes) noexcept;
        void afterSweep(std::size_t allocatedBytes) noexcept;

    private:
        std::size_t allocatedBytesLastRecorded_ = 0;
    };

    class Heap {
    public:
        void recordDifference(std::ptrdiff_t diffBytes, bool notifyScheduler) noexcept;
    private:
        std::atomic<std::ptrdiff_t> allocatedBytes_ = 0;
    };
};

}
