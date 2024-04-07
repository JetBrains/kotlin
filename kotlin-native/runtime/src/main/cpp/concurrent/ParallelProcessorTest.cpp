/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "concurrent/ParallelProcessor.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include <list>
#include <vector>

#include "IntrusiveList.hpp"
#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"

using ::testing::_;
using namespace kotlin;

namespace {

struct Task {
    template <typename WorkList>
    static void workLoop(WorkList& workList) {
        while (Task* task = workList.tryPop()) {
            RuntimeAssert(!task->done_.load(), "Tasks are not idempotent");
            task->done_ = true;
        }
    }

    Task* next() const noexcept { return next_; }

    void setNext(Task* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_ = next;
    }

    bool trySetNext(Task* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (next_ == nullptr) {
            next_ = next;
            return true;
        }
        return false;
    }

    std::atomic<bool> done_ = false;
    Task* next_ = nullptr;
};

auto createWork(std::size_t size) {
    std::list<Task> batch;
    for (size_t i = 0; i < size; ++i) {
        batch.emplace_back();
    }
    return batch;
}

template <typename WorkList, typename Iterable>
void offerWork(WorkList& wl, Iterable& batch) {
    for (auto& task : batch) {
        bool accepted = wl.tryPush(task);
        RuntimeAssert(accepted, "Must be accepted");
    }
}

using ListImpl = intrusive_forward_list<Task>;
constexpr auto kBatchSize = 256;
constexpr auto kBatchPoolSize = 4;
using Processor = ParallelProcessor<ListImpl, kBatchSize, kBatchPoolSize>;
using Worker = typename Processor::Worker;
using WorkSource = typename Processor::WorkSource;

} // namespace

TEST(ParallelProcessorTest, ContededRegistration) {
    Processor processor;
    std::vector<std::unique_ptr<Worker>> workers(kDefaultThreadCount);

    std::atomic<bool> start = false;
    std::list<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([i, &start, &workers, &processor] {
            while (!start.load()) {
            }
            workers[i] = std::make_unique<Worker>(processor);
        });
    }

    start = true;

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(processor.registeredWorkers(), kDefaultThreadCount);

    workers.clear();
}

TEST(ParallelProcessorTest, Sharing) {
    Processor processor;

    Worker giver(processor);
    Worker taker(processor);
    EXPECT_THAT(processor.registeredWorkers(), 2);

    auto twoBatches = createWork(kBatchSize * 2);
    offerWork(giver, twoBatches);

    EXPECT_TRUE(taker.retainsNoWork());

    // have to steal from giver
    EXPECT_NE(taker.tryPop(), nullptr);

    EXPECT_FALSE(taker.retainsNoWork());
    taker.clear();
    giver.clear();
}

TEST(ParallelProcessorTest, SharingFromNonWorkerSource) {
    Processor processor;

    WorkSource giver(processor);
    EXPECT_THAT(processor.registeredWorkers(), 0);

    Worker taker(processor);
    EXPECT_THAT(processor.registeredWorkers(), 1);

    auto work = createWork(kBatchSize * 2);
    offerWork(giver, work);

    EXPECT_TRUE(taker.retainsNoWork());

    // have to steal from giver
    EXPECT_NE(taker.tryPop(), nullptr);

    EXPECT_FALSE(taker.retainsNoWork());
    taker.clear();
    giver.clear();
}

TEST(ParallelProcessorTest, Overflow) {
    Processor processor;
    Worker worker(processor);

    auto workSize = kBatchSize * (kBatchPoolSize + 2);
    auto work = createWork(workSize);
    offerWork(worker, work);

    std::size_t poppedCount = 0;
    while (worker.tryPop() != nullptr) {
        ++poppedCount;
    }
    EXPECT_THAT(poppedCount, workSize);
    EXPECT_THAT(worker.retainsNoWork(), true);
}

TEST(ParallelProcessorTest, ForceFlush) {
    Processor processor;

    WorkSource source(processor);

    auto workSize = kBatchSize / 2;
    auto halfBatch = createWork(workSize);
    offerWork(source, halfBatch);

    EXPECT_THAT(source.forceFlush(), true);
    EXPECT_THAT(source.retainsNoWork(), true);

    Worker checker(processor);
    std::size_t poppedCount = 0;
    while (checker.tryPop() != nullptr) {
        ++poppedCount;
    }
    EXPECT_THAT(poppedCount, workSize);
}

TEST(ParallelProcessorTest, ForceFlushWithOverflow) {
    Processor processor;

    // Fill up the processor's work pool
    Worker overflower(processor);

    auto poolSizeBatches = createWork(kBatchSize * kBatchPoolSize);
    offerWork(overflower, poolSizeBatches);

    EXPECT_THAT(overflower.forceFlush(), true);

    // No overflow a local work source
    WorkSource source(processor);

    auto twoBatches = createWork(kBatchSize * 2);
    offerWork(source, twoBatches);

    // no space to flush into
    EXPECT_THAT(source.forceFlush(), false);

    // drain the processor's pool
    while (overflower.tryPop() != nullptr) {}

    // now can flush
    EXPECT_THAT(source.forceFlush(), true);
    EXPECT_THAT(source.retainsNoWork(), true);
}
