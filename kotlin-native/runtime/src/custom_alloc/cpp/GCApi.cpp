/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCApi.hpp"

#include <atomic>
#include <limits>

#include "ConcurrentMarkAndSweep.hpp"
#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "FinalizerHooks.hpp"
#include "GCStatistics.hpp"
#include "KAssert.h"
#include "ObjectFactory.hpp"

namespace {

std::atomic<size_t> allocatedBytesCounter;

}

namespace kotlin::alloc {

bool SweepObject(uint8_t* object, FinalizerQueue& finalizerQueue, gc::GCHandle::GCSweepScope& gcHandle) noexcept {
    HeapObjHeader* objHeader = reinterpret_cast<HeapObjHeader*>(object);
    if (objHeader->gcData.tryResetMark()) {
        CustomAllocDebug("SweepObject(%p): still alive", object);
        gcHandle.addKeptObject();
        return true;
    }
    auto* extraObject = mm::ExtraObjectData::Get(&objHeader->object);
    if (extraObject) {
        if (!extraObject->getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE)) {
            CustomAllocDebug("SweepObject(%p): needs to be finalized, extraObject at %p", object, extraObject);
            extraObject->setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
            CustomAllocDebug("SweepObject: fromExtraObject(%p) = %p", extraObject, ExtraObjectCell::fromExtraObject(extraObject));
            finalizerQueue.Push(ExtraObjectCell::fromExtraObject(extraObject));
            gcHandle.addMarkedObject();
            return true;
        }
        if (!extraObject->getFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE)) {
            CustomAllocDebug("SweepObject(%p): already waiting to be finalized", object);
            gcHandle.addMarkedObject();
            return true;
        }
    }
    CustomAllocDebug("SweepObject(%p): can be reclaimed", object);
    gcHandle.addSweptObject();
    return false;
}

bool SweepExtraObject(mm::ExtraObjectData* extraObject, gc::GCHandle::GCSweepExtraObjectsScope& gcHandle) noexcept {
    if (extraObject->getFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE)) {
        CustomAllocDebug("SweepExtraObject(%p): can be reclaimed", extraObject);
        return false;
    }
    CustomAllocDebug("SweepExtraObject(%p): is still needed", extraObject);
    return true;
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
