/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include <list>

#include "../IntrusiveList.hpp"

#include "CooperativeWorkLists.hpp"
#include "ParallelProcessor.hpp"

#include "std_support/Vector.hpp"
#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"

using ::testing::_;
using namespace kotlin;

template <typename T>
class ParallelProcessorTest : public testing::Test {};

constexpr auto kMaxWorkers = kDefaultThreadCount * 2;

struct Task {
    template<typename WorkList>
    static void workLoop(WorkList& workList, std::atomic<std::size_t>& counter) {
        while (Task* task = workList.tryPop()) {
            RuntimeAssert(!task->done_.load(), "Tasks are not idempotent");
            task->done_ = true;
            ++counter;
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

using ListImpl = intrusive_forward_list<Task>;

struct NonSharingTest {
    template<typename PP> using CWL = NonSharing<PP, ListImpl>;
    using Processor = ParallelProcessor<kMaxWorkers, CWL>;
    static constexpr const char* name = "NonSharing";
};

struct SharedGlobalQueueTest {
    template<typename PP> using CWL = SharedGlobalQueue<PP, ListImpl, 256>;
    using Processor = ParallelProcessor<kMaxWorkers, CWL>;
    static constexpr const char* name = "SharedGlobalQueue";
};

struct SharableQueuePerWorkerTest {
    template<typename PP> using CWL = SharableQueuePerWorker<PP, ListImpl, 256>;
    using Processor = ParallelProcessor<kMaxWorkers, CWL>;
    static constexpr const char* name = "SharableQueuePerWorker";
};

using ParallelProcessorKinds = testing::Types<NonSharingTest, SharedGlobalQueueTest, SharableQueuePerWorkerTest>;

class ParallelProcessorKindNames {
public:
    template <typename T>
    static std::string GetName(int) {
        return T::name;
    }
};

TYPED_TEST_SUITE(ParallelProcessorTest, ParallelProcessorKinds, ParallelProcessorKindNames);

TYPED_TEST(ParallelProcessorTest, ContededRegistration) {
    using ParallelProcessor = typename TypeParam::Processor;
    using Worker = typename ParallelProcessor::Worker;

    ParallelProcessor processor(kDefaultThreadCount);
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

TYPED_TEST(ParallelProcessorTest, Latecomers) {
    using ParallelProcessor = typename TypeParam::Processor;
    using Worker = typename ParallelProcessor::Worker;

    ParallelProcessor processor(kMaxWorkers);

    const auto kTasksPerWorker = 1000;
    std::vector<Task> tasks(kMaxWorkers * kTasksPerWorker);
    std::atomic<std::size_t> counter = 0;

    std::vector<std::unique_ptr<Worker>> workers;
    for (int i = 0; i < kMaxWorkers; ++i) {
        workers.emplace_back(std::make_unique<Worker>(processor));
    }

    std::atomic<bool> firstHalfStart = false;
    std::list<ScopedThread> firstHalf;
    for (int workerIdx = 0; workerIdx < kDefaultThreadCount; ++workerIdx) {
        firstHalf.emplace_back([&, workerIdx] {
            auto& worker = *workers[workerIdx];
            for (int taskIdx = 0; taskIdx < kTasksPerWorker; ++taskIdx) {
                bool pushed = worker.workList().tryPush(tasks[workerIdx * kTasksPerWorker + taskIdx]);
                RuntimeAssert(pushed, "Must push");
            }
            while (!firstHalfStart.load()) {}
            worker.performWork([&](auto& workList) { Task::workLoop(workList, counter); });
        });
    }

    firstHalfStart = true;

    while (counter < kDefaultThreadCount * kTasksPerWorker) {
        std::this_thread::yield();
    }

    std::atomic<bool> secondHalfStart = false;
    std::list<ScopedThread> secondHalf;
    for (int workerIdx = kDefaultThreadCount; workerIdx < kDefaultThreadCount * 2; ++workerIdx) {
        secondHalf.emplace_back([&, workerIdx] {
            auto& worker = *workers[workerIdx];
            for (int taskIdx = 0; taskIdx < kTasksPerWorker; ++taskIdx) {
                bool pushed = worker.workList().tryPush(tasks[workerIdx * kTasksPerWorker + taskIdx]);
                RuntimeAssert(pushed, "Must push");
            }
            while (!firstHalfStart.load()) {}
            worker.performWork([&](auto& workList) { Task::workLoop(workList, counter); });
        });
    }

    secondHalfStart = true;

    for (auto& t : firstHalf) {
        t.join();
    }

    for (auto& t : secondHalf) {
        t.join();
    }

    EXPECT_THAT(counter.load(), tasks.size());
    EXPECT_TRUE(std::all_of(tasks.begin(), tasks.end(), [](Task& task) { return task.done_.load(); }));

    workers.clear();
}
