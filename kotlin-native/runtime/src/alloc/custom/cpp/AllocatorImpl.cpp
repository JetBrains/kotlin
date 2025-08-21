/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatorImpl.hpp"

#include "Allocator.hpp"
#include "GCApi.hpp"
#include "Heap.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

alloc::Allocator::ThreadData::ThreadData(Allocator& allocator) noexcept : impl_(std::make_unique<Impl>(allocator.impl())) {}

alloc::Allocator::ThreadData::~ThreadData() = default;

PERFORMANCE_INLINE ObjHeader* alloc::Allocator::ThreadData::allocateObject(const TypeInfo* typeInfo) noexcept {
    return impl_->alloc().CreateObject(typeInfo);
}

PERFORMANCE_INLINE ArrayHeader* alloc::Allocator::ThreadData::allocateArray(const TypeInfo* typeInfo, uint32_t elements) noexcept {
    return impl_->alloc().CreateArray(typeInfo, elements);
}

PERFORMANCE_INLINE mm::ExtraObjectData& alloc::Allocator::ThreadData::allocateExtraObjectData(
        ObjHeader* object, const TypeInfo* typeInfo) noexcept {
    return *impl_->alloc().CreateExtraObjectDataForObject(object, typeInfo);
}

ALWAYS_INLINE void alloc::Allocator::ThreadData::destroyUnattachedExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    extraObject.setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
}

void alloc::Allocator::ThreadData::prepareForGC() noexcept {
    impl_->alloc().PrepareForGC();
}

void alloc::Allocator::ThreadData::clearForTests() noexcept {
    impl_->alloc().PrepareForGC();
}

alloc::Allocator::Allocator() noexcept : impl_(std::make_unique<Impl>()) {}

alloc::Allocator::~Allocator() = default;

void alloc::Allocator::prepareForGC() noexcept {
    impl_->heap().PrepareForGC();
}

void alloc::Allocator::clearForTests() noexcept {
    stopFinalizerThreadIfRunning();
    impl_->heap().ClearForTests();
    impl_->pendingFinalizers() = FinalizerQueue();
}

void alloc::Allocator::TraverseAllocatedObjects(std::function<void(ObjHeader*)> fn) noexcept {
    impl_->heap().TraverseAllocatedObjects(fn);
}

void alloc::Allocator::TraverseAllocatedExtraObjects(std::function<void(mm::ExtraObjectData*)> fn) noexcept {
    impl_->heap().TraverseAllocatedExtraObjects(fn);
}

void alloc::Allocator::startFinalizerThreadIfNeeded() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    impl_->finalizerProcessor().startThreadIfNeeded();
}

void alloc::Allocator::stopFinalizerThreadIfRunning() noexcept {
    NativeOrUnregisteredThreadGuard guard(true);
    impl_->finalizerProcessor().stopThread();
}

bool alloc::Allocator::finalizersThreadIsRunning() noexcept {
    return impl_->finalizerProcessor().isThreadRunning();
}

void alloc::Allocator::configureMainThreadFinalizerProcessor(std::function<void(alloc::RunLoopFinalizerProcessorConfig&)> f) noexcept {
    impl_->finalizerProcessor().configureMainThread(std::move(f));
}

bool alloc::Allocator::mainThreadFinalizerProcessorAvailable() noexcept {
    return impl_->finalizerProcessor().mainThreadAvailable();
}

void alloc::Allocator::sweep(gc::GCHandle gcHandle) noexcept {
    // also sweeps extraObjects
    auto finalizerQueue = impl_->heap().Sweep(gcHandle);
    for (auto& thread : kotlin::mm::ThreadRegistry::Instance().LockForIter()) {
        finalizerQueue.mergeFrom(thread.allocator().impl().alloc().ExtractFinalizerQueue());
    }
    finalizerQueue.mergeFrom(impl_->heap().ExtractFinalizerQueue());
    RuntimeAssert(impl_->pendingFinalizers().size() == 0, "pendingFinalizers_ were not empty");
    impl_->pendingFinalizers() = std::move(finalizerQueue);
}

void alloc::Allocator::scheduleFinalization(gc::GCHandle gcHandle) noexcept {
    auto queue = std::move(impl_->pendingFinalizers());
    gcHandle.finalizersScheduled(queue.size());
    impl_->finalizerProcessor().schedule(std::move(queue), gcHandle.getEpoch());
}

void alloc::initObjectPool() noexcept {}

void alloc::compactObjectPoolInCurrentThread() noexcept {}

gc::GC::ObjectData& alloc::objectDataForObject(ObjHeader* object) noexcept {
    return CustomHeapObject::from(object).heapHeader();
}

ObjHeader* alloc::objectForObjectData(gc::GC::ObjectData& objectData) noexcept {
    return CustomHeapObject::from(objectData).object();
}

size_t alloc::allocatedHeapSize(ObjHeader* object) noexcept {
    return CustomAllocator::GetAllocatedHeapSize(object);
}

size_t alloc::allocatedBytes() noexcept {
    return GetAllocatedBytes();
}

void alloc::destroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    extraObject.ReleaseAssociatedObject();
    if (extraObject.GetBaseObject()) {
        // If there's an object attached to this extra object, the next
        // GC sweep will have to resolve this cycle.
        extraObject.setFlag(mm::ExtraObjectData::FLAGS_FINALIZED);
    } else {
        // If there's no object attached to this extra object, the next
        // GC sweep will just collect this extra object.
        extraObject.setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
    }
}
