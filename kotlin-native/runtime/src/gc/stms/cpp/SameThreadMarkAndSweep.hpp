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
    public:
        bool tryMark() noexcept {
            return trySetNext(reinterpret_cast<ObjectData*>(1));
        }

        bool marked() const noexcept { return next_ != nullptr; }

        bool tryResetMark() noexcept {
            if (next_ == nullptr) return false;
            next_ = nullptr;
            return true;
        }

    private:
        friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

        ObjectData* next() const noexcept { return next_; }
        void setNext(ObjectData* next) noexcept {
            RuntimeAssert(next, "next cannot be nullptr");
            next_ = next;
        }
        bool trySetNext(ObjectData* next) noexcept {
            RuntimeAssert(next, "next cannot be nullptr");
            if (next_ != nullptr) {
                return false;
            }
            next_ = next;
            return true;
        }

        ObjectData* next_ = nullptr;
    };

    using MarkQueue = intrusive_forward_list<ObjectData>;

    class ThreadData : private Pinned {
    public:
        using ObjectData = SameThreadMarkAndSweep::ObjectData;
        using Allocator = AllocatorWithGC<Allocator, ThreadData>;

        ThreadData(SameThreadMarkAndSweep& gc, mm::ThreadData& threadData, GCSchedulerThreadData& gcScheduler) noexcept :
            gc_(gc), gcScheduler_(gcScheduler) {}
        ~ThreadData() = default;

        void SafePointSlowPath(SafepointFlag flag) noexcept;
        void SafePointAllocation(size_t size) noexcept;

        void Schedule() noexcept { ScheduleAndWaitFullGC(); }
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

    uint64_t epoch_ = 0;

    mm::ObjectFactory<SameThreadMarkAndSweep>& objectFactory_;
    GCScheduler& gcScheduler_;

    MarkQueue markQueue_;
};

namespace internal {

struct MarkTraits {
    using MarkQueue = gc::SameThreadMarkAndSweep::MarkQueue;

    static void clear(MarkQueue& queue) noexcept { queue.clear(); }

    static ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
        if (auto* top = queue.try_pop_front()) {
            auto node = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(*top);
            return node->GetObjHeader();
        }
        return nullptr;
    }

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).ObjectData();
        return queue.try_push_front(objectData);
    }

    static bool tryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).ObjectData();
        return objectData.tryMark();
    }

    static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
        auto process = object->type_info()->processObjectInMark;
        RuntimeAssert(process != nullptr, "Got null processObjectInMark for object %p", object);
        process(static_cast<void*>(&markQueue), object);
    }
};

SameThreadMarkAndSweep::SafepointFlag loadSafepointFlag() noexcept;

} // namespace internal

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
