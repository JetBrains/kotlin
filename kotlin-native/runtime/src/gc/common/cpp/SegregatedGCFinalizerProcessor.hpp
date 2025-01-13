/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GCState.hpp"
#include "GCStatistics.hpp"
#include "FinalizerProcessor.hpp"
#include "MainThreadFinalizerProcessor.hpp"
#include "SegregatedFinalizerQueue.hpp"

namespace kotlin::gc::internal {

template <typename FinalizerQueueSingle, typename FinalizerQueueTraits>
class SegregatedGCFinalizerProcessor : private Pinned {
public:
    explicit SegregatedGCFinalizerProcessor(GCStateHolder& state) noexcept :
        finalizerProcessor_([&state](int64_t epoch) noexcept {
            GCHandle::getByEpoch(epoch).finalizersDone();
            state.finalized(epoch);
        }) {}

    void schedule(alloc::SegregatedFinalizerQueue<FinalizerQueueSingle> queue, int64_t epoch) noexcept {
        if (!mainThreadFinalizerProcessor_.available()) {
            queue.mergeIntoRegular();
        }
        finalizerProcessor_.ScheduleTasks(std::move(queue.regular), epoch);
        mainThreadFinalizerProcessor_.schedule(std::move(queue.mainThread), epoch);
    }

    void startThreadIfNeeded() noexcept {
        finalizerProcessor_.StartFinalizerThreadIfNone();
        finalizerProcessor_.WaitFinalizerThreadInitialized();
    }

    void stopThread() noexcept { finalizerProcessor_.StopFinalizerThread(); }

    bool isThreadRunning() noexcept { return finalizerProcessor_.IsRunning(); }

    void configureMainThread(std::function<void(alloc::RunLoopFinalizerProcessorConfig&)> f) noexcept {
        mainThreadFinalizerProcessor_.withConfig(std::move(f));
    }

    bool mainThreadAvailable() noexcept { return mainThreadFinalizerProcessor_.available(); }

private:
    FinalizerProcessor<FinalizerQueueSingle, FinalizerQueueTraits> finalizerProcessor_;
    alloc::MainThreadFinalizerProcessor<FinalizerQueueSingle, FinalizerQueueTraits> mainThreadFinalizerProcessor_;
};

} // namespace kotlin::gc::internal
