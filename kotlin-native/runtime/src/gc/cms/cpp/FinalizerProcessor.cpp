/*
* Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#include "FinalizerProcessor.hpp"
#include "ObjectFactory.hpp"
#include "Runtime.h"


void kotlin::gc::FinalizerProcessor::StartFinalizerThreadIfNone() noexcept {
    if (finalizerThread_.joinable()) return;
    finalizerThread_ = ScopedThread(ScopedThread::attributes().name("GC finalizer processor"), [this] {
        Kotlin_initRuntimeIfNeeded();
        {
            std::unique_lock guard(initializedMutex_);
            initialized_ = true;
        }
        initializedCondVar_.notify_all();
        int64_t finalizersEpoch = 0;
        while (true) {
            std::unique_lock lock(finalizerQueueMutex_);
            finalizerQueueCondVar_.wait(lock, [this, &finalizersEpoch] {
                return finalizerQueue_.size() > 0 || finalizerQueueEpoch_ != finalizersEpoch || shutdownFlag_;
            });
            if (finalizerQueue_.size() == 0 && finalizerQueueEpoch_ == finalizersEpoch) {
                newTasksAllowed_ = false;
                RuntimeAssert(shutdownFlag_, "Nothing to do, but no shutdownFlag_ is set on wakeup");
                break;
            }
            auto queue = std::move(finalizerQueue_);
            finalizersEpoch = finalizerQueueEpoch_;
            lock.unlock();
            if (queue.size() > 0) {
                ThreadStateGuard guard(ThreadState::kRunnable);
                queue.Finalize();
            }
            epochDoneCallback_(finalizersEpoch);
        }
        {
            std::unique_lock guard(initializedMutex_);
            initialized_ = false;
        }
        initializedCondVar_.notify_all();
    });
}

void kotlin::gc::FinalizerProcessor::StopFinalizerThread() noexcept {
    {
        std::unique_lock guard(finalizerQueueMutex_);
        if (!finalizerThread_.joinable()) return;
        shutdownFlag_ = true;
        finalizerQueueCondVar_.notify_all();
    }
    finalizerThread_.join();
    shutdownFlag_ = false;
    RuntimeAssert(finalizerQueue_.size() == 0, "Finalizer queue should be empty when killing finalizer thread");
    std::unique_lock guard(finalizerQueueMutex_);
    newTasksAllowed_ = true;
    finalizerQueueCondVar_.notify_all();
}

void kotlin::gc::FinalizerProcessor::ScheduleTasks(Queue&& tasks, int64_t epoch) noexcept {
    std::unique_lock guard(finalizerQueueMutex_);
    if (tasks.size() == 0 && !IsRunning()) {
        epochDoneCallback_(epoch);
        return;
    }
    finalizerQueueCondVar_.wait(guard, [this] { return newTasksAllowed_; });
    StartFinalizerThreadIfNone();
    finalizerQueue_.MergeWith(std::move(tasks));
    finalizerQueueEpoch_ = epoch;
    finalizerQueueCondVar_.notify_all();
}

bool kotlin::gc::FinalizerProcessor::IsRunning() noexcept {
    return finalizerThread_.joinable();
}

void kotlin::gc::FinalizerProcessor::WaitFinalizerThreadInitialized() noexcept {
    std::unique_lock guard(initializedMutex_);
    initializedCondVar_.wait(guard, [this] { return initialized_; });
}

kotlin::gc::FinalizerProcessor::~FinalizerProcessor() {
    StopFinalizerThread();
}
