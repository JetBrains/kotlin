/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "AllocatorImpl.hpp"
#include "Barriers.hpp"
#include "ConcurrentMark.hpp"
#include "GC.hpp"
#include "GCState.hpp"
#include "GCThread.hpp"
#include "SegregatedGCFinalizerProcessor.hpp"

namespace kotlin {
namespace gc {

// Concurrent mark & sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler, bool mutatorsCooperate, size_t auxGCThreads) noexcept :
        finalizerProcessor_(state_), gcThread_(state_, finalizerProcessor_, markDispatcher_, allocator, gcScheduler) {
        RuntimeAssert(!mutatorsCooperate, "Cooperative mutators aren't supported yet");
        RuntimeAssert(auxGCThreads == 0, "Auxiliary GC threads aren't supported yet");
    }

    GCStateHolder state_;
    internal::SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> finalizerProcessor_;
    mark::ConcurrentMark markDispatcher_;
    internal::GCThread gcThread_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(mark::ConcurrentMark& mark, mm::ThreadData& threadData) noexcept : mark_(mark, threadData) {}

    barriers::BarriersThreadData barriers_;
    mark::ConcurrentMark::ThreadData mark_;
};

} // namespace gc
} // namespace kotlin
