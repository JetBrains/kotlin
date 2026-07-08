/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Barriers.hpp"
#include "CollectionScope.hpp"
#include "ConcurrentMark.hpp"
#include "GmsCollectionPolicy.hpp"
#include "GmsGCTraits.hpp"
#include "GC.hpp"
#include "GCState.hpp"
#include "MainGCThread.hpp"

namespace kotlin {
namespace gc {

// Generational mark & sweep: CMS's concurrent mark & sweep machinery plus a logical (non-moving)
// generational layer. Objects that survive a collection are "old" (sticky, packed into
// GC::ObjectData's mark word); a minor "Eden" collection traces only young objects plus old->young edges
// recorded by the write barrier (see RememberedSet / Barriers), while a major "Full" collection
// re-traces and sweeps the whole heap. See CollectionScope and the milestone plan.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler, bool mutatorsCooperate, size_t auxGCThreads) noexcept :
        gcThread_(state_, allocator, gcScheduler, mark_) {
        RuntimeAssert(!mutatorsCooperate, "Cooperative mutators aren't supported yet");
        RuntimeAssert(auxGCThreads == 0, "Auxiliary GC threads aren't supported yet");
    }

    // Requested scope for the next collection, from the heap-growth policy. A pending Full request
    // (remembered-set overflow / explicit collect) can still upgrade an Eden decision at STW; see
    // GmsGCTraits::onCollectionStart.
    gc::CollectionScope chooseCollectionScope() noexcept { return gms::choosePolicyScope(); }

    GCStateHolder state_;
    mark::ConcurrentMark mark_{};
    internal::MainGCThread<internal::GmsGCTraits> gcThread_;
};

class GC::ThreadData::Impl : private Pinned {
public:
    Impl(mark::ConcurrentMark& mark, mm::ThreadData& threadData) noexcept : mark_(mark, threadData) {}

    barriers::BarriersThreadData barriers_;
    mark::ConcurrentMark::ThreadData mark_;
};

} // namespace gc
} // namespace kotlin
