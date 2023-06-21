/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "ConcurrentMarkAndSweep.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "CustomAllocator.hpp"
#else
#include "ExtraObjectDataFactory.hpp"
#endif

namespace kotlin {
namespace gc {

using GCImpl = ConcurrentMarkAndSweep;

class GC::Impl : private Pinned {
public:
#ifdef CUSTOM_ALLOCATOR
    explicit Impl(gcScheduler::GCScheduler& gcScheduler) noexcept : gc_(gcScheduler) {}
#else
    explicit Impl(gcScheduler::GCScheduler& gcScheduler) noexcept : gc_(objectFactory_, extraObjectDataFactory_, gcScheduler) {}
#endif

#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<gc::GCImpl>& objectFactory() noexcept { return objectFactory_; }
    mm::ExtraObjectDataFactory& extraObjectDataFactory() noexcept { return extraObjectDataFactory_; }
#endif
    GCImpl& gc() noexcept { return gc_; }

private:
#ifndef CUSTOM_ALLOCATOR
    mm::ObjectFactory<gc::GCImpl> objectFactory_;
    mm::ExtraObjectDataFactory extraObjectDataFactory_;
#endif
    GCImpl gc_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        gc_(gc.impl_->gc(), threadData),
#ifndef CUSTOM_ALLOCATOR
        objectFactoryThreadQueue_(gc.impl_->objectFactory(), gc_.CreateAllocator()),
        extraObjectDataFactoryThreadQueue_(gc.impl_->extraObjectDataFactory()) {
    }
#else
        alloc_(gc.impl_->gc().heap()) {
    }
#endif

    GCImpl::ThreadData& gc() noexcept { return gc_; }
#ifdef CUSTOM_ALLOCATOR
    alloc::CustomAllocator& alloc() noexcept { return alloc_; }
#else
    mm::ObjectFactory<GCImpl>::ThreadQueue& objectFactoryThreadQueue() noexcept { return objectFactoryThreadQueue_; }
    mm::ExtraObjectDataFactory::ThreadQueue& extraObjectDataFactoryThreadQueue() noexcept { return extraObjectDataFactoryThreadQueue_; }
#endif

private:
    GCImpl::ThreadData gc_;
#ifdef CUSTOM_ALLOCATOR
    alloc::CustomAllocator alloc_;
#else
    mm::ObjectFactory<GCImpl>::ThreadQueue objectFactoryThreadQueue_;
    mm::ExtraObjectDataFactory::ThreadQueue extraObjectDataFactoryThreadQueue_;
#endif
};

} // namespace gc
} // namespace kotlin
