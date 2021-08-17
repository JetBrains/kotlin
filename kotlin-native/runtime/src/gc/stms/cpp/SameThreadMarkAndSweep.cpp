/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SameThreadMarkAndSweep.hpp"

#include "CompilerConstants.hpp"
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
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).GCObjectData();
        return objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack;
    }

    static bool TryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(object).GCObjectData();
        if (objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack) return false;
        objectData.setColor(gc::SameThreadMarkAndSweep::ObjectData::Color::kBlack);
        return true;
    };
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.GCObjectData();
        if (objectData.color() == gc::SameThreadMarkAndSweep::ObjectData::Color::kWhite) return false;
        objectData.setColor(gc::SameThreadMarkAndSweep::ObjectData::Color::kWhite);
        return true;
    }
};

struct FinalizeTraits {
    using ObjectFactory = mm::ObjectFactory<gc::SameThreadMarkAndSweep>;
};

} // namespace

void gc::SameThreadMarkAndSweep::ThreadData::SafePointFunctionEpilogue() noexcept {
    SafePointRegular(1);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    SafePointRegular(1);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointExceptionUnwind() noexcept {
    SafePointRegular(1);
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
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

void gc::SameThreadMarkAndSweep::ThreadData::PerformFullGC() noexcept {
    mm::ObjectFactory<gc::SameThreadMarkAndSweep>::FinalizerQueue finalizerQueue;
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

void gc::SameThreadMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    PerformFullGC();
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointRegularSlowPath() noexcept {
    safePointsCounter_ = 0;
    if (konan::getTimeMicros() - timeOfLastGcUs_ >= gc_.GetCooldownThresholdUs()) {
        timeOfLastGcUs_ = konan::getTimeMicros();
        PerformFullGC();
    }
}

void gc::SameThreadMarkAndSweep::ThreadData::SafePointRegular(size_t weight) noexcept {
    if (threadData_.suspensionData().suspendIfRequested()) {
        safePointsCounter_ = 0;
    } else {
        safePointsCounter_ += weight;
        if (safePointsCounter_ >= gc_.GetThreshold()) {
            SafePointRegularSlowPath();
        }
    }
}

gc::SameThreadMarkAndSweep::SameThreadMarkAndSweep() noexcept {
    if (compiler::gcAggressive()) {
        // TODO: Make it even more aggressive and run on a subset of backend.native tests.
        threshold_ = 1000;
        allocationThresholdBytes_ = 10000;
        cooldownThresholdUs_ = 0;
    }
}

mm::ObjectFactory<gc::SameThreadMarkAndSweep>::FinalizerQueue gc::SameThreadMarkAndSweep::PerformFullGC() noexcept {
    bool didSuspend = mm::SuspendThreads();
    if (!didSuspend) {
        // Somebody else suspended the threads, and so ran a GC.
        // TODO: This breaks if suspension is used by something apart from GC.
        return {};
    }

    KStdVector<ObjHeader*> graySet;
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
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
