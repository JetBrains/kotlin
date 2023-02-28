/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FinalizerProcessor.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "TestSupport.hpp"

using namespace kotlin;

namespace {

class FinalizerProcessorTest : public testing::Test {
public:
    using FinalizerQueue = std_support::vector<int>;

    struct FinalizerQueueTraits {
        static bool isEmpty(const FinalizerQueue& queue) noexcept { return queue.empty(); }
        static void add(FinalizerQueue& into, FinalizerQueue from) noexcept { into.insert(into.end(), from.begin(), from.end()); }
        static void process(FinalizerQueue queue) noexcept {
            AssertThreadState(ThreadState::kRunnable);
            for (auto& obj : queue) {
                setFinalizerHook_->Call(obj);
            }
        }
    };

    using FinalizerProcessor = gc::FinalizerProcessor<FinalizerQueue, FinalizerQueueTraits>;

    FinalizerProcessorTest() noexcept { setFinalizerHook_ = &finalizerHook_; }

    ~FinalizerProcessorTest() { setFinalizerHook_ = nullptr; }

    testing::MockFunction<void(int)>& finalizerHook() { return finalizerHook_; }

private:
    static testing::MockFunction<void(int)>* setFinalizerHook_;
    testing::StrictMock<testing::MockFunction<void(int)>> finalizerHook_;
};

// static
testing::MockFunction<void(int)>* FinalizerProcessorTest::setFinalizerHook_ = nullptr;

int threadsCount() {
    auto iter = mm::ThreadRegistry::Instance().LockForIter();
    return std::distance(iter.begin(), iter.end());
};

} // namespace

TEST_F(FinalizerProcessorTest, NotRunningThreadWhenUnused) {
    FinalizerProcessor processor([](int64_t) {});
    ASSERT_EQ(threadsCount(), 0);
    ASSERT_FALSE(processor.IsRunning());
    FinalizerQueue queue;
    processor.ScheduleTasks(std::move(queue), 1);
    ASSERT_EQ(threadsCount(), 0);
    ASSERT_FALSE(processor.IsRunning());
}

TEST_F(FinalizerProcessorTest, RemoveObject) {
    RunInNewThread([this] {
        ASSERT_EQ(threadsCount(), 1);
        std::atomic<int64_t> done = 0;
        FinalizerProcessor processor([&](int64_t epoch) { done = epoch; });
        FinalizerQueue queue;
        auto obj = 42;
        queue.push_back(obj);
        EXPECT_CALL(finalizerHook(), Call(obj));
        processor.ScheduleTasks(std::move(queue), 1);
        while (done != 1) {
        }
        ASSERT_EQ(threadsCount(), 2);
        ASSERT_TRUE(processor.IsRunning());
        processor.StopFinalizerThread();
        ASSERT_EQ(threadsCount(), 1);
    });
}

TEST_F(FinalizerProcessorTest, ScheduleTasksWhileFinalizing) {
    RunInNewThread([this] {
        std::atomic<int64_t> done = 0;
        FinalizerProcessor processor([&done](int64_t epoch) { done = epoch; });
        std::vector<FinalizerQueue> queues;
        int epochs = 100;
        std::vector<int> objects;
        for (int epoch = 0; epoch < epochs; epoch++) {
            FinalizerQueue queue;
            for (int i = 0; i < 10; i++) {
                auto obj = objects.size();
                queue.push_back(obj);
                objects.push_back(obj);
            }
            queues.emplace_back(std::move(queue));
        }
        for (auto object : objects) {
            EXPECT_CALL(finalizerHook(), Call(object));
        }
        for (int epoch = 0; epoch < epochs; epoch++) {
            processor.ScheduleTasks(std::move(queues[epoch]), epoch + 1);
        }
        while (done != epochs) {
        }
        ASSERT_EQ(threadsCount(), 2);
        ASSERT_TRUE(processor.IsRunning());
        processor.StopFinalizerThread();
        ASSERT_EQ(threadsCount(), 1);
    });
}
