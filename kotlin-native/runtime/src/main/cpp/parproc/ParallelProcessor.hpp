/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "../CompilerConstants.hpp"
#include "../KAssert.h"
#include "../Logging.hpp"
#include "../Utils.hpp"
#include "Porting.h"
#include "PushOnlyAtomicArray.hpp"
#include "SplitSharedList.hpp"

namespace kotlin {

namespace internal {

enum class ShareOn { kPush, kPop };

} // namespace internal

/**
 * Coordinates a group of workers working in parallel on a large amounts of identical tasks.
 * The dispatcher will try to balance the work among the workers.
 *
 * Requirements:
 * -  Every instantiated worker must execute `tryPop` sooner or later;
 * -  No worker must be instantiated after at least one other worker has executed `tryPop`
 */
template <std::size_t kMaxWorkers, typename ListImpl, std::size_t kMinSizeToShare, std::size_t kMaxSizeToSteal = kMinSizeToShare / 2, internal::ShareOn kShareOn = internal::ShareOn::kPush>
class ParallelProcessor : private Pinned {
public:
    static const std::size_t kStealingAttemptCyclesBeforeWait = 4;

    class Worker : private Pinned {
        friend ParallelProcessor;
    public:
        explicit Worker(ParallelProcessor& dispatcher) : dispatcher_(dispatcher) {
            dispatcher_.registerWorker(*this);
        }

        ALWAYS_INLINE bool empty() const noexcept {
            return list_.localEmpty() && list_.sharedEmpty();
        }

        ALWAYS_INLINE bool tryPushLocal(typename ListImpl::reference value) noexcept {
            return list_.tryPushLocal(value);
        }

        ALWAYS_INLINE bool tryPush(typename ListImpl::reference value) noexcept {
            bool pushed = tryPushLocal(value);
            if (pushed && kShareOn == internal::ShareOn::kPush) {
                shareAll();
            }
            return pushed;
        }

        ALWAYS_INLINE typename ListImpl::pointer tryPopLocal() noexcept {
            return list_.tryPopLocal();
        }

        ALWAYS_INLINE typename ListImpl::pointer tryPop() noexcept {
            while (true) {
                if (auto popped = tryPopLocal()) {
                    if (kShareOn == internal::ShareOn::kPop) {
                        shareAll();
                    }
                    return popped;
                }
                if (tryAcquireWork()) {
                    continue;
                }
                break;
            }
            return nullptr;
        }

    private:
        bool tryTransferFromLocal() noexcept {
            auto transferred = list_.tryTransferFrom(list_, kMaxSizeToSteal);
            if (transferred > 0) {
                RuntimeLogDebug({"balancing"}, "Worker has acquired %zu tasks from itself", transferred);
                return true;
            }
            return false;
        }

        bool tryTransferFromCooperating() {
            for (size_t i = 0; i < kStealingAttemptCyclesBeforeWait; ++i) {
                for (auto& fromAtomic : dispatcher_.registeredWorkers_) {
                    auto& from = *fromAtomic.load(std::memory_order_relaxed);
                    auto transferred = list_.tryTransferFrom(from.list_, kMaxSizeToSteal);
                    if (transferred > 0) {
                        RuntimeLogDebug({"balancing"}, "Worker has acquired %zu tasks from %d", transferred, from.carrierThreadId_);
                        return true;
                    }
                }
                std::this_thread::yield();
            }
            return false;
        }

        bool tryAcquireWork() noexcept {
            if (tryTransferFromLocal()) return true;
            if (tryTransferFromCooperating()) return true;

            RuntimeLogDebug({"balancing"}, "Worker has not found a victim to steal from :(");

            return waitForMoreWork();
        }

        bool waitForMoreWork() noexcept {
            std::unique_lock lock(dispatcher_.waitMutex_);

            auto nowWaiting = dispatcher_.waitingWorkers_.fetch_add(1, std::memory_order_relaxed) + 1;
            RuntimeLogDebug({ "balancing" }, "Worker goes to sleep (now sleeping %zu of %zu)",
                            nowWaiting, dispatcher_.registeredWorkers_.size());

            if (dispatcher_.allDone_) {
                dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                return false;
            }

            if (nowWaiting == dispatcher_.registeredWorkers_.size()) {
                // we are the last ones awake
                RuntimeLogDebug({ "balancing" }, "Worker has detected termination");
                dispatcher_.allDone_ = true;
                lock.unlock();
                dispatcher_.waitCV_.notify_all();
                dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                return false;
            }

            dispatcher_.waitCV_.wait(lock);
            dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
            if (dispatcher_.allDone_) {
                return false;
            }
            RuntimeLogDebug({ "balancing" }, "Worker woke up");

            return true;
        }

        void shareAll() noexcept {
            if (list_.localSize() > kMinSizeToShare) {
                auto shared = list_.shareAllWith(list_);
                if (shared > 0) {
                    dispatcher_.onShare(shared);
                }
            }
        }

        const int carrierThreadId_ = konan::currentThreadId();
        ParallelProcessor& dispatcher_;
        SplitSharedList<ListImpl> list_;
    };

    ParallelProcessor() = default;

    ~ParallelProcessor() {
        RuntimeAssert(waitingWorkers_.load() == 0, "All the workers must terminate before dispatcher destruction");
    }

    size_t registeredWorkers() {
        return registeredWorkers_.size(std::memory_order_relaxed);
    }

private:
    void registerWorker(Worker& worker) {
        RuntimeAssert(worker.empty(), "Work list of an unregistered worker must be empty (e.g. fully depleted earlier)");
        RuntimeAssert(!allDone_, "Dispatcher must wait for every possible worker to register before finishing the work");
        RuntimeAssert(!isRegistered(worker), "Task registration is not idempotent");

        registeredWorkers_.push(&worker);
        RuntimeLogDebug({ "balancing" }, "Worker registered");
    }

    // Primarily to be used in assertions
    bool isRegistered(const Worker& worker) const {
        for (size_t i = 0; i < registeredWorkers_.size(std::memory_order_acquire); ++i) {
            if (registeredWorkers_[i] == &worker) return true;
        }
        return false;
    }

    void onShare(std::size_t sharedAmount) {
        RuntimeAssert(sharedAmount > 0, "Must have shared something");
        RuntimeLogDebug({ "balancing" }, "Worker has shared %zu tasks", sharedAmount);
        if (waitingWorkers_.load(std::memory_order_relaxed) > 0) {
            waitCV_.notify_all();
        }
    }

    PushOnlyAtomicArray<Worker*, kMaxWorkers, nullptr> registeredWorkers_;
    std::atomic<size_t> waitingWorkers_ = 0;

    std::atomic<bool> allDone_ = false;
    mutable std::mutex waitMutex_;
    mutable std::condition_variable waitCV_;
};

}
