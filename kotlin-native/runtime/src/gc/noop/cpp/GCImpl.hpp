/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

#include "NoOpGC.hpp"
#include "ObjectFactory.hpp"

#ifdef CUSTOM_ALLOCATOR
#include "CustomAllocator.hpp"
#else
#include "ExtraObjectDataFactory.hpp"
#endif

namespace kotlin {
namespace gc {

using GCImpl = NoOpGC;

class GC::Impl : private Pinned {
public:
    Impl() noexcept = default;

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
#ifdef CUSTOM_ALLOCATOR
    Impl(GC& gc, mm::ThreadData& threadData) noexcept : alloc_(gc.impl_->gc().heap()) {}
#else
    Impl(GC& gc, mm::ThreadData& threadData) noexcept :
        objectFactoryThreadQueue_(gc.impl_->objectFactory(), gc_.CreateAllocator()),
        extraObjectDataFactoryThreadQueue_(gc.impl_->extraObjectDataFactory()) {}
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
