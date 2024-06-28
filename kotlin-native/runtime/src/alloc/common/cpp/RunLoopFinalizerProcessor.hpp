/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cinttypes>
#include <deque>
#include <mutex>

#include "Clock.hpp"
#include "Logging.hpp"
#include "Utils.hpp"
#include "objc_support/RunLoopSource.hpp"
#include "objc_support/RunLoopTimer.hpp"
#include "objc_support/AutoreleasePool.hpp"

namespace kotlin::alloc {

// Configuration for `RunLoopFinalizerProcessor`
// When updating the default values, do not forget to update stdlib API docs.
struct RunLoopFinalizerProcessorConfig {
    // How long can finalizers be processed in a single task. If some finalizer takes too long, the entire
    // batch of `batchSize` will overshoot this target.
    // This cannot be too large to allow the attached run loop process other tasks (not from this finalizer processor).
    std::chrono::nanoseconds maxTimeInTask = std::chrono::milliseconds(300);
    // The minimum time between two tasks.
    // This cannot be too small to allow the attached run loop process other tasks (not from this finalizer processor).
    std::chrono::nanoseconds minTimeBetweenTasks = std::chrono::milliseconds(1);
    // How many finalizers are processed in a single batch in a single autoreleasepool.
    uint64_t batchSize = 100;
};

#if KONAN_HAS_FOUNDATION_FRAMEWORK

// Finalizer processor that runs on `CFRunLoop`.
//
// It's attached to a run loop via `attachToCurrentRunLoop` and detached when the returned `Subscription`
// is destroyed. It cannot be simulatenously attached to multiple run loops.
//
// Tasks are scheduled via `schedule` and are guaranteed to be processed before the tasks from the next `schedule` call.
// The processor will process finalizers in groups of `batchSize` and will stop after either all finalizers are processed or
// more than `maxTimeInTask` have passed (if some finalizer takes a very long time, the overshoot may be significant). The
// processor will not start processing next finalizers for `minTimeBetweenTasks`.
//
// The default configuration may be changed by `withConfig`.
template <typename FinalizerQueue, typename FinalizerQueueTraits>
class RunLoopFinalizerProcessor : private Pinned {
public:
    // A token that `RunLoopFinalizerProcessor` is attached to a run loop.
    //
    // Must be destroyed on the same thread that called `attachToCurrentRunLoop`.
    class [[nodiscard]] Subscription : private Pinned {
    public:
        ~Subscription() = default;

    private:
        friend class RunLoopFinalizerProcessor;

        explicit Subscription(RunLoopFinalizerProcessor& owner) noexcept :
            sourceSubscription_(owner.source_.attachToCurrentRunLoop()), timerSubscription_(owner.timer_.attachToCurrentRunLoop()) {}

        std::unique_ptr<objc_support::RunLoopSource::Subscription> sourceSubscription_;
        std::unique_ptr<objc_support::RunLoopTimer::Subscription> timerSubscription_;
    };

    // The constructed processor is not attached to any run loop, and so will not be processing
    // tasks. Call `attachToCurrentRunLoop` to attach it to the current thread's run loop.
    RunLoopFinalizerProcessor() noexcept = default;

    // Schedule `tasks` from epoch `epoch` to be processed on this finalizer processor.
    //
    // It's guaranteed that these `tasks` will be processed only after `tasks` from the previous
    // call to `schedule`.
    void schedule(FinalizerQueue tasks, uint64_t epoch) noexcept {
        if (FinalizerQueueTraits::isEmpty(tasks)) return;
        {
            std::unique_lock guard(queueMutex_);
            queue_.emplace_back(std::move(tasks), epoch);
        }
        source_.signal();
    }

    // Modify the configuration of this `RunLoopFinalizerProcessor`. There's no guarantee, when will it be applied.
    template <typename F>
    std::invoke_result_t<F, RunLoopFinalizerProcessorConfig&> withConfig(F&& f) noexcept {
        std::unique_lock guard(configMutex_);
        return std::invoke(std::forward<F>(f), config_);
    }

    // Attach this `RunLoopFinalizerProcessor` to the current thread's run loop.
    //
    // This processor can only be attached to one run loop at a time.
    Subscription attachToCurrentRunLoop() noexcept { return Subscription(*this); }

private:
    void process() noexcept {
        auto startTime = steady_clock::now();
        {
            std::unique_lock guard(configMutex_);
            auto minStartTime = lastProcessTimestamp_ + config_.minTimeBetweenTasks;
            if (startTime < minStartTime) {
                // `process` is being called too frequently. Wait until the next allowed time.
                auto interval = minStartTime - startTime;
                // TODO: std::common_type between double and saturated is undefined.
                using Unsaturated = std::chrono::duration<decltype(interval)::rep::value_type, decltype(interval)::period>;
                auto unsaturatedInterval = Unsaturated(interval);
                timer_.setNextFiring(unsaturatedInterval);
                return;
            }
        }
        steady_clock::time_point deadline;
        uint64_t batchCount;
        {
            std::unique_lock guard(configMutex_);
            RuntimeLogDebug(
                    {kTagGC}, "Processing finalizers on a run loop for maximum %" PRId64 "ms",
                    std::chrono::duration_cast<std::chrono::milliseconds>(config_.maxTimeInTask).count());
            deadline = startTime + config_.maxTimeInTask;
            batchCount = config_.batchSize;
        }
        uint64_t processedCount = 0;
        while (true) {
            auto now = steady_clock::now();
            if (now > deadline) {
                // Finalization is being run too long. Stop processing and reschedule until the next allowed time.
                std::unique_lock guard(configMutex_);
                RuntimeLogDebug(
                        {kTagGC}, "Processing %" PRIu64 " finalizers on a run loop has taken %" PRId64 " ms. Stopping for %" PRId64 "ms.",
                        processedCount, std::chrono::duration_cast<milliseconds>(now - startTime).count().value,
                        std::chrono::duration_cast<std::chrono::milliseconds>(config_.minTimeBetweenTasks).count());
                timer_.setNextFiring(config_.minTimeBetweenTasks);
                lastProcessTimestamp_ = now;
                return;
            }
            {
                objc_support::AutoreleasePool autoreleasePool;
                for (uint64_t i = 0; i < batchCount; ++i) {
                    // There's no point checking `deadline` here since the majority of the time will probably
                    // be spent in `AutoreleasePool` destructor.
                    if (!FinalizerQueueTraits::processSingle(currentQueue_.queue)) {
                        break;
                    }
                    ++processedCount;
                }
            }
            if (!FinalizerQueueTraits::isEmpty(currentQueue_.queue)) {
                continue;
            }
            RuntimeLogDebug({kTagGC}, "Epoch #%" PRIu64 ": finished processing finalizers on a run loop", currentQueue_.epoch);
            // Attempt to fill `currentQueue_` from the global `queue_`.
            std::unique_lock guard(queueMutex_);
            if (queue_.empty()) {
                // Let's keep this under the lock. This way if someone were to schedule new tasks, they
                // would definitely have to wait long enough to see the updated lastProcessTimestamp_.
                lastProcessTimestamp_ = steady_clock::now();
                RuntimeLogDebug(
                        {kTagGC}, "Processing %" PRIu64 " finalizers on a run loop has finished in %" PRId64 "ms.", processedCount,
                        std::chrono::duration_cast<milliseconds>(lastProcessTimestamp_ - startTime).count().value);
                return;
            }
            currentQueue_ = std::move(queue_.front());
            RuntimeLogDebug({kTagGC}, "Epoch #%" PRIu64 ": will process finalizers on a run loop", currentQueue_.epoch);
            queue_.pop_front();
            RuntimeAssert(!FinalizerQueueTraits::isEmpty(currentQueue_.queue), "Empty queue should not have been scheduled");
        }
    }

    std::mutex configMutex_;
    RunLoopFinalizerProcessorConfig config_;

    struct ScheduledQueue {
        ScheduledQueue() noexcept = default;
        ScheduledQueue(FinalizerQueue queue, uint64_t epoch) noexcept : queue(std::move(queue)), epoch(epoch) {}

        FinalizerQueue queue;
        uint64_t epoch = 0;
    };

    std::mutex queueMutex_;
    ScheduledQueue currentQueue_;
    std::deque<ScheduledQueue> queue_;

    steady_clock::time_point lastProcessTimestamp_ =
            steady_clock::time_point::min(); // Only accessed by the process() function called only by the `CFRunLoop`.

    objc_support::RunLoopSource source_{[this]() noexcept { process(); }};
    // `timer_` is triggered manually with `setNextFiring`, so `interval` and `initialFiring` are set very high.
    // This follows https://developer.apple.com/documentation/corefoundation/1542501-cfrunlooptimersetnextfiredate#discussion
    objc_support::RunLoopTimer timer_{
            [this]() noexcept { source_.signal(); }, std::chrono::hours(100), objc_support::cf_clock::now() + std::chrono::hours(100)};
};

#endif

} // namespace kotlin::alloc