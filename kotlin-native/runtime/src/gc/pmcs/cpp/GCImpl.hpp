/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

#include "AllocatorImpl.hpp"
#include "Barriers.hpp"
#include "GC.hpp"
#include "GCState.hpp"
#include "GCThread.hpp"
#include "ParallelMark.hpp"
#include "SegregatedGCFinalizerProcessor.hpp"
#include "ThreadData.hpp"

namespace kotlin {
namespace gc {

// Stop-the-world parallel mark + concurrent sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler, bool mutatorsCooperate, size_t auxGCThreads) noexcept :
        finalizerProcessor_(state_),
        markDispatcher_(mutatorsCooperate),
        mainThread_(state_, finalizerProcessor_, markDispatcher_, allocator, gcScheduler),
        auxThreads_(markDispatcher_, auxGCThreads) {}

    GCStateHolder state_;
    internal::SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> finalizerProcessor_;
    mark::ParallelMark markDispatcher_;
    internal::MainGCThread mainThread_;
    internal::AuxiliaryGCThreads auxThreads_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC::Impl& gc, mm::ThreadData& threadData) noexcept : markDispatcher_(gc.markDispatcher_, threadData) {}

    BarriersThreadData barriers_;
    mark::ParallelMarkThreadData markDispatcher_;
};

namespace barriers {
class SpecialRefReleaseGuard::Impl {};
} // namespace barriers

} // namespace gc
} // namespace kotlin
