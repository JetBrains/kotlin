/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
#define RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H

#include <cstddef>

#include "Allocator.hpp"
#include "GCScheduler.hpp"
#include "IntrusiveList.hpp"
#include "ObjectFactory.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

namespace mm {
class ThreadData;
}

namespace gc {

// Stop-the-world Mark-and-Sweep that runs on mutator threads. Can support targets that do not have threads.
class SameThreadMarkAndSweep : private Pinned {
public:
    enum class SafepointFlag {
        kNone,
        kNeedsSuspend,
        kNeedsGC,
    };

    class ObjectData {
        static inline constexpr unsigned colorMask = (1 << 1) - 1;

    public:
        enum class Color : unsigned {
            kWhite = 0, // Initial color at the start of collection cycles. Objects with this color at the end of GC cycle are collected.
                        // All new objects are allocated with this color.
            kBlack, // Objects encountered during mark phase.
        };

        Color color() const noexcept { return static_cast<Color>(getPointerBits(next_, colorMask)); }
        void setColor(Color color) noexcept { next_ = setPointerBits(clearPointerBits(next_, colorMask), static_cast<unsigned>(color)); }

        ObjectData* next() const noexcept { return clearPointerBits(next_, colorMask); }
        void setNext(ObjectData* next) noexcept {
            RuntimeAssert(!hasPointerBits(next, colorMask), "next must be untagged: %p", next);
            auto bits = getPointerBits(next_, colorMask);
            next_ = setPointerBits(next, bits);
        }

    private:
        // Color is encoded in low bits.
        ObjectData* next_ = nullptr;
    };

    struct MarkQueueTraits {
        static ObjectData* next(const ObjectData& value) noexcept { return value.next(); }

        static void setNext(ObjectData& value, ObjectData* next) noexcept { value.setNext(next); }
    };

    using MarkQueue = intrusive_forward_list<ObjectData, MarkQueueTraits>;

    class ThreadData : private Pinned {
    public:
        using ObjectData = SameThreadMarkAndSweep::ObjectData;
        using Allocator = AllocatorWithGC<Allocator, ThreadData>;

        ThreadData(SameThreadMarkAndSweep& gc, mm::ThreadData& threadData, GCSchedulerThreadData& gcScheduler) noexcept :
            gc_(gc), gcScheduler_(gcScheduler) {}
        ~ThreadData() = default;

        void SafePointSlowPath(SafepointFlag flag) noexcept;
        void SafePointAllocation(size_t size) noexcept;

        void ScheduleAndWaitFullGC() noexcept;
        void ScheduleAndWaitFullGCWithFinalizers() noexcept { ScheduleAndWaitFullGC(); }

        void OnOOM(size_t size) noexcept;

        Allocator CreateAllocator() noexcept { return Allocator(gc::Allocator(), *this); }

    private:

        SameThreadMarkAndSweep& gc_;
        GCSchedulerThreadData& gcScheduler_;
    };

    using Allocator = ThreadData::Allocator;

    SameThreadMarkAndSweep(mm::ObjectFactory<SameThreadMarkAndSweep>& objectFactory, GCScheduler& gcScheduler) noexcept;
    ~SameThreadMarkAndSweep() = default;

private:
    // Returns `true` if GC has happened, and `false` if not (because someone else has suspended the threads).
    bool PerformFullGC() noexcept;

    size_t epoch_ = 0;
    uint64_t lastGCTimestampUs_ = 0;

    mm::ObjectFactory<SameThreadMarkAndSweep>& objectFactory_;
    GCScheduler& gcScheduler_;

    MarkQueue markQueue_;
};

namespace internal {

SameThreadMarkAndSweep::SafepointFlag loadSafepointFlag() noexcept;

} // namespace internal

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
