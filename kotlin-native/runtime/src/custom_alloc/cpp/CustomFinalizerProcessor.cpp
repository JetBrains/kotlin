/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CustomFinalizerProcessor.hpp"

#include <cstdint>
#include <mutex>
#include <thread>

#include "AtomicStack.hpp"
#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "FinalizerHooks.hpp"
#include "Memory.h"
#include "Runtime.h"

namespace kotlin::alloc {

void CustomFinalizerProcessor::StartFinalizerThreadIfNone() noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::StartFinalizerThreadIfNone()");
    std::unique_lock guard(threadCreatingMutex_);
    if (finalizerThread_.joinable()) return;

    finalizerThread_ = ScopedThread(ScopedThread::attributes().name("Custom finalizer processor"), [this] {
        Kotlin_initRuntimeIfNeeded();
        {
            std::unique_lock guard(initializedMutex_);
            initialized_ = true;
        }
        initializedCondVar_.notify_all();
        while (true) {
            std::unique_lock lock(finalizerQueueMutex_);
            finalizerQueueCondVar_.wait(lock, [this] {
                return finalizedEpoch_ != scheduledEpoch_ || shutdownFlag_;
            });
            if (finalizedEpoch_ == scheduledEpoch_) {
                RuntimeAssert(shutdownFlag_, "Nothing to do, but no shutdownFlag_ is set on wakeup");
                RuntimeAssert(finalizerQueue_.isEmpty(), "Finalizer queue should be empty when killing finalizer thread");
                break;
            }
            int64_t queueEpoch = scheduledEpoch_;
            Queue finalizerQueue = std::move(finalizerQueue_);
            lock.unlock();
            ThreadStateGuard guard(ThreadState::kRunnable);
            ExtraObjectCell* cell;
            while ((cell = finalizerQueue.Pop())) {
                auto* extraObject = cell->Data();
                auto* baseObject = extraObject->GetBaseObject();
                RunFinalizers(baseObject);
                extraObject->setFlag(mm::ExtraObjectData::FLAGS_FINALIZED);
            }
            while (finalizedEpoch_ != queueEpoch) {
                epochDoneCallback_(++finalizedEpoch_);
            }
        }
        {
            std::unique_lock guard(initializedMutex_);
            initialized_ = false;
        }
        initializedCondVar_.notify_all();
    });
}

void CustomFinalizerProcessor::StopFinalizerThread() noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::StopFinalizerThread()");
    {
        std::unique_lock guard(finalizerQueueMutex_);
        if (!finalizerThread_.joinable()) return;
        shutdownFlag_ = true;
        finalizerQueueCondVar_.notify_all();
    }
    finalizerThread_.join();
    shutdownFlag_ = false;
    RuntimeAssert(finalizerQueue_.isEmpty(), "Finalizer queue should be empty when killing finalizer thread");
    std::unique_lock guard(finalizerQueueMutex_);
    finalizerQueueCondVar_.notify_all();
}

void CustomFinalizerProcessor::ScheduleTasks(Queue&& tasks, int64_t epoch) noexcept {
    std::unique_lock guard(finalizerQueueMutex_);
    StartFinalizerThreadIfNone();
    finalizerQueue_.TransferAllFrom(std::move(tasks));
    scheduledEpoch_ = epoch;
    finalizerQueueCondVar_.notify_all();
}

bool CustomFinalizerProcessor::IsRunning() noexcept {
    return finalizerThread_.joinable();
}

void CustomFinalizerProcessor::WaitFinalizerThreadInitialized() noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::WaitFinalizerThreadInitialized()");
    std::unique_lock guard(initializedMutex_);
    initializedCondVar_.wait(guard, [this] { return initialized_; });
}

CustomFinalizerProcessor::~CustomFinalizerProcessor() {
    StopFinalizerThread();
}

} // namespace kotlin::alloc
