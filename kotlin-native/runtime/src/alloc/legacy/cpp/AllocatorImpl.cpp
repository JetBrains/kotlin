/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatorImpl.hpp"

#include "Allocator.hpp"
#include "ExtraObjectDataFactory.hpp"
#include "GC.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

namespace {

// TODO move to common
[[maybe_unused]] inline void checkMarkCorrectness(alloc::ObjectFactoryImpl::Iterable& heap) {
    if (compiler::runtimeAssertsMode() == compiler::RuntimeAssertsMode::kIgnore) return;
    for (auto objRef : heap) {
        auto obj = objRef.GetObjHeader();
        if (gc::isMarked(obj)) {
            traverseReferredObjects(obj, [obj](ObjHeader* field) {
                if (field->heap()) {
                    RuntimeAssert(gc::isMarked(field), "Field %p of an alive obj %p must be alive", field, obj);
                }
            });
        }
    }
}

}

alloc::Allocator::ThreadData::ThreadData(Allocator& allocator) noexcept : impl_(std::make_unique<Impl>(allocator.impl())) {}

alloc::Allocator::ThreadData::~ThreadData() = default;

PERFORMANCE_INLINE ObjHeader* alloc::Allocator::ThreadData::allocateObject(const TypeInfo* typeInfo) noexcept {
    return impl_->objectFactoryThreadQueue().CreateObject(typeInfo);
}

PERFORMANCE_INLINE ArrayHeader* alloc::Allocator::ThreadData::allocateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
    return impl_->objectFactoryThreadQueue().CreateArray(typeInfo, elements);
}

PERFORMANCE_INLINE mm::ExtraObjectData& alloc::Allocator::ThreadData::allocateExtraObjectData(
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

void alloc::Allocator::TraverseAllocatedObjects(std::function<void(ObjHeader*)> fn) noexcept {
    for (auto node : impl_->objectFactory().LockForIter()) {
        fn(node.GetObjHeader());
    }
}

void alloc::Allocator::TraverseAllocatedExtraObjects(std::function<void(mm::ExtraObjectData*)> fn) noexcept {
    for (auto& extraObjectData : impl_->extraObjectDataFactory().LockForIter()) {
        fn(&extraObjectData);
    }
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

alloc::SweepState::SweepState(alloc::ObjectFactoryImpl& objectFactory, alloc::ExtraObjectDataFactory& extraObjectDataFactory) noexcept :
    extraObjectFactoryIterable_(extraObjectDataFactory.LockForIter()), objectFactoryIterable_(objectFactory.LockForIter()) {}

alloc::SweepState alloc::Allocator::Impl::prepareForSweep() noexcept {
    alloc::SweepState result{objectFactory_, extraObjectDataFactory_};
    checkMarkCorrectness(*result.objectFactoryIterable_);
    return result;
}

alloc::FinalizerQueue alloc::Allocator::Impl::sweep(gc::GCHandle gcHandle, alloc::SweepState state) noexcept {
    alloc::SweepExtraObjects<alloc::DefaultSweepTraits<alloc::ObjectFactoryImpl>>(gcHandle, *state.extraObjectFactoryIterable_);
    state.extraObjectFactoryIterable_ = std::nullopt;
    auto finalizerQueue = alloc::Sweep<alloc::DefaultSweepTraits<alloc::ObjectFactoryImpl>>(gcHandle, *state.objectFactoryIterable_);
    state.objectFactoryIterable_ = std::nullopt;
    alloc::compactObjectPoolInMainThread();
    return finalizerQueue;
}
