/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"
#include "GCState.hpp"
#include "MainGCThread.hpp"
#include "StwmsGCTraits.hpp"

namespace kotlin::gc {

// Stop-the-world mark & sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler) noexcept
        : gcThread_(state_, allocator, gcScheduler, mark_) {}

    GCStateHolder state_;
    internal::StwmsGCTraits::Mark mark_{};
    internal::MainGCThread<internal::StwmsGCTraits> gcThread_;
};

class GC::ThreadData::Impl {};

namespace barriers {
class ExternalRCRefReleaseGuard::Impl {};
} // namespace barriers

} // namespace kotlin::gc
