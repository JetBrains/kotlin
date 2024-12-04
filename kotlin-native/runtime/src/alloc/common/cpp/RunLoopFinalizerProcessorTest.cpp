/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "RunLoopFinalizerProcessor.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "objc_support/RunLoopTestSupport.hpp"

using namespace kotlin;

namespace {

using FinalizerQueue = std::vector<std::function<void()>>;

struct FinalizerQueueTraits {
    static void add(FinalizerQueue& into, FinalizerQueue from) noexcept {
        into.insert(into.end(), std::make_move_iterator(from.begin()), std::make_move_iterator(from.end()));
    }

    static bool isEmpty(const FinalizerQueue& queue) noexcept { return queue.empty(); }

    static bool processSingle(FinalizerQueue& queue) noexcept {
        if (queue.empty()) return false;
        auto item = std::move(queue.back());
        queue.pop_back();
        item();
        return true;
    }
};

using RunLoopFinalizerProcessor = alloc::RunLoopFinalizerProcessor<FinalizerQueue, FinalizerQueueTraits>;

} // namespace

TEST(RunLoopFinalizerProcessorTest, Basic) {
    RunLoopFinalizerProcessor processor;
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return processor.attachToCurrentRunLoop(); });

    std::array<testing::StrictMock<testing::MockFunction<void()>>, 4> finalizers;

    std::atomic<bool> done = false;
    {
        testing::InSequence seq;
        EXPECT_CALL(finalizers[1], Call());
        EXPECT_CALL(finalizers[0], Call());
        EXPECT_CALL(finalizers[3], Call());
        EXPECT_CALL(finalizers[2], Call()).WillOnce([&] { done.store(true, std::memory_order_release); });
    }
    processor.schedule({finalizers[0].AsStdFunction(), finalizers[1].AsStdFunction()}, 1);
    processor.schedule({finalizers[2].AsStdFunction(), finalizers[3].AsStdFunction()}, 2);
    runLoop.wakeUp();
    while (!done.load(std::memory_order_acquire)) {
        std::this_thread::yield();
    }
}

TEST(RunLoopFinalizerProcessorTest, ScheduleWhileProcessing) {
    RunLoopFinalizerProcessor processor;
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return processor.attachToCurrentRunLoop(); });

    std::array<testing::StrictMock<testing::MockFunction<void()>>, 4> finalizers;

    std::atomic<bool> done = false;
    {
        testing::InSequence seq;
        EXPECT_CALL(finalizers[1], Call()).WillOnce([&] {
            processor.schedule({finalizers[2].AsStdFunction(), finalizers[3].AsStdFunction()}, 2);
        });
        EXPECT_CALL(finalizers[0], Call());
        EXPECT_CALL(finalizers[3], Call());
        EXPECT_CALL(finalizers[2], Call()).WillOnce([&] { done.store(true, std::memory_order_release); });
    }
    processor.schedule({finalizers[0].AsStdFunction(), finalizers[1].AsStdFunction()}, 1);
    runLoop.wakeUp();
    while (!done.load(std::memory_order_acquire)) {
        std::this_thread::yield();
    }
}

TEST(RunLoopFinalizerProcessorTest, Overtime) {
    constexpr std::chrono::nanoseconds overtime = std::chrono::milliseconds(1);
    constexpr std::chrono::nanoseconds timeoutBetween = std::chrono::milliseconds(10);
    RunLoopFinalizerProcessor processor;
    processor.withConfig([&](alloc::RunLoopFinalizerProcessorConfig& config) noexcept {
        config.minTimeBetweenTasks = timeoutBetween;
        config.maxTimeInTask = overtime;
        config.batchSize = 3;
    });
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return processor.attachToCurrentRunLoop(); });

    std::array<testing::StrictMock<testing::MockFunction<void()>>, 4> finalizers;

    std::atomic<bool> done = false;
    steady_clock::time_point sleptAt;
    testing::StrictMock<testing::MockFunction<void()>> checkpoint;
    {
        testing::InSequence seq;
        EXPECT_CALL(finalizers[3], Call()).WillOnce([&] { runLoop.schedule(checkpoint.AsStdFunction()); });
        EXPECT_CALL(finalizers[2], Call()).WillOnce([&] {
            std::this_thread::sleep_for(overtime);
            sleptAt = steady_clock::now();
        });
        EXPECT_CALL(finalizers[1], Call());
        EXPECT_CALL(checkpoint, Call());
        EXPECT_CALL(finalizers[0], Call()).WillOnce([&] {
            EXPECT_GE(steady_clock::now(), sleptAt + timeoutBetween);
            done.store(true, std::memory_order_release);
        });
    }
    processor.schedule(
            {finalizers[0].AsStdFunction(), finalizers[1].AsStdFunction(), finalizers[2].AsStdFunction(), finalizers[3].AsStdFunction()},
            1);
    runLoop.wakeUp();

    while (!done.load(std::memory_order_acquire)) {
        std::this_thread::yield();
    }
}

TEST(RunLoopFinalizerProcessorTest, ScheduleWhileOvertime) {
    constexpr std::chrono::nanoseconds overtime = std::chrono::milliseconds(1);
    constexpr std::chrono::nanoseconds timeoutBetween = std::chrono::milliseconds(10);
    RunLoopFinalizerProcessor processor;
    processor.withConfig([&](alloc::RunLoopFinalizerProcessorConfig& config) noexcept {
        config.minTimeBetweenTasks = timeoutBetween;
        config.maxTimeInTask = overtime;
        config.batchSize = 2;
    });
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() noexcept { return processor.attachToCurrentRunLoop(); });

    std::array<testing::StrictMock<testing::MockFunction<void()>>, 6> finalizers;

    std::atomic<bool> done = false;
    steady_clock::time_point sleptAt;
    testing::StrictMock<testing::MockFunction<void()>> checkpoint;
    {
        testing::InSequence seq;
        EXPECT_CALL(finalizers[3], Call()).WillOnce([&] {
            processor.schedule({finalizers[4].AsStdFunction(), finalizers[5].AsStdFunction()}, 1);
            runLoop.schedule(checkpoint.AsStdFunction());
            std::this_thread::sleep_for(overtime);
            sleptAt = steady_clock::now();
        });
        EXPECT_CALL(finalizers[2], Call());
        EXPECT_CALL(checkpoint, Call());
        EXPECT_CALL(finalizers[1], Call()).WillOnce([&] { EXPECT_GE(steady_clock::now(), sleptAt + timeoutBetween); });
        EXPECT_CALL(finalizers[0], Call());
        EXPECT_CALL(finalizers[5], Call());
        EXPECT_CALL(finalizers[4], Call()).WillOnce([&] { done.store(true, std::memory_order_release); });
    }
    processor.schedule(
            {finalizers[0].AsStdFunction(), finalizers[1].AsStdFunction(), finalizers[2].AsStdFunction(), finalizers[3].AsStdFunction()},
            1);
    runLoop.wakeUp();

    while (!done.load(std::memory_order_acquire)) {
        std::this_thread::yield();
    }
}

#endif