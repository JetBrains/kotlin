/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Allocator.hpp"

#include "ExtraObjectDataFactory.hpp"
#include "GC.hpp"
#include "GCScheduler.hpp"
#include "GCStatistics.hpp"
#include "GlobalData.hpp"
#include "ObjectFactory.hpp"
#include "ObjectFactoryAllocator.hpp"
#include "ObjectFactorySweep.hpp"
#include "SegregatedFinalizerProcessor.hpp"
#include "Logging.hpp"

namespace kotlin::alloc {

struct ObjectFactoryTraits {
    using Allocator = alloc::AllocatorWithGC<alloc::AllocatorBasic, ObjectFactoryTraits>;
    using ObjectData = gc::GC::ObjectData;

    Allocator CreateAllocator() noexcept { return Allocator(alloc::AllocatorBasic(), *this); }

    void OnOOM(size_t size) noexcept {
        RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
        // TODO: This will print the log for "manual" scheduling. Fix this.
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinished();
    }
};

using ObjectFactoryImpl = ObjectFactory<ObjectFactoryTraits>;

using FinalizerQueueSingle = ObjectFactoryImpl::FinalizerQueue;
using FinalizerQueue = SegregatedFinalizerQueue<FinalizerQueueSingle>;
using FinalizerQueueTraits = ObjectFactoryImpl::FinalizerQueueTraits;

struct SweepState : private MoveOnly {
    SweepState(ObjectFactoryImpl& objectFactory, ExtraObjectDataFactory& extraObjectDataFactory) noexcept;

    std::optional<ExtraObjectDataFactory::Iterable> extraObjectFactoryIterable_;
    std::optional<ObjectFactoryImpl::Iterable> objectFactoryIterable_;
};

class Allocator::Impl : private Pinned {
public:
    Impl() noexcept : finalizerProcessor_([](int64_t epoch) noexcept { mm::GlobalData::Instance().gc().onEpochFinalized(epoch); }) {}

    ObjectFactoryImpl& objectFactory() noexcept { return objectFactory_; }
    ExtraObjectDataFactory& extraObjectDataFactory() noexcept { return extraObjectDataFactory_; }
    SegregatedFinalizerProcessor<FinalizerQueueSingle, FinalizerQueueTraits>& finalizerProcessor() noexcept { return finalizerProcessor_; }

    SweepState prepareForSweep() noexcept;
    FinalizerQueue sweep(gc::GCHandle gcHandle, SweepState state) noexcept;
    void scheduleFinalization(FinalizerQueue queue, int64_t epoch) noexcept;

private:
    ObjectFactoryImpl objectFactory_;
    ExtraObjectDataFactory extraObjectDataFactory_;
    SegregatedFinalizerProcessor<FinalizerQueueSingle, FinalizerQueueTraits> finalizerProcessor_;
};

class Allocator::ThreadData::Impl : private Pinned {
public:
    explicit Impl(Allocator::Impl& allocator) noexcept :
        objectFactoryThreadQueue_(allocator.objectFactory(), objectFactoryTraits_.CreateAllocator()),
        extraObjectDataFactoryThreadQueue_(allocator.extraObjectDataFactory()) {}

    ObjectFactoryImpl::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }
    ExtraObjectDataFactory::ThreadQueue& extraObjectDataFactoryThreadQueue() noexcept { return extraObjectDataFactoryThreadQueue_; }

private:
    [[no_unique_address]] ObjectFactoryTraits objectFactoryTraits_;
    ObjectFactoryImpl::ThreadQueue objectFactoryThreadQueue_;
    ExtraObjectDataFactory::ThreadQueue extraObjectDataFactoryThreadQueue_;
};

} // namespace kotlin::alloc
