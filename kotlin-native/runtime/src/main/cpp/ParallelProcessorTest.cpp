/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ParallelProcessor.hpp"

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

auto workBatch(std::size_t size) {
    std::list<Task> batch;
    for (size_t i = 0; i < size; ++i) {
        batch.emplace_back();
    }
    return batch;
}

template <typename WorkList, typename Iterable>
void offerWork(WorkList& wl, Iterable& batch) {
    for (auto& task: batch) {
        bool accepted = wl.tryPush(task);
        RuntimeAssert(accepted, "Must be accepted");
    }
}

using ListImpl = intrusive_forward_list<Task>;
static constexpr auto kBatchSize = 256;
using Processor = ParallelProcessor<ListImpl, kBatchSize, 1024>;
using Worker = typename Processor::Worker;

} // namespace


TEST(ParallelProcessorTest, ContededRegistration) {
    Processor processor;
    std::vector<std::unique_ptr<Worker>> workers(kDefaultThreadCount);

    std::atomic<bool> start = false;
    std::list<ScopedThread> threads;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        threads.emplace_back([i, &start, &workers, &processor] {
            while (!start.load()) {}
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

    auto work = workBatch(kBatchSize * 2);
    offerWork(giver, work);

    EXPECT_TRUE(taker.localEmpty());

    // have to steal from giver
    EXPECT_NE(taker.tryPop(), nullptr);

    EXPECT_FALSE(taker.localEmpty());
}
