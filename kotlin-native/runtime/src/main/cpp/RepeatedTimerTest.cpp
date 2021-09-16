/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RepeatedTimer.hpp"

#include <atomic>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

TEST(RepeatedTimerTest, WillRun) {
    std::atomic<int> counter = 0;
    RepeatedTimer timer(std::chrono::milliseconds(10), [&counter]() {
        ++counter;
        return std::chrono::milliseconds(10);
    });
    // The function is not executed immediately.
    EXPECT_THAT(counter.load(), 0);
    // And now wait until the counter increases at least twice
    while (counter < 2) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

TEST(RepeatedTimerTest, WillStop) {
    std::atomic<int> counter = 0;
    {
        RepeatedTimer timer(std::chrono::minutes(1), [&counter]() {
            ++counter;
            return std::chrono::minutes(10);
        });
    }
    // The function was never executed.
    EXPECT_THAT(counter.load(), 0);
}

TEST(RepeatedTimerTest, AdjustInterval) {
    std::atomic<int> counter = 0;
    RepeatedTimer timer(std::chrono::milliseconds(1), [&counter]() {
        ++counter;
        if (counter < 2) {
            return std::chrono::milliseconds(1);
        } else {
            return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::minutes(10));
        }
    });
    // Wait until counter grows to 2, when the waiting time changes to 10 minutes.
    while (counter < 2) {
    }
    EXPECT_THAT(counter.load(), 2);
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    // After we've slept for 10ms, we still haven't executed the function another time.
    EXPECT_THAT(counter.load(), 2);
}
