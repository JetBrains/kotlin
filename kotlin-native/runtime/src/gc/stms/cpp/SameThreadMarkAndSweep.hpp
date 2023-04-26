/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
#define RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H

#include <cstddef>

#include "Allocator.hpp"
#include "FinalizerProcessor.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "IntrusiveList.hpp"
#include "ObjectFactory.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

namespace mm {
class ThreadData;
}

namespace gc {

// Stop-the-world mark & sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
// TODO: Rename to StopTheWorldMarkAndSweep.
class SameThreadMarkAndSweep : private Pinned {
public:
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

        ThreadData(SameThreadMarkAndSweep& gc, mm::ThreadData& threadData, gcScheduler::GCSchedulerThreadData& gcScheduler) noexcept :
            gc_(gc), gcScheduler_(gcScheduler) {}
        ~ThreadData() = default;

        void SafePointAllocation(size_t size) noexcept;

        void Schedule() noexcept;
        void ScheduleAndWaitFullGC() noexcept;
        void ScheduleAndWaitFullGCWithFinalizers() noexcept;

        void OnOOM(size_t size) noexcept;

        Allocator CreateAllocator() noexcept { return Allocator(gc::Allocator(), *this); }

    private:

        SameThreadMarkAndSweep& gc_;
        gcScheduler::GCSchedulerThreadData& gcScheduler_;
    };

    using Allocator = ThreadData::Allocator;

    using FinalizerQueue = mm::ObjectFactory<SameThreadMarkAndSweep>::FinalizerQueue;
    using FinalizerQueueTraits = mm::ObjectFactory<SameThreadMarkAndSweep>::FinalizerQueueTraits;

    SameThreadMarkAndSweep(mm::ObjectFactory<SameThreadMarkAndSweep>& objectFactory, gcScheduler::GCScheduler& gcScheduler) noexcept;
    ~SameThreadMarkAndSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    int64_t Schedule() noexcept { return state_.schedule(); }
    void WaitFinalized(int64_t epoch) noexcept { state_.waitEpochFinalized(epoch); }

private:
    void PerformFullGC(int64_t epoch) noexcept;

    mm::ObjectFactory<SameThreadMarkAndSweep>& objectFactory_;
    gcScheduler::GCScheduler& gcScheduler_;

    GCStateHolder state_;
    ScopedThread gcThread_;
    FinalizerProcessor<FinalizerQueue, FinalizerQueueTraits> finalizerProcessor_;

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

} // namespace internal

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
