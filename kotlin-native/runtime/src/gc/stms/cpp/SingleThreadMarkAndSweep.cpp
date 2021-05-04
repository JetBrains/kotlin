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
    if (gc_.GetThreshold() == 0 || safePointsCounter_ % gc_.GetThreshold() == 0) {
        PerformFullGC();
    }
    ++safePointsCounter_;
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    if (gc_.GetThreshold() == 0 || safePointsCounter_ % gc_.GetThreshold() == 0) {
        PerformFullGC();
    }
    ++safePointsCounter_;
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointExceptionUnwind() noexcept {
    if (gc_.GetThreshold() == 0 || safePointsCounter_ % gc_.GetThreshold() == 0) {
        PerformFullGC();
    }
    ++safePointsCounter_;
}

void gc::SingleThreadMarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    size_t allocationOverhead =
            gc_.GetAllocationThresholdBytes() == 0 ? allocatedBytes_ : allocatedBytes_ % gc_.GetAllocationThresholdBytes();
    if (allocationOverhead + size >= gc_.GetAllocationThresholdBytes()) {
        PerformFullGC();
    }
    allocatedBytes_ += size;
}

void gc::SingleThreadMarkAndSweep::ThreadData::PerformFullGC() noexcept {
    gc_.PerformFullGC();
}

void gc::SingleThreadMarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    PerformFullGC();
}

void gc::SingleThreadMarkAndSweep::PerformFullGC() noexcept {
    RuntimeAssert(running_ == false, "Cannot have been called during another collection");
    running_ = true;

    KStdVector<ObjHeader*> graySet;
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().Iter()) {
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

    running_ = false;

    // TODO: These will actually need to be run on a separate thread.
    // TODO: This probably should check for the existence of runtime itself, but unit tests initialize only memory.
    RuntimeAssert(mm::ThreadRegistry::Instance().CurrentThreadData() != nullptr, "Finalizers need a Kotlin runtime");
    finalizerQueue.Finalize();
}
