/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "RunLoopTimer.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "RunLoopTestSupport.hpp"
#include "Clock.hpp"

using namespace kotlin;

TEST(RunLoopTimerTest, MainThread) {
    ASSERT_EQ(CFRunLoopGetMain(), CFRunLoopGetCurrent());
    testing::StrictMock<testing::MockFunction<void()>> callback;
    constexpr auto initialTimeout = std::chrono::milliseconds(50);
    constexpr auto interval = std::chrono::milliseconds(10);
    steady_clock::time_point start = steady_clock::now();
    objc_support::RunLoopTimer timer(callback.AsStdFunction(), interval, objc_support::cf_clock::now() + initialTimeout);
    auto subscription = timer.attachToCurrentRunLoop();
    {
        testing::InSequence seq;
        EXPECT_CALL(callback, Call()).WillOnce([&] {
            auto callbackTime = steady_clock::now();
            EXPECT_GE(callbackTime, start + initialTimeout);
        });
        EXPECT_CALL(callback, Call()).WillOnce([&] {
            auto callbackTime = steady_clock::now();
            EXPECT_GE(callbackTime, start + initialTimeout + interval);
        });
        EXPECT_CALL(callback, Call()).WillOnce([&] {
            auto callbackTime = steady_clock::now();
            EXPECT_GE(callbackTime, start + initialTimeout + interval + interval);
            CFRunLoopStop(CFRunLoopGetMain());
        });
    }
    CFRunLoopRun();
}

TEST(RunLoopTimerTest, RepeatedTimer) {
    testing::StrictMock<testing::MockFunction<void()>> callback;
    objc_support::RunLoopTimer timer(
            callback.AsStdFunction(), std::chrono::microseconds(10), objc_support::cf_clock::now() + std::chrono::seconds(0));
    std::atomic<int> counter = 0;
    EXPECT_CALL(callback, Call()).WillRepeatedly([&] { counter.fetch_add(1, std::memory_order_relaxed); });
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() { return timer.attachToCurrentRunLoop(); });
    while (counter.load(std::memory_order_relaxed) < 2) {
        std::this_thread::yield();
    }
}

TEST(RunLoopTimerTest, EffectivelyOneShotTimer) {
    testing::StrictMock<testing::MockFunction<void()>> callback;
    objc_support::RunLoopTimer timer(
            callback.AsStdFunction(), std::chrono::hours(10), objc_support::cf_clock::now() + std::chrono::microseconds(10));
    std::atomic<int> counter = 0;
    EXPECT_CALL(callback, Call()).WillOnce([&] { counter.fetch_add(1, std::memory_order_relaxed); });
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() { return timer.attachToCurrentRunLoop(); });
    while (counter.load(std::memory_order_relaxed) < 1) {
        std::this_thread::yield();
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
}

TEST(RunLoopTimerTest, EffectivelyNeverTimer) {
    testing::StrictMock<testing::MockFunction<void()>> callback;
    objc_support::RunLoopTimer timer(
            callback.AsStdFunction(), std::chrono::hours(10), objc_support::cf_clock::now() + std::chrono::hours(10));
    EXPECT_CALL(callback, Call()).Times(0);
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() { return timer.attachToCurrentRunLoop(); });
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
}

TEST(RunLoopTimerTest, RescheduleTimer) {
    testing::StrictMock<testing::MockFunction<void()>> callback;
    objc_support::RunLoopTimer timer(
            callback.AsStdFunction(), std::chrono::hours(10), objc_support::cf_clock::now() + std::chrono::hours(10));
    objc_support::test_support::RunLoopInScopedThread runLoop([&]() { return timer.attachToCurrentRunLoop(); });
    std::atomic<int> counter = 0;
    EXPECT_CALL(callback, Call()).WillOnce([&] { counter.fetch_add(1, std::memory_order_relaxed); });
    timer.setNextFiring(std::chrono::microseconds(10));
    while (counter.load(std::memory_order_relaxed) < 1) {
        std::this_thread::yield();
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
}

#endif