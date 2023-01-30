/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "ConcurrentMarkAndSweep.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "CustomAllocator.hpp"
#endif

namespace kotlin {
namespace gc {

using GCImpl = ConcurrentMarkAndSweep;

class GC::Impl : private Pinned {
public:
#ifdef CUSTOM_ALLOCATOR
    Impl() noexcept : gc_(gcScheduler_) {}
#else
    Impl() noexcept : gc_(objectFactory_, gcScheduler_) {}
#endif

#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<gc::GCImpl>& objectFactory() noexcept { return objectFactory_; }
#endif
    GCScheduler& gcScheduler() noexcept { return gcScheduler_; }
    GCImpl& gc() noexcept { return gc_; }

private:
#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<gc::GCImpl> objectFactory_;
#endif
    GCScheduler gcScheduler_;
    GCImpl gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        gcScheduler_(gc.impl_->gcScheduler().NewThreadData()),
        gc_(gc.impl_->gc(), threadData, gcScheduler_),
#ifndef CUSTOM_ALLOCATOR
        objectFactoryThreadQueue_(gc.impl_->objectFactory(), gc_.CreateAllocator()) {}
#else
        alloc_(gc.impl_->gc().heap(), gcScheduler_) {}
#endif

    GCSchedulerThreadData& gcScheduler() noexcept { return gcScheduler_; }
    GCImpl::ThreadData& gc() noexcept { return gc_; }
#ifdef CUSTOM_ALLOCATOR
    alloc::CustomAllocator& alloc() noexcept { return alloc_; }
#else
    mm::ObjectFactory<GCImpl>::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }
#endif

private:
    GCSchedulerThreadData gcScheduler_;
    GCImpl::ThreadData gc_;
#ifdef CUSTOM_ALLOCATOR
    alloc::CustomAllocator alloc_;
#else
    mm::ObjectFactory<GCImpl>::ThreadQueue objectFactoryThreadQueue_;
#endif
};

} // namespace gc
} // namespace kotlin
