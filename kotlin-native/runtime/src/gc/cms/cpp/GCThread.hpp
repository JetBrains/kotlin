/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "AllocatorImpl.hpp"
#include "ConcurrentMark.hpp"
#include "GC.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "SegregatedGCFinalizerProcessor.hpp"
#include "Utils.hpp"
#include "concurrent/UtilityThread.hpp"

namespace kotlin::gc::internal {

class GCThread : private MoveOnly {
public:
    GCThread(
            GCStateHolder& state,
            SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits>& finalizerProcessor,
            mark::ConcurrentMark& markDispatcher,
            alloc::Allocator& allocator,
            gcScheduler::GCScheduler& gcScheduler) noexcept;

private:
    void body() noexcept;
    void PerformFullGC(int64_t epoch) noexcept;

    GCStateHolder& state_;
    SegregatedGCFinalizerProcessor<alloc::FinalizerQueueSingle, alloc::FinalizerQueueTraits>& finalizerProcessor_;
    mark::ConcurrentMark& markDispatcher_;
    alloc::Allocator& allocator_;
    gcScheduler::GCScheduler& gcScheduler_;
    UtilityThread thread_;
};

} // namespace kotlin::gc::internal
