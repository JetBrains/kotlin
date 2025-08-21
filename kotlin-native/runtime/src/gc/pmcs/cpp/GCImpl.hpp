/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

#include "AuxiliaryGCThreads.hpp"
#include "Barriers.hpp"
#include "GC.hpp"
#include "GCState.hpp"
#include "MainGCThread.hpp"
#include "ParallelMark.hpp"
#include "PmcsGCTraits.hpp"
#include "ThreadData.hpp"

namespace kotlin::gc {

// Stop-the-world parallel mark + concurrent sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler, bool mutatorsCooperate, size_t auxGCThreads) noexcept :
        mark_(mutatorsCooperate),
        mainThread_(state_, allocator, gcScheduler, mark_),
        auxThreads_(mark_, auxGCThreads) {}

    GCStateHolder state_;
    internal::PmcsGCTraits::Mark mark_;
    internal::MainGCThread<internal::PmcsGCTraits> mainThread_;
    internal::AuxiliaryGCThreads auxThreads_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(GC::Impl& gc, mm::ThreadData& threadData) noexcept : markDispatcher_(gc.mark_, threadData) {}

    BarriersThreadData barriers_;
    mark::ParallelMarkThreadData markDispatcher_;
};

namespace barriers {
class ExternalRCRefReleaseGuard::Impl {};
} // namespace barriers

} // namespace kotlin::gc
