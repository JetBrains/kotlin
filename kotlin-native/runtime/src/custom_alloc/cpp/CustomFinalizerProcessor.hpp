/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
#define CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_

#include <cstdint>
#include <condition_variable>

#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"
#include "ScopedThread.hpp"

namespace kotlin::alloc {

class CustomFinalizerProcessor : Pinned {
public:
    using Queue = typename kotlin::alloc::AtomicStack<kotlin::alloc::ExtraObjectCell>;
    explicit CustomFinalizerProcessor(std::function<void(int64_t)> epochDoneCallback) : epochDoneCallback_(std::move(epochDoneCallback)) {}
    void ScheduleTasks(Queue&& tasks, int64_t epoch) noexcept;
    void StopFinalizerThread() noexcept;
    bool IsRunning() noexcept;
    void StartFinalizerThreadIfNone() noexcept;
    void WaitFinalizerThreadInitialized() noexcept;
    ~CustomFinalizerProcessor();

private:
    ScopedThread finalizerThread_;
    Queue finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t scheduledEpoch_ = 0;
    int64_t finalizedEpoch_ = 0;
    bool shutdownFlag_ = false;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;
};

} // namespace kotlin::alloc

#endif // CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
