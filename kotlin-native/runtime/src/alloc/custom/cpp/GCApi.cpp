/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCApi.hpp"

#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <limits>

#ifndef KONAN_WINDOWS
#include <sys/mman.h>
#endif

#include "CompilerConstants.hpp"
#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "FinalizerHooks.hpp"
#include "GC.hpp"
#include "GCStatistics.hpp"
#include "KAssert.h"
#include "Memory.h"

namespace {

std::atomic<size_t> allocatedBytesCounter;

}

namespace kotlin::alloc {

bool SweepObject(uint8_t* object, FinalizerQueue& finalizerQueue, gc::GCHandle::GCSweepScope& gcHandle) noexcept {
    auto* heapObjHeader = reinterpret_cast<HeapObjHeader*>(object);
    if (gc::tryResetMark(heapObjHeader->objectData())) {
        CustomAllocDebug("SweepObject(%p): still alive", heapObjHeader);
        gcHandle.addKeptObject();
        return true;
    }
    auto* extraObject = mm::ExtraObjectData::Get(heapObjHeader->object());
    if (extraObject) {
        if (!extraObject->getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE)) {
            CustomAllocDebug("SweepObject(%p): needs to be finalized, extraObject at %p", heapObjHeader, extraObject);
            extraObject->setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
            extraObject->ClearRegularWeakReferenceImpl();
            CustomAllocDebug("SweepObject: fromExtraObject(%p) = %p", extraObject, ExtraObjectCell::fromExtraObject(extraObject));
            finalizerQueue.Push(ExtraObjectCell::fromExtraObject(extraObject));
            gcHandle.addMarkedObject();
            gcHandle.addKeptObject();
            return true;
        }
        if (!extraObject->getFlag(mm::ExtraObjectData::FLAGS_FINALIZED)) {
            CustomAllocDebug("SweepObject(%p): already waiting to be finalized", heapObjHeader);
            gcHandle.addMarkedObject();
            gcHandle.addKeptObject();
            return true;
        }
        extraObject->UnlinkFromBaseObject();
        extraObject->setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
    }
    CustomAllocDebug("SweepObject(%p): can be reclaimed", heapObjHeader);
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
    if (size > std::numeric_limits<size_t>::max()) {
        konan::consoleErrorf("Out of memory trying to allocate %" PRIu64 "bytes. Aborting.\n", size);
        std::abort();
    }
    void* memory;
    bool error;
    if (compiler::disableMmap()) {
        memory = calloc(size, 1);
        error = memory == nullptr;
    } else {
#if KONAN_WINDOWS
        RuntimeFail("mmap is not available on mingw");
#elif KONAN_LINUX
        memory = mmap(nullptr, size, PROT_WRITE | PROT_READ, MAP_ANONYMOUS | MAP_PRIVATE | MAP_NORESERVE | MAP_POPULATE, -1, 0);
        error = memory == MAP_FAILED;
#else
        memory = mmap(nullptr, size, PROT_WRITE | PROT_READ, MAP_ANONYMOUS | MAP_PRIVATE | MAP_NORESERVE, -1, 0);
        error = memory == MAP_FAILED;
#endif
    }
    if (error) {
        konan::consoleErrorf("Out of memory trying to allocate %" PRIu64 "bytes: %s. Aborting.\n", size, strerror(errno));
        std::abort();
    }
    auto previousSize = allocatedBytesCounter.fetch_add(static_cast<size_t>(size), std::memory_order_relaxed);
    OnMemoryAllocation(previousSize + static_cast<size_t>(size));
    CustomAllocDebug("SafeAlloc(%zu) = %p", static_cast<size_t>(size), memory);
    return memory;
}

void Free(void* ptr, size_t size) noexcept {
    CustomAllocDebug("Free(%p, %zu)", ptr, size);
    if (compiler::disableMmap()) {
        free(ptr);
    } else {
#if KONAN_WINDOWS
        RuntimeFail("mmap is not available on mingw");
#else
        auto result = munmap(ptr, size);
        RuntimeAssert(result == 0, "Failed to munmap: %s", strerror(errno));
#endif
    }
    allocatedBytesCounter.fetch_sub(static_cast<size_t>(size), std::memory_order_relaxed);
}

size_t GetAllocatedBytes() noexcept {
    return allocatedBytesCounter.load(std::memory_order_relaxed);
}

} // namespace kotlin::alloc
