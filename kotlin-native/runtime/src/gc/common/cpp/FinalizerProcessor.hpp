/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <cstdint>
#include <functional>
#include <mutex>
#include <utility>

#include "KAssert.h"
#include "Memory.h"
#include "Runtime.h"
#include "ScopedThread.hpp"
#include "Utils.hpp"
#include "Logging.hpp"
#include "objc_support/AutoreleasePool.hpp"
#include "objc_support/RunLoopSource.hpp"

#if KONAN_OBJC_INTEROP
#include <CoreFoundation/CFRunLoop.h>
#endif

namespace kotlin::gc {

template <typename FinalizerQueue, typename FinalizerQueueTraits>
class FinalizerProcessor : private Pinned {
public:
    // epochDoneCallback could be called on any subset of them.
    // If no new tasks are set, epochDoneCallback will be eventually called on last epoch
    explicit FinalizerProcessor(std::function<void(int64_t)> epochDoneCallback) noexcept :
        epochDoneCallback_(std::move(epochDoneCallback)), processingLoop_(makeProcessingLoop(*this)) {}

    ~FinalizerProcessor() { StopFinalizerThread(); }

    void ScheduleTasks(FinalizerQueue tasks, int64_t epoch) noexcept {
        std::unique_lock guard(finalizerQueueMutex_);
        if (FinalizerQueueTraits::isEmpty(tasks) && !IsRunning()) {
            epochDoneCallback_(epoch);
            return;
        }
        finalizerQueueCondVar_.wait(guard, [this] { return newTasksAllowed_; });
        StartFinalizerThreadIfNone();
        FinalizerQueueTraits::add(finalizerQueue_, std::move(tasks));
        finalizerQueueEpoch_ = epoch;
        processingLoop_->notify();
    }

    void StopFinalizerThread() noexcept {
        {
            std::unique_lock guard(finalizerQueueMutex_);
            if (!finalizerThread_.joinable()) return;
            shutdownFlag_ = true;
            processingLoop_->notify();
        }
        finalizerThread_.join();
        shutdownFlag_ = false;
        RuntimeAssert(FinalizerQueueTraits::isEmpty(finalizerQueue_), "Finalizer queue should be empty when killing finalizer thread");
        std::unique_lock guard(finalizerQueueMutex_);
        newTasksAllowed_ = true;
        finalizerQueueCondVar_.notify_all();
    }

    bool IsRunning() const noexcept { return finalizerThread_.joinable(); }

    void StartFinalizerThreadIfNone() noexcept {
        std::unique_lock guard(threadCreatingMutex_);
        if (finalizerThread_.joinable()) return;

        finalizerThread_ = ScopedThread(ScopedThread::attributes().name("GC finalizer processor"), [this] {
            processingLoop_->initThreadData();
            Kotlin_initRuntimeIfNeeded();
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = true;
            }
            initializedCondVar_.notify_all();
            processingLoop_->body();
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = false;
            }
            initializedCondVar_.notify_all();
        });
    }

    void WaitFinalizerThreadInitialized() noexcept {
        std::unique_lock guard(initializedMutex_);
        initializedCondVar_.wait(guard, [this] { return initialized_; });
    }

private:
    // should be called under the finalizerQueueMutex_
    bool hasNewTasks(int64_t lastProcessedEpoch) noexcept { // FIXME name
        return !FinalizerQueueTraits::isEmpty(finalizerQueue_) || finalizerQueueEpoch_ != lastProcessedEpoch;
    }

    void processSingle(FinalizerQueue&& queue, int64_t currentEpoch) noexcept {
        if (!FinalizerQueueTraits::isEmpty(queue)) {
            objc_support::AutoreleasePool autoreleasePool;
            ThreadStateGuard guard(ThreadState::kRunnable);
            FinalizerQueueTraits::process(std::move(queue));
        }
        epochDoneCallback_(currentEpoch);
    }

    class ProcessingLoop : private Pinned {
    public:
        virtual ~ProcessingLoop() = default;

        virtual void notify() = 0;
        virtual void initThreadData() = 0;
        virtual void body() = 0;
    };

#if KONAN_OBJC_INTEROP
    class ProcessingLoopWithCFImpl final : public ProcessingLoop {
    public:
        explicit ProcessingLoopWithCFImpl(FinalizerProcessor& owner) : owner_(owner), runLoopSource_([this]() noexcept { handleNewFinalizers(); }) {}

        void notify() override {
            // wait until runLoop_ ptr is published
            while (runLoop_.load(std::memory_order_acquire) == nullptr) {
                std::this_thread::yield();
            }
            // notify
            runLoopSource_.signal();
            CFRunLoopWakeUp(runLoop_);
        }

        void initThreadData() override {
            runLoop_.store(CFRunLoopGetCurrent(), std::memory_order_release);
        }

        void body() override {
            objc_support::AutoreleasePool autoreleasePool;
            {
                auto subscription = runLoopSource_.attachToCurrentRunLoop();
                CFRunLoopRun();
            }
            runLoop_.store(nullptr, std::memory_order_release);
        }

    private:
        void handleNewFinalizers() {
            std::unique_lock lock(owner_.finalizerQueueMutex_);
            if (owner_.shutdownFlag_) {
                owner_.newTasksAllowed_ = false;
                CFRunLoopStop(runLoop_.load(std::memory_order_acquire));
                return;
            }
            auto queue = std::move(owner_.finalizerQueue_);
            int64_t currentEpoch = owner_.finalizerQueueEpoch_;
            lock.unlock();

            owner_.processSingle(std::move(queue), currentEpoch);
            finishedEpoch_ = currentEpoch;
        }

        FinalizerProcessor& owner_;
        int64_t finishedEpoch_ = 0;
        objc_support::RunLoopSource runLoopSource_;
        std::atomic<CFRunLoopRef> runLoop_ = nullptr;
    };
#endif

    class ProcessingLoopImpl final : public ProcessingLoop {
    public:
        explicit ProcessingLoopImpl(FinalizerProcessor& owner) : owner_(owner) {}

        void notify() override {
            owner_.finalizerQueueCondVar_.notify_all();
        }

        void initThreadData() override { /* noop */ }

        void body() override {
            int64_t finishedEpoch = 0;
            while (true) {
                std::unique_lock lock(owner_.finalizerQueueMutex_);
                owner_.finalizerQueueCondVar_.wait(lock, [this, &finishedEpoch] {
                    return owner_.hasNewTasks(finishedEpoch) || owner_.shutdownFlag_;
                });
                if (!owner_.hasNewTasks(finishedEpoch)) {
                    RuntimeAssert(owner_.shutdownFlag_, "Nothing to do, but no shutdownFlag_ is set on wakeup");
                    owner_.newTasksAllowed_ = false;
                    break;
                }
                auto queue = std::move(owner_.finalizerQueue_);
                auto currentEpoch = owner_.finalizerQueueEpoch_;
                lock.unlock();
                owner_.processSingle(std::move(queue), currentEpoch);
                finishedEpoch = currentEpoch;
            }
        }

    private:
        FinalizerProcessor& owner_;
    };

    static std::unique_ptr<ProcessingLoop> makeProcessingLoop(FinalizerProcessor& owner) noexcept {
#if KONAN_OBJC_INTEROP
        if (compiler::objcDisposeWithRunLoop()) {
            return std::make_unique<ProcessingLoopWithCFImpl>(owner);
        }
#endif
        return std::make_unique<ProcessingLoopImpl>(owner);
    }

    ScopedThread finalizerThread_;
    FinalizerQueue finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t finalizerQueueEpoch_ = 0;
    bool shutdownFlag_ = false;
    bool newTasksAllowed_ = true;

    std::unique_ptr<ProcessingLoop> processingLoop_;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;

};

} // namespace kotlin::gc
