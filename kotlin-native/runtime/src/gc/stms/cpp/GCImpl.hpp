/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "AllocatorImpl.hpp"
#include "GC.hpp"
#include "GCState.hpp"
#include "GCThread.hpp"
#include "SegregatedGCFinalizerProcessor.hpp"

namespace kotlin::gc {

// Stop-the-world mark & sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept :
        finalizerProcessor_(state_), gcThread_(state_, finalizerProcessor_, allocator, gcScheduler) {}

    GCStateHolder state_;
    internal::SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits> finalizerProcessor_;
    internal::GCThread gcThread_;
};

class GC::ThreadData::Impl {};

namespace barriers {
class SpecialRefReleaseGuard::Impl {};
} // namespace barriers

} // namespace kotlin::gc
