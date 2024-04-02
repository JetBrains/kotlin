/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatorImpl.hpp"

#include "ThreadData.hpp"

using namespace kotlin;

alloc::Allocator::ThreadData::ThreadData(Allocator& allocator) noexcept : impl_(std::make_unique<Impl>(allocator.impl())) {}

alloc::Allocator::ThreadData::~ThreadData() = default;

ALWAYS_INLINE ObjHeader* alloc::Allocator::ThreadData::allocateObject(const TypeInfo* typeInfo) noexcept {
    return impl_->objectFactoryThreadQueue().CreateObject(typeInfo);
}

ALWAYS_INLINE ArrayHeader* alloc::Allocator::ThreadData::allocateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
    return impl_->objectFactoryThreadQueue().CreateArray(typeInfo, elements);
}

ALWAYS_INLINE mm::ExtraObjectData& alloc::Allocator::ThreadData::allocateExtraObjectData(
        ObjHeader* object, const TypeInfo* typeInfo) noexcept {
    return impl_->extraObjectDataFactoryThreadQueue().CreateExtraObjectDataForObject(object, typeInfo);
}

ALWAYS_INLINE void alloc::Allocator::ThreadData::destroyUnattachedExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    impl_->extraObjectDataFactoryThreadQueue().DestroyExtraObjectData(extraObject);
}

void alloc::Allocator::ThreadData::prepareForGC() noexcept {
    impl_->extraObjectDataFactoryThreadQueue().Publish();
    impl_->objectFactoryThreadQueue().Publish();
}

void alloc::Allocator::ThreadData::clearForTests() noexcept {
    impl_->extraObjectDataFactoryThreadQueue().ClearForTests();
    impl_->objectFactoryThreadQueue().ClearForTests();
}

alloc::Allocator::Allocator() noexcept : impl_(std::make_unique<Impl>()) {}

alloc::Allocator::~Allocator() = default;

void alloc::Allocator::prepareForGC() noexcept {}

void alloc::Allocator::clearForTests() noexcept {
    impl_->extraObjectDataFactory().ClearForTests();
    impl_->objectFactory().ClearForTests();
}

gc::GC::ObjectData& alloc::objectDataForObject(ObjHeader* object) noexcept {
    return ObjectFactoryImpl::NodeRef::From(object).ObjectData();
}

ObjHeader* alloc::objectForObjectData(gc::GC::ObjectData& objectData) noexcept {
    return ObjectFactoryImpl::NodeRef::From(objectData)->GetObjHeader();
}

size_t alloc::allocatedHeapSize(ObjHeader* object) noexcept {
    return ObjectFactoryImpl::GetAllocatedHeapSize(object);
}

void alloc::destroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    extraObject.Uninstall();
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->allocator().impl().extraObjectDataFactoryThreadQueue().DestroyExtraObjectData(extraObject);
}
