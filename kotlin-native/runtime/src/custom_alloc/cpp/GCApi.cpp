/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCApi.hpp"

#include <atomic>
#include <limits>

#include "ConcurrentMarkAndSweep.hpp"
#include "CustomLogging.hpp"
#include "FinalizerHooks.hpp"
#include "KAssert.h"
#include "ObjectFactory.hpp"

namespace {

std::atomic<size_t> allocatedBytesCounter;

}

namespace kotlin::alloc {

bool TryResetMark(void* ptr) noexcept {
    using Node = typename kotlin::mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::Storage::Node;
    using NodeRef = typename kotlin::mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::NodeRef;
    Node& node = Node::FromData(ptr);
    NodeRef ref = NodeRef(node);
    auto& objectData = ref.ObjectData();
    bool reset = objectData.tryResetMark();
    CustomAllocDebug("TryResetMark(%p) = %d", ptr, reset);
    return reset;
}

using ObjectFactory = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>;
using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

static void KeepAlive(ObjHeader* baseObject) noexcept {
    auto& objectData = ObjectFactory::NodeRef::From(baseObject).ObjectData();
    objectData.tryMark();
}

static bool IsAlive(ObjHeader* baseObject) noexcept {
    auto& objectData = ObjectFactory::NodeRef::From(baseObject).ObjectData();
    return objectData.marked();
}

ExtraObjectStatus SweepExtraObject(ExtraObjectCell* extraObjectCell, AtomicStack<ExtraObjectCell>& finalizerQueue) noexcept {
    auto* extraObject = extraObjectCell->Data();
    if (extraObject->getFlag(mm::ExtraObjectData::FLAGS_FINALIZED)) {
        CustomAllocDebug("SweepIsCollectable(%p): already finalized", extraObject);
        return ExtraObjectStatus::SWEPT;
    }
    auto* baseObject = extraObject->GetBaseObject();
    RuntimeAssert(baseObject->heap(), "SweepIsCollectable on a non-heap object");
    if (extraObject->getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE)) {
        CustomAllocDebug("SweepIsCollectable(%p): already in finalizer queue, keep base object (%p) alive", extraObject, baseObject);
        KeepAlive(baseObject);
        return ExtraObjectStatus::TO_BE_FINALIZED;
    }
    if (IsAlive(baseObject)) {
        CustomAllocDebug("SweepIsCollectable(%p): base object (%p) is alive", extraObject, baseObject);
        return ExtraObjectStatus::KEPT;
    }
    extraObject->ClearRegularWeakReferenceImpl();
    if (extraObject->HasAssociatedObject()) {
        extraObject->setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
        finalizerQueue.Push(extraObjectCell);
        KeepAlive(baseObject);
        CustomAllocDebug("SweepIsCollectable(%p): add to finalizerQueue", extraObject);
        return ExtraObjectStatus::TO_BE_FINALIZED;
    } else {
        if (HasFinalizers(baseObject)) {
            extraObject->setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
            finalizerQueue.Push(extraObjectCell);
            KeepAlive(baseObject);
            CustomAllocDebug("SweepIsCollectable(%p): addings to finalizerQueue, keep base object (%p) alive", extraObject, baseObject);
            return ExtraObjectStatus::TO_BE_FINALIZED;
        }
        extraObject->Uninstall();
        CustomAllocDebug("SweepIsCollectable(%p): uninstalled extraObject", extraObject);
        return ExtraObjectStatus::SWEPT;
    }
}

void* SafeAlloc(uint64_t size) noexcept {
    void* memory;
    if (size > std::numeric_limits<size_t>::max() || !(memory = std_support::malloc(size))) {
        konan::consoleErrorf("Out of memory trying to allocate %" PRIu64 "bytes. Aborting.\n", size);
        konan::abort();
    }
    allocatedBytesCounter.fetch_add(static_cast<size_t>(size), std::memory_order_relaxed);
    return memory;
}

void Free(void* ptr, size_t size) noexcept {
    std_support::free(ptr);
    allocatedBytesCounter.fetch_sub(static_cast<size_t>(size), std::memory_order_relaxed);
}

size_t GetAllocatedBytes() noexcept {
    return allocatedBytesCounter.load(std::memory_order_relaxed);
}

} // namespace kotlin::alloc
