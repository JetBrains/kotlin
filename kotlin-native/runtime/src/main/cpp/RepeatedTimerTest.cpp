/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RepeatedTimer.hpp"

#include <atomic>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ClockTestSupport.hpp"

using namespace kotlin;

class RepeatedTimerTest : public testing::Test {
public:
    RepeatedTimerTest() { test_support::manual_clock::reset(); }
};

TEST_F(RepeatedTimerTest, WillNotExecuteImmediately) {
    testing::StrictMock<testing::MockFunction<void()>> f;
    RepeatedTimer<test_support::manual_clock> timer(minutes(10), f.AsStdFunction());
}

TEST_F(RepeatedTimerTest, WillRun) {
    testing::StrictMock<testing::MockFunction<void()>> f;
    RepeatedTimer<test_support::manual_clock> timer(minutes(10), f.AsStdFunction());
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));

    EXPECT_CALL(f, Call());
    test_support::manual_clock::sleep_for(minutes(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));
    testing::Mock::VerifyAndClearExpectations(&f);

    EXPECT_CALL(f, Call());
    test_support::manual_clock::sleep_for(minutes(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));
    testing::Mock::VerifyAndClearExpectations(&f);
}

TEST_F(RepeatedTimerTest, WillStopInDestructor) {
    testing::StrictMock<testing::MockFunction<void()>> f;
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    {
        RepeatedTimer<test_support::manual_clock> timer(minutes(10), f.AsStdFunction());
        // Wait until the counter increases once.
        test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));

        EXPECT_CALL(f, Call()).WillOnce([&] { promise.set_value_at_thread_exit(42); });
        test_support::manual_clock::sleep_for(minutes(10));
        test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));
        testing::Mock::VerifyAndClearExpectations(&f);
    }
    future.wait();
    // The thread has definitely finished.
    EXPECT_THAT(future.get(), 42);
    // Timer is gone, so nothing can be pending.
    EXPECT_THAT(test_support::manual_clock::pending(), std::nullopt);
}

TEST_F(RepeatedTimerTest, InfiniteInterval) {
    test_support::manual_clock::reset(test_support::manual_clock::time_point::max() - hours(365 * 24));

    constexpr auto infinite = test_support::manual_clock::duration::max();
    testing::StrictMock<testing::MockFunction<void()>> f;
    RepeatedTimer<test_support::manual_clock> timer(infinite, f.AsStdFunction());
    // test_support::manual_clock will wait the absolute maximum time.
    test_support::manual_clock::waitForPending(test_support::manual_clock::time_point::max());
}

TEST_F(RepeatedTimerTest, Restart) {
    test_support::manual_clock::reset(test_support::manual_clock::time_point::max() - hours(365 * 24));

    testing::StrictMock<testing::MockFunction<void()>> f;
    RepeatedTimer<test_support::manual_clock> timer(minutes(10), f.AsStdFunction());
    // Wait until the clock starts waiting.
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));

    // Now restart the timer to fire in 10 seconds instead.
    timer.restart(seconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + seconds(10));

    // Now wait until task triggers once and is scheduled again with the same interval.
    EXPECT_CALL(f, Call());
    test_support::manual_clock::sleep_for(seconds(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + seconds(10));
    testing::Mock::VerifyAndClearExpectations(&f);

    // Wait 5 seconds and restart the timer to fire in 1 minute time.
    test_support::manual_clock::sleep_for(seconds(5));
    timer.restart(minutes(1));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(1));

    // And immediately restart the timer again to run in 5 minutes instead.
    timer.restart(minutes(5));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(5));

    // And just restart the timer to fire after "infinite" interval.
    timer.restart(test_support::manual_clock::duration::max());
    test_support::manual_clock::waitForPending(test_support::manual_clock::time_point::max());

    // Wait for an hour and restart the timer to fire every 10 minutes.
    test_support::manual_clock::sleep_for(hours(1));
    timer.restart(minutes(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));

    // Wait for 5 minutes, and restart the timer keeping the same interval.
    test_support::manual_clock::sleep_for(minutes(5));
    timer.restart(minutes(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));
}

TEST_F(RepeatedTimerTest, RestartFromTask) {
    testing::StrictMock<testing::MockFunction<void()>> f;
    RepeatedTimer<test_support::manual_clock> timer(minutes(10), f.AsStdFunction());

    // Wait until the clock starts waiting.
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(10));

    // Make the task execute once and restart the task to fire every minute.
    EXPECT_CALL(f, Call()).WillOnce([&] { timer.restart(minutes(1)); });
    test_support::manual_clock::sleep_for(minutes(10));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(1));
    testing::Mock::VerifyAndClear(&f);

    // Now wait for that minute and make sure the next task is also scheduled 1 minute from now.
    EXPECT_CALL(f, Call());
    test_support::manual_clock::sleep_for(minutes(1));
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + minutes(1));
}
