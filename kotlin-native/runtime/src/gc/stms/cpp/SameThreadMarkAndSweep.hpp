/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H
#define RUNTIME_GC_STMS_SAME_THREAD_MARK_AND_SWEEP_H

#include <cstddef>

#include "AllocatorImpl.hpp"
#include "FinalizerProcessor.hpp"
#include "GC.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "GlobalData.hpp"
#include "IntrusiveList.hpp"
#include "ObjectData.hpp"
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
    using MarkQueue = intrusive_forward_list<GC::ObjectData>;

    class ThreadData : private Pinned {
    public:
        ThreadData(SameThreadMarkAndSweep& gc, mm::ThreadData& threadData) noexcept {}
        ~ThreadData() = default;
    private:
    };

#ifdef CUSTOM_ALLOCATOR
    SameThreadMarkAndSweep(gcScheduler::GCScheduler& gcScheduler) noexcept;
#else
    SameThreadMarkAndSweep(
            ObjectFactory& objectFactory,
            mm::ExtraObjectDataFactory& extraObjectDataFactory,
            gcScheduler::GCScheduler& gcScheduler) noexcept;
#endif

    ~SameThreadMarkAndSweep();

    void StartFinalizerThreadIfNeeded() noexcept;
    void StopFinalizerThreadIfRunning() noexcept;
    bool FinalizersThreadIsRunning() noexcept;

    GCStateHolder& state() noexcept { return state_; }

#ifdef CUSTOM_ALLOCATOR
    alloc::Heap& heap() noexcept { return heap_; }
#endif

private:
    void PerformFullGC(int64_t epoch) noexcept;

#ifndef CUSTOM_ALLOCATOR
    ObjectFactory& objectFactory_;
    mm::ExtraObjectDataFactory& extraObjectDataFactory_;
#else
    alloc::Heap heap_;
#endif
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
            return objectForObjectData(*top);
        }
        return nullptr;
    }

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept { return queue.try_push_front(objectDataForObject(object)); }

    static bool tryMark(ObjHeader* object) noexcept { return objectDataForObject(object).tryMark(); }

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
