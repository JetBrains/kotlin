/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectOps.hpp"

#include "Common.h"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

#if __has_feature(thread_sanitizer)
#include <sanitizer/tsan_interface.h>
#endif

using namespace kotlin;

OBJ_GETTER(mm::AllocateObject, ThreadData* threadData, const TypeInfo* typeInfo) noexcept {
    AssertThreadState(threadData, ThreadState::kRunnable);
    // TODO: Make this work with GCs that can stop thread at any point.
    auto* object = threadData->allocator().allocateObject(typeInfo);
    threadData->gc().onAllocation(object);
    // Prevents unsafe class publication (see KT-58995).
    // Also important in case of the concurrent GC mark phase.
    std::atomic_thread_fence(std::memory_order_release);
#if __has_feature(thread_sanitizer)
    // TSAN doesn't support fences.
    __tsan_release(object);
#endif
    RETURN_OBJ(object);
}

OBJ_GETTER(mm::AllocateArray, ThreadData* threadData, const TypeInfo* typeInfo, uint32_t elements) noexcept {
    AssertThreadState(threadData, ThreadState::kRunnable);
    // TODO: Make this work with GCs that can stop thread at any point.
    auto* array = threadData->allocator().allocateArray(typeInfo, static_cast<uint32_t>(elements));
    threadData->gc().onAllocation(array->obj());
    // Prevents unsafe class publication (see KT-58995).
    // Also important in case of the concurrent GC mark phase.
    std::atomic_thread_fence(std::memory_order_release);
#if __has_feature(thread_sanitizer)
    // TSAN doesn't support fences.
    __tsan_release(array);
#endif
    RETURN_OBJ(array->obj());
}
