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

#if KONAN_OBJC_INTEROP
#include "ObjCMMAPI.h"
#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFRunLoop.h>
#endif

namespace kotlin::gc {

template <typename FinalizerQueue, typename FinalizerQueueTraits>
class FinalizerProcessor : private Pinned {
public:
    // epochDoneCallback could be called on any subset of them.
    // If no new tasks are set, epochDoneCallback will be eventually called on last epoch
    explicit FinalizerProcessor(std::function<void(int64_t)> epochDoneCallback) noexcept :
        epochDoneCallback_(std::move(epochDoneCallback)), processingLoop_(*this) {}

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
        processingLoop_.notify();
    }

    void StopFinalizerThread() noexcept {
        {
            std::unique_lock guard(finalizerQueueMutex_);
            if (!finalizerThread_.joinable()) return;
            shutdownFlag_ = true;
            processingLoop_.notify();
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
            processingLoop_.initThreadData();
            Kotlin_initRuntimeIfNeeded();
            {
                std::unique_lock guard(initializedMutex_);
                initialized_ = true;
            }
            initializedCondVar_.notify_all();
            processingLoop_.body();
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
#if KONAN_OBJC_INTEROP
            konan::AutoreleasePool autoreleasePool;
#endif
            ThreadStateGuard guard(ThreadState::kRunnable);
            FinalizerQueueTraits::process(std::move(queue));
        }
        epochDoneCallback_(currentEpoch);
    }

#if KONAN_OBJC_INTEROP
    class ProcessingLoop {
    public:
        explicit ProcessingLoop(FinalizerProcessor& owner) :
                owner_(owner),
                sourceContext_{
                        0, this, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr,
                        [](void* info) {
                            auto& self = *reinterpret_cast<ProcessingLoop*>(info);
                            self.handleNewFinalizers();
                        }},
                runLoopSource_(CFRunLoopSourceCreate(nullptr, 0, &sourceContext_)) {}

        ~ProcessingLoop() {
            CFRelease(runLoopSource_);
        }

        void notify() {
            // wait until runLoop_ ptr is published
            while (runLoop_.load(std::memory_order_acquire) == nullptr) {
                std::this_thread::yield();
            }
            // notify
            CFRunLoopSourceSignal(runLoopSource_);
            CFRunLoopWakeUp(runLoop_);
        }

        void initThreadData() {
            runLoop_.store(CFRunLoopGetCurrent(), std::memory_order_release);
        }

        void body() {
            konan::AutoreleasePool autoreleasePool;
            auto mode = kCFRunLoopDefaultMode;
            CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource_, mode);

            CFRunLoopRun();

            CFRunLoopRemoveSource(CFRunLoopGetCurrent(), runLoopSource_, mode);
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
        CFRunLoopSourceContext sourceContext_;
        std::atomic<CFRunLoopRef> runLoop_ = nullptr;
        CFRunLoopSourceRef runLoopSource_;
    };
#else
    class ProcessingLoop {
    public:
        explicit ProcessingLoop(FinalizerProcessor& owner) : owner_(owner) {}

        void notify() {
            owner_.finalizerQueueCondVar_.notify_all();
        }

        void initThreadData() { /* noop */ }

        void body() {
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
#endif

    ScopedThread finalizerThread_;
    FinalizerQueue finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t finalizerQueueEpoch_ = 0;
    bool shutdownFlag_ = false;
    bool newTasksAllowed_ = true;

    ProcessingLoop processingLoop_;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;

};

} // namespace kotlin::gc
