/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleThreadMarkAndSweep.hpp"

#include "GlobalData.hpp"
#include "MarkAndSweepUtils.hpp"
#include "Memory.h"
#include "RootSet.hpp"
#include "Runtime.h"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadSuspension.hpp"

using namespace kotlin;

namespace {

struct MarkTraits {
    static bool IsMarked(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SingleThreadMarkAndSweep>::NodeRef::From(object).GCObjectData();
        return objectData.color() == gc::SingleThreadMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SingleThreadMarkAndSweep>::NodeRef::From(object).GCObjectData();
        if (objectData.color() == gc::SingleThreadMarkAndSweep::ObjectData::Color::kBlack) return false;
        objectData.setColor(gc::SingleThreadMarkAndSweep::ObjectData::Color::kBlack);
        return true;
    };
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SingleThreadMarkAndSweep>;

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.GCObjectData();
        if (objectData.color() == gc::SingleThreadMarkAndSweep::ObjectData::Color::kWhite) return false;
        objectData.setColor(gc::SingleThreadMarkAndSweep::ObjectData::Color::kWhite);
        return true;
    }
};

struct FinalizeTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SingleThreadMarkAndSweep>;
};

} // namespace

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointFunctionEpilogue() noexcept {
    SafePointRegular(1);
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    SafePointRegular(1);
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointExceptionUnwind() noexcept {
    SafePointRegular(1);
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    size_t allocationOverhead =
            gc_.GetAllocationThresholdBytes() == 0 ? allocatedBytes_ : allocatedBytes_ % gc_.GetAllocationThresholdBytes();
    if (threadData_.suspensionData().suspendIfRequested()) {
        allocatedBytes_ = 0;
    } else if (allocationOverhead + size >= gc_.GetAllocationThresholdBytes()) {
        allocatedBytes_ = 0;
        PerformFullGC();
    }
    allocatedBytes_ += size;
}

void gc::SingleThreadMarkAndSweep::ThreadData::PerformFullGC() noexcept {
    mm::ObjectFactory<gc::SingleThreadMarkAndSweep>::FinalizerQueue finalizerQueue;
    {
        // Switch state to native to simulate this thread being a GC thread.
        // As a bonus, if we failed to suspend threads (which means some other thread asked for a GC),
        // we will automatically suspend at the scope exit.
        // TODO: Cannot use `threadData_` here, because there's no way to transform `mm::ThreadData` into `MemoryState*`.
        ThreadStateGuard guard(ThreadState::kNative);
        finalizerQueue = gc_.PerformFullGC();
    }

    // Finalizers are run after threads are resumed, because finalizers may request GC themselves, which would
    // try to suspend threads again. Also, we run finalizers in the runnable state, because they may be executing
    // kotlin code.

    // TODO: These will actually need to be run on a separate thread.
    // TODO: Cannot use `threadData_` here, because there's no way to transform `mm::ThreadData` into `MemoryState*`.
    AssertThreadState(ThreadState::kRunnable);
    finalizerQueue.Finalize();
}

void gc::SingleThreadMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    PerformFullGC();
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointRegular(size_t weight) noexcept {
    size_t counterOverhead = gc_.GetThreshold() == 0 ? safePointsCounter_ : safePointsCounter_ % gc_.GetThreshold();
    if (threadData_.suspensionData().suspendIfRequested()) {
        safePointsCounter_ = 0;
    } else if (counterOverhead + weight >= gc_.GetThreshold()) {
        safePointsCounter_ = 0;
        PerformFullGC();
    }
    safePointsCounter_ += weight;
}

mm::ObjectFactory<gc::SingleThreadMarkAndSweep>::FinalizerQueue gc::SingleThreadMarkAndSweep::PerformFullGC() noexcept {
    bool didSuspend = mm::SuspendThreads();
    if (!didSuspend) {
        // Somebody else suspended the threads, and so ran a GC.
        // TODO: This breaks if suspension is used by something apart from GC.
        return {};
    }

    KStdVector<ObjHeader*> graySet;
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().Iter()) {
        // TODO: Maybe it's more efficient to do by the suspending thread?
        thread.Publish();
        for (auto* object : mm::ThreadRootSet(thread)) {
            if (!isNullOrMarker(object)) {
                graySet.push_back(object);
            }
        }
    }
    mm::StableRefRegistry::Instance().ProcessDeletions();
    for (auto* object : mm::GlobalRootSet()) {
        if (!isNullOrMarker(object)) {
            graySet.push_back(object);
        }
    }

    gc::Mark<MarkTraits>(std::move(graySet));
    auto finalizerQueue = gc::Sweep<SweepTraits>(mm::GlobalData::Instance().objectFactory());

    mm::ResumeThreads();

    return finalizerQueue;
}
