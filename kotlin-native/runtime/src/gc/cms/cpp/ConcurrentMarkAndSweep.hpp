/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

#include "Allocator.hpp"
#include "GCScheduler.hpp"
#include "ObjectFactory.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

namespace mm {
class ThreadData;
}

namespace gc {

// Stop-the-world Mark-and-Sweep that runs on mutator threads. Can support targets that do not have threads.
class ConcurrentMarkAndSweep : private Pinned {
public:
    enum class SafepointFlag {
        kNone,
        kNeedsSuspend,
        kNeedsGC,
    };

    class ObjectData {
    public:
        enum class Color {
            kWhite = 0, // Initial color at the start of collection cycles. Objects with this color at the end of GC cycle are collected.
                        // All new objects are allocated with this color.
            kBlack, // Objects encountered during mark phase.
        };

        Color color() const noexcept { return color_; }
        void setColor(Color color) noexcept { color_ = color; }

    private:
        Color color_ = Color::kWhite;
    };

    class ThreadData : private Pinned {
    public:
        using ObjectData = ConcurrentMarkAndSweep::ObjectData;
        using Allocator = AllocatorWithGC<AlignedAllocator, ThreadData>;

        explicit ThreadData(ConcurrentMarkAndSweep& gc, mm::ThreadData& threadData) noexcept : gc_(gc), threadData_(threadData) {}
        ~ThreadData() = default;

        void SafePointFunctionPrologue() noexcept;
        void SafePointLoopBody() noexcept;
        void SafePointExceptionUnwind() noexcept;
        void SafePointAllocation(size_t size) noexcept;

        void PerformFullGC() noexcept;

        void OnOOM(size_t size) noexcept;

        Allocator CreateAllocator() noexcept { return Allocator(AlignedAllocator(), *this); }

    private:
        void SafePointRegular(size_t weight) noexcept;
        void SafePointSlowPath(SafepointFlag flag) noexcept;

        ConcurrentMarkAndSweep& gc_;
        mm::ThreadData& threadData_;
    };

    using Allocator = ThreadData::Allocator;

    ConcurrentMarkAndSweep() noexcept;
    ~ConcurrentMarkAndSweep() = default;

private:
    // Returns `true` if GC has happened, and `false` if not (because someone else has suspended the threads).
    bool PerformFullGC() noexcept;

    size_t epoch_ = 0;
    uint64_t lastGCTimestampUs_ = 0;
};

} // namespace gc
} // namespace kotlin
