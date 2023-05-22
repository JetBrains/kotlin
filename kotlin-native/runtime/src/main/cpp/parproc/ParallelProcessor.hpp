/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "../CompilerConstants.hpp"
#include "../KAssert.h"
#include "../Logging.hpp"
#include "../Utils.hpp"
#include "PushOnlyAtomicArray.hpp"

namespace kotlin {

namespace internal {

template<typename Iterable, typename MapFun>
class Map {
public:
    class iterator {
        using Base = typename Iterable::iterator;
        using MappingResult = typename std::invoke_result<MapFun, typename std::iterator_traits<Base>::reference>::type;
    public:
        using difference_type = typename std::iterator_traits<Base>::difference_type;
        using value_type = typename std::remove_reference<MappingResult>::type;
        using pointer = value_type*;
        using reference = value_type&;
        using iterator_category = std::input_iterator_tag;

        iterator(typename Iterable::iterator base, const MapFun& mapping) : base_(base), mapping_(mapping) {}
        iterator(const iterator&) noexcept = default;
        iterator& operator=(const iterator&) noexcept = default;

        reference operator*() noexcept { return mapping_(*base_); }

        iterator& operator++() noexcept {
            ++base_;
            return *this;
        }

        iterator operator++(int) noexcept {
            auto result = *this;
            ++base_;
            return result;
        }

        bool operator==(const iterator& rhs) const noexcept { return base_ == rhs.base_; }
        bool operator!=(const iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        typename Iterable::iterator base_;
        const MapFun& mapping_;
    };

    Map(Iterable& iterable, MapFun&& mapping) : iterable_(iterable), mapping_(std::move(mapping)) {}

    iterator begin() noexcept {
        return iterator{iterable_.begin(), mapping_};
    }

    iterator end() noexcept {
        return iterator{iterable_.end(), mapping_};
    }

private:
    Iterable& iterable_;
    MapFun mapping_;
};

} // namespace internal

/**
 * Coordinates a group of workers working in parallel on a large amounts of identical tasks.
 * The dispatcher will try to balance the work among the workers.
 *
 * In order for the work to be completed:
 * 1.  There must be exactly `expectedWorkers()` number of workers instantiated;
 * 2.  Every worker must execute `performWork` sooner or later;
 * 3.  No work must be pushed into a worker's work list from outside (by any means other than `serialWorkProcessor`)
 *     after the start of `performWork` execution.
 */
template <std::size_t kMaxWorkers, template <typename> typename CooperativeWorkList>
class ParallelProcessor : private Pinned {
public:

    using WorkListInterface = typename CooperativeWorkList<ParallelProcessor>::LocalQueue;

    class Worker : private Pinned {
        friend ParallelProcessor;
    public:
        explicit Worker(ParallelProcessor& dispatcher)
                : dispatcher_(dispatcher), workList_(dispatcher_.commonWorkList_) {
            dispatcher_.registerWorker(*this);
        }

        WorkListInterface& workList() {
            return workList_;
        }

        template <typename SerialWorkProcessor>
        void performWork(SerialWorkProcessor serialWorkProcessor) {
            RuntimeAssert(dispatcher_.isRegistered(*this),
                          "A worker must be registered in dispatcher before the start of execution");
            while (true) {
                bool hasWork = workList_.tryAcquireWork();
                if (hasWork) {
                    serialWorkProcessor(workList_);
                } else {
                    std::unique_lock lock(dispatcher_.waitMutex_);

                    auto nowWaiting = dispatcher_.waitingWorkers_.fetch_add(1, std::memory_order_relaxed) + 1;
                    RuntimeLogDebug({ "balancing" },
                                    "Worker goes to sleep (now sleeping %zu registered %zu expected %zu)",
                                    nowWaiting,
                                    dispatcher_.registeredWorkers_.size(),
                                    dispatcher_.expectedWorkers_.load());

                    if (dispatcher_.allDone_) {
                        break;
                    }

                    auto registeredWorkers = dispatcher_.registeredWorkers_.size();
                    if (nowWaiting == registeredWorkers
                            && registeredWorkers == dispatcher_.expectedWorkers_.load(std::memory_order_relaxed)) {
                        // we are the last ones awake
                        RuntimeLogDebug({ "balancing" }, "Worker has detected termination");
                        RuntimeAssert(dispatcher_.commonWorkList_.empty(), "There should be no shared tasks left");
                        dispatcher_.allDone_ = true;
                        lock.unlock();
                        dispatcher_.waitCV_.notify_all();
                        break;
                    }

                    dispatcher_.waitCV_.wait(lock);
                    if (dispatcher_.allDone_) {
                        break;
                    }
                    dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                    RuntimeLogDebug({ "balancing" }, "Worker woke up");
                }
            }
            RuntimeAssert(workList_.empty(), "There should be no local tasks left");

            RuntimeAssert(dispatcher_.allDone_, "Work must be done");
            dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
        }

        void waitEveryWorkerTermination() {
            RuntimeAssert(dispatcher_.allDone_.load(), "Must only be called when the work is done");

            std::size_t expected = 0;
            dispatcher_.workersWaitingForTermination_.compare_exchange_strong(expected, dispatcher_.registeredWorkers_.size());

            RuntimeLogDebug({ "balancing" }, "Worker waits for others to terminate");
            while (dispatcher_.waitingWorkers_.load(std::memory_order_relaxed) > 0) {
                std::this_thread::yield();
            }

            --dispatcher_.workersWaitingForTermination_;
        }

    private:
        ParallelProcessor& dispatcher_;
        WorkListInterface workList_;
    };

    explicit ParallelProcessor(size_t expectedWorkers) : commonWorkList_(*this), expectedWorkers_(expectedWorkers) {}

    ~ParallelProcessor() {
        RuntimeAssert(waitingWorkers_.load() == 0, "All the workers must terminate before dispatcher destruction");
        while (workersWaitingForTermination_ > 0) {
            std::this_thread::yield();
        }
    }

    void lowerExpectations(size_t nowExpectedWorkers) {
        RuntimeAssert(nowExpectedWorkers <= kMaxWorkers,
                      "WorkBalancingDispatcher supports max %zu workers, but %zu requested",
                      kMaxWorkers,
                      nowExpectedWorkers);
        RuntimeAssert(nowExpectedWorkers <= expectedWorkers_, "Previous expectation must have been not less");
        RuntimeAssert(nowExpectedWorkers >= registeredWorkers_.size(), "Can't set expectations lower than the number of already registered workers");
        expectedWorkers_ = nowExpectedWorkers;
        RuntimeAssert(registeredWorkers_.size() <= expectedWorkers_, "Must not have registered more jobs than expected");
    }

    size_t expectedWorkers() {
        return expectedWorkers_.load(std::memory_order_relaxed);
    }

    size_t registeredWorkers() {
        return registeredWorkers_.size(std::memory_order_relaxed);
    }

private:
    void registerWorker(Worker& worker) {
        RuntimeAssert(worker.workList_.empty(), "Work list of an unregistered worker must be empty (e.g. fully depleted earlier)");
        RuntimeAssert(!allDone_, "Dispatcher must wait for every possible worker to register before finishing the work");
        RuntimeAssert(!isRegistered(worker), "Task registration is not idempotent");

        RuntimeAssert(registeredWorkers_.size() + 1 <= expectedWorkers_, "Impossible to register more tasks than expected");
        registeredWorkers_.push(&worker);
        RuntimeLogDebug({ "balancing" }, "Worker registered");

        if (registeredWorkers_.size() == expectedWorkers_) {
            RuntimeLogDebug({ "balancing" }, "All the expected workers registered");
        }
    }

    // Primarily to be used in assertions
    bool isRegistered(const Worker& worker) const {
        for (size_t i = 0; i < registeredWorkers_.size(std::memory_order_acquire); ++i) {
            if (registeredWorkers_[i] == &worker) return true;
        }
        return false;
    }

    friend typename CooperativeWorkList<ParallelProcessor>::CommonStorage;
    friend typename CooperativeWorkList<ParallelProcessor>::LocalQueue;

    void onShare(std::size_t sharedAmount) {
        RuntimeAssert(sharedAmount > 0, "Must have shared something");
        RuntimeLogDebug({ "balancing" }, "Worker has shared %zu tasks", sharedAmount);
        if (waitingWorkers_.load(std::memory_order_relaxed) > 0) {
            waitCV_.notify_all();
        }
    }

    auto workerQueues() {
        return internal::Map(registeredWorkers_, [](std::atomic<Worker*>& worker) -> WorkListInterface& {
            return worker.load(std::memory_order_relaxed)->workList_; });
    }

    typename CooperativeWorkList<ParallelProcessor>::CommonStorage commonWorkList_;

    PushOnlyAtomicArray<Worker*, kMaxWorkers, nullptr> registeredWorkers_;
    std::atomic<size_t> expectedWorkers_ = 0;
    std::atomic<size_t> waitingWorkers_ = 0;

    // special counter indicating the number of workers that can still spin in some loop and read one of the processor's fields
    std::atomic<size_t> workersWaitingForTermination_ = 0;

    std::atomic<bool> allDone_ = false;
    mutable std::mutex waitMutex_;
    mutable std::condition_variable waitCV_;
};

}