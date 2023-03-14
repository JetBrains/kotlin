/*
* Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#pragma once

#include "ConcurrentMarkAndSweep.hpp"
#include "GCState.hpp"
#include "ObjectFactory.hpp"
#include "ScopedThread.hpp"

namespace kotlin::gc {

class FinalizerProcessor : Pinned {
public:
    using Queue = typename kotlin::mm::ObjectFactory<ConcurrentMarkAndSweep>::FinalizerQueue;
    // epochDoneCallback could be called on any subset of them.
    // If no new tasks are set, epochDoneCallback will be eventually called on last epoch
    explicit FinalizerProcessor(std::function<void(int64_t)> epochDoneCallback): epochDoneCallback_(std::move(epochDoneCallback)) {}
    void ScheduleTasks(Queue&& tasks, int64_t epoch) noexcept;
    void StopFinalizerThread() noexcept;
    bool IsRunning() noexcept;
    void StartFinalizerThreadIfNone() noexcept;
    void WaitFinalizerThreadInitialized() noexcept;
    ~FinalizerProcessor();

private:
    ScopedThread finalizerThread_;
    Queue finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t finalizerQueueEpoch_ = 0;
    bool shutdownFlag_ = false;
    bool newTasksAllowed_ = true;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;
};

} // namespace kotlin::gc
