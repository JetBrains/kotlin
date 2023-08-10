/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Clock.hpp"

#include <atomic>
#include <condition_variable>
#include <mutex>
#include <shared_mutex>
#include <tuple>
#include <type_traits>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ClockTestSupport.hpp"
#include "ScopedThread.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

TEST(ClockInternalTest, WaitUntilViaFor_Int_ImmediateOK) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    constexpr int okValue = 13;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(okValue));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, okValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Int_EventualOK) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    constexpr int okValue = 13;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(okValue));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, okValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Int_LastChanceOK) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    constexpr int okValue = 13;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step));
        EXPECT_CALL(waitForF, Call(rest)).WillOnce(testing::Return(okValue));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, okValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Int_Timeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step));
        EXPECT_CALL(waitForF, Call(rest)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step + rest));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, timeoutValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Int_ImmediateTimeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step + step));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, timeoutValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Int_ClockJumpTimeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        // Instead of incrementing by `step`, the clock jumped straight to `until`.
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(until));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, timeoutValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Int_NonconformantWaitTimeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    // waitFor non-conformingly waits less than specified.
    constexpr auto actualStep = std::chrono::seconds(7);
    constexpr auto rest = std::chrono::seconds(3);
    constexpr auto until = TimePoint() + step + step + rest;
    constexpr int timeoutValue = 42;
    testing::StrictMock<testing::MockFunction<int(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep));
        EXPECT_CALL(waitForF, Call(step)).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep + actualStep));
        // Waited 2 * 7 out of 2 * 10 + 3 seconds. 9 seconds left. Will wait only 6 seconds.
        EXPECT_CALL(waitForF, Call(std::chrono::seconds(9))).WillOnce(testing::Return(timeoutValue));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep + actualStep + std::chrono::seconds(6)));
        EXPECT_CALL(waitForF, Call(std::chrono::seconds(3))).WillOnce(testing::Return(timeoutValue));
        // Finally waited enough.
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep + actualStep + std::chrono::seconds(9)));
    }
    auto result = internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, timeoutValue, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
    EXPECT_THAT(result, timeoutValue);
}

TEST(ClockInternalTest, WaitUntilViaFor_Void_Timeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    testing::StrictMock<testing::MockFunction<void(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step));
        EXPECT_CALL(waitForF, Call(step));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step));
        EXPECT_CALL(waitForF, Call(rest));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step + rest));
    }
    internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
}

TEST(ClockInternalTest, WaitUntilViaFor_Void_ImmediateTimeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    testing::StrictMock<testing::MockFunction<void(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + step + step + step));
    }
    internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
}

TEST(ClockInternalTest, WaitUntilViaFor_Void_ClockJumpTimeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    constexpr auto rest = std::chrono::seconds(1);
    constexpr auto until = TimePoint() + step + step + rest;
    testing::StrictMock<testing::MockFunction<void(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step));
        // Instead of incrementing by `step`, the clock jumped straight to `until`.
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(until));
    }
    internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
}

TEST(ClockInternalTest, WaitUntilViaFor_Void_NonconformantWaitTimeout) {
    using TimePoint = std::chrono::time_point<test_support::manual_clock>;
    testing::StrictMock<testing::MockFunction<TimePoint()>> nowF;
    constexpr auto step = std::chrono::seconds(10);
    // waitFor non-conformingly waits less than specified.
    constexpr auto actualStep = std::chrono::seconds(7);
    constexpr auto rest = std::chrono::seconds(3);
    constexpr auto until = TimePoint() + step + step + rest;
    testing::StrictMock<testing::MockFunction<void(std::chrono::seconds)>> waitForF;

    {
        testing::InSequence s;
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint()));
        EXPECT_CALL(waitForF, Call(step));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep));
        EXPECT_CALL(waitForF, Call(step));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep + actualStep));
        // Waited 2 * 7 out of 2 * 10 + 3 seconds. 9 seconds left. Will wait only 6 seconds.
        EXPECT_CALL(waitForF, Call(std::chrono::seconds(9)));
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep + actualStep + std::chrono::seconds(6)));
        EXPECT_CALL(waitForF, Call(std::chrono::seconds(3)));
        // Finally waited enough.
        EXPECT_CALL(nowF, Call()).WillOnce(testing::Return(TimePoint() + actualStep + actualStep + std::chrono::seconds(9)));
    }
    internal::waitUntilViaFor(nowF.AsStdFunction(), step, until, [&](auto interval) {
        return waitForF.Call(std::chrono::duration_cast<std::chrono::seconds>(interval));
    });
}

namespace {

class ClockTestNames {
public:
    template <typename T>
    static std::string GetName(int) {
        if constexpr (std::is_same_v<T, kotlin::steady_clock>) {
            return "steady_clock";
        } else if constexpr (std::is_same_v<T, kotlin::test_support::manual_clock>) {
            return "manual_clock";
        } else {
            return "unknown";
        }
    }
};

template <typename Clock>
struct WaitForPendingImpl {
    void operator()() {}
};

template <>
struct WaitForPendingImpl<test_support::manual_clock> {
    void operator()() {
        while (!test_support::manual_clock::pending()) {
        }
    }
};

template <typename Clock>
void waitForPending() {
    WaitForPendingImpl<Clock>()();
}

} // namespace

template <typename T>
class ClockTest : public testing::Test {
public:
    ClockTest() noexcept { test_support::manual_clock::reset(); }
};

using ClockTestTypes = testing::Types<kotlin::steady_clock, kotlin::test_support::manual_clock>;
TYPED_TEST_SUITE(ClockTest, ClockTestTypes, ClockTestNames);

TYPED_TEST(ClockTest, SleepFor) {
    constexpr auto interval = milliseconds(1);
    auto before = TypeParam::now();
    TypeParam::sleep_for(interval);
    auto after = TypeParam::now();
    EXPECT_THAT(after - before, testing::Ge(interval));
}

TYPED_TEST(ClockTest, SleepUntil) {
    auto until = TypeParam::now() + milliseconds(1);
    TypeParam::sleep_until(until);
    auto after = TypeParam::now();
    EXPECT_THAT(after, testing::Ge(until));
}

TYPED_TEST(ClockTest, CVWaitFor_OK) {
    constexpr auto interval = hours(10);
    std::condition_variable cv;
    std::mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(cv, guard, interval, [&] { return ok; });
    auto after = TypeParam::now();
    EXPECT_TRUE(result);
    EXPECT_THAT(after - before, testing::Lt(interval));
}

TYPED_TEST(ClockTest, CVWaitFor_Timeout) {
    constexpr auto interval = microseconds(10);
    std::condition_variable cv;
    std::mutex m;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    std::unique_lock guard(m);
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(cv, guard, interval, [] { return false; });
    auto after = TypeParam::now();
    EXPECT_FALSE(result);
    EXPECT_THAT(after - before, testing::Ge(interval));
}

TYPED_TEST(ClockTest, CVWaitFor_InfiniteTimeout) {
    std::condition_variable cv;
    std::mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_for` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    go = true;
    auto result = TypeParam::wait_for(cv, guard, microseconds::max(), [&] { return ok; });
    EXPECT_TRUE(result);
}

TYPED_TEST(ClockTest, CVWaitUntil_OK) {
    std::condition_variable cv;
    std::mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    auto until = TypeParam::now() + hours(10);
    go = true;
    auto result = TypeParam::wait_until(cv, guard, until, [&] { return ok; });
    auto after = TypeParam::now();
    EXPECT_TRUE(result);
    EXPECT_THAT(after, testing::Lt(until));
}

TYPED_TEST(ClockTest, CVWaitUntil_Timeout) {
    constexpr auto interval = microseconds(10);
    std::condition_variable cv;
    std::mutex m;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    std::unique_lock guard(m);
    auto until = TypeParam::now() + interval;
    go = true;
    auto result = TypeParam::wait_until(cv, guard, until, [] { return false; });
    auto after = TypeParam::now();
    EXPECT_FALSE(result);
    EXPECT_THAT(after, testing::Ge(until));
}

TYPED_TEST(ClockTest, CVWaitUntil_InfiniteTimeout) {
    std::condition_variable cv;
    std::mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_until` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    go = true;
    auto result = TypeParam::wait_until(cv, guard, TypeParam::time_point::max(), [&] { return ok; });
    EXPECT_TRUE(result);
}

TYPED_TEST(ClockTest, CVAnyWaitFor_OK) {
    constexpr auto interval = hours(10);
    std::condition_variable_any cv;
    std::shared_mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(cv, guard, interval, [&] { return ok; });
    auto after = TypeParam::now();
    EXPECT_TRUE(result);
    EXPECT_THAT(after - before, testing::Lt(interval));
}

TYPED_TEST(ClockTest, CVAnyWaitFor_Timeout) {
    constexpr auto interval = microseconds(10);
    std::condition_variable_any cv;
    std::shared_mutex m;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    std::unique_lock guard(m);
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(cv, guard, interval, [] { return false; });
    auto after = TypeParam::now();
    EXPECT_FALSE(result);
    EXPECT_THAT(after - before, testing::Ge(interval));
}

TYPED_TEST(ClockTest, CVAnyWaitFor_InfiniteTimeout) {
    std::condition_variable_any cv;
    std::shared_mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_for` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    go = true;
    auto result = TypeParam::wait_for(cv, guard, microseconds::max(), [&] { return ok; });
    EXPECT_TRUE(result);
}

TYPED_TEST(ClockTest, CVAnyWaitUntil_OK) {
    std::condition_variable_any cv;
    std::shared_mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    auto until = TypeParam::now() + hours(10);
    go = true;
    auto result = TypeParam::wait_until(cv, guard, until, [&] { return ok; });
    auto after = TypeParam::now();
    EXPECT_TRUE(result);
    EXPECT_THAT(after, testing::Lt(until));
}

TYPED_TEST(ClockTest, CVAnyWaitUntil_Timeout) {
    constexpr auto interval = microseconds(10);
    std::condition_variable_any cv;
    std::shared_mutex m;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    std::unique_lock guard(m);
    auto until = TypeParam::now() + interval;
    go = true;
    auto result = TypeParam::wait_until(cv, guard, until, [] { return false; });
    auto after = TypeParam::now();
    EXPECT_FALSE(result);
    EXPECT_THAT(after, testing::Ge(until));
}

TYPED_TEST(ClockTest, CVAnyWaitUntil_InfiniteTimeout) {
    std::condition_variable_any cv;
    std::shared_mutex m;
    bool ok = false;
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_until` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        {
            std::unique_lock guard(m);
            ok = true;
        }
        cv.notify_all();
    });
    std::unique_lock guard(m);
    go = true;
    auto result = TypeParam::wait_until(cv, guard, TypeParam::time_point::max(), [&] { return ok; });
    EXPECT_TRUE(result);
}

TYPED_TEST(ClockTest, FutureWaitFor_OK) {
    constexpr auto interval = hours(10);
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        promise.set_value(42);
    });
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(future, interval);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::ready);
    EXPECT_THAT(after - before, testing::Lt(interval));
}

TYPED_TEST(ClockTest, FutureWaitFor_Deferred) {
    constexpr auto interval = hours(10);
    std::future<int> future = std::async(std::launch::deferred, [] { return 42; });
    auto before = TypeParam::now();
    auto result = TypeParam::wait_for(future, interval);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::deferred);
    EXPECT_THAT(after - before, testing::Lt(interval));
}

TYPED_TEST(ClockTest, FutureWaitFor_Timeout) {
    constexpr auto interval = microseconds(10);
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(future, interval);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::timeout);
    EXPECT_THAT(after - before, testing::Ge(interval));
}

TYPED_TEST(ClockTest, FutureWaitFor_InfiniteTimeout) {
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_for` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        promise.set_value(42);
    });
    go = true;
    auto result = TypeParam::wait_for(future, microseconds::max());
    EXPECT_THAT(result, std::future_status::ready);
}

TYPED_TEST(ClockTest, FutureWaitUntil_OK) {
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        promise.set_value(42);
    });
    auto until = TypeParam::now() + hours(10);
    go = true;
    auto result = TypeParam::wait_until(future, until);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::ready);
    EXPECT_THAT(after, testing::Lt(until));
}

TYPED_TEST(ClockTest, FutureWaitUntil_Deferred) {
    std::future<int> future = std::async(std::launch::deferred, [] { return 42; });
    auto until = TypeParam::now() + hours(10);
    auto result = TypeParam::wait_until(future, until);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::deferred);
    EXPECT_THAT(after, testing::Lt(until));
}

TYPED_TEST(ClockTest, FutureWaitUntil_Timeout) {
    constexpr auto interval = microseconds(10);
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    auto until = TypeParam::now() + interval;
    go = true;
    auto result = TypeParam::wait_until(future, until);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::timeout);
    EXPECT_THAT(after, testing::Ge(until));
}

TYPED_TEST(ClockTest, FutureWaitUntil_InfiniteTimeout) {
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_until` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        promise.set_value(42);
    });
    go = true;
    auto result = TypeParam::wait_until(future, TypeParam::time_point::max());
    EXPECT_THAT(result, std::future_status::ready);
}

TYPED_TEST(ClockTest, SharedFutureWaitFor_OK) {
    constexpr auto interval = hours(10);
    std::promise<int> promise;
    std::shared_future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        promise.set_value(42);
    });
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(future, interval);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::ready);
    EXPECT_THAT(after - before, testing::Lt(interval));
}

TYPED_TEST(ClockTest, SharedFutureWaitFor_Deferred) {
    constexpr auto interval = hours(10);
    std::shared_future<int> future = std::async(std::launch::deferred, [] { return 42; });
    auto before = TypeParam::now();
    auto result = TypeParam::wait_for(future, interval);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::deferred);
    EXPECT_THAT(after - before, testing::Lt(interval));
}

TYPED_TEST(ClockTest, SharedFutureWaitFor_Timeout) {
    constexpr auto interval = microseconds(10);
    std::promise<int> promise;
    std::shared_future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    auto before = TypeParam::now();
    go = true;
    auto result = TypeParam::wait_for(future, interval);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::timeout);
    EXPECT_THAT(after - before, testing::Ge(interval));
}

TYPED_TEST(ClockTest, SharedFutureWaitFor_InfiniteTimeout) {
    std::promise<int> promise;
    std::shared_future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_for` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        promise.set_value(42);
    });
    go = true;
    auto result = TypeParam::wait_for(future, microseconds::max());
    EXPECT_THAT(result, std::future_status::ready);
}

TYPED_TEST(ClockTest, SharedFutureWaitUntil_OK) {
    std::promise<int> promise;
    std::shared_future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        promise.set_value(42);
    });
    auto until = TypeParam::now() + hours(10);
    go = true;
    auto result = TypeParam::wait_until(future, until);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::ready);
    EXPECT_THAT(after, testing::Lt(until));
}

TYPED_TEST(ClockTest, SharedFutureWaitUntil_Deferred) {
    std::shared_future<int> future = std::async(std::launch::deferred, [] { return 42; });
    auto until = TypeParam::now() + hours(10);
    auto result = TypeParam::wait_until(future, until);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::deferred);
    EXPECT_THAT(after, testing::Lt(until));
}

TYPED_TEST(ClockTest, SharedFutureWaitUntil_Timeout) {
    constexpr auto interval = microseconds(10);
    std::promise<int> promise;
    std::shared_future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        TypeParam::sleep_for(interval);
    });
    auto until = TypeParam::now() + interval;
    go = true;
    auto result = TypeParam::wait_until(future, until);
    auto after = TypeParam::now();
    EXPECT_THAT(result, std::future_status::timeout);
    EXPECT_THAT(after, testing::Ge(until));
}

TYPED_TEST(ClockTest, SharedFutureWaitUntil_InfiniteTimeout) {
    std::promise<int> promise;
    std::shared_future<int> future = promise.get_future();
    std::atomic<bool> go = false;
    ScopedThread thread([&] {
        while (!go.load()) {
        }
        waitForPending<TypeParam>();
        // Wait to see if `TypeParam::wait_until` wakes up from timeout.
        TypeParam::sleep_for(milliseconds(1));
        promise.set_value(42);
    });
    go = true;
    auto result = TypeParam::wait_until(future, TypeParam::time_point::max());
    EXPECT_THAT(result, std::future_status::ready);
}

namespace {

class ClockTypesTestNames {
public:
    template <typename T>
    static std::string GetName(int) {
        std::string result;
        result += clockName<T>();
        result += "_";
        result += durationName<T>();
        return result;
    }

private:
    template <typename T>
    static const char* clockName() {
        using Clock = typename std::tuple_element<0, T>::type;
        if constexpr (std::is_same_v<Clock, kotlin::steady_clock>) {
            return "steady_clock";
        } else if constexpr (std::is_same_v<Clock, kotlin::test_support::manual_clock>) {
            return "manual_clock";
        } else {
            return "unknown";
        }
    }

    template <typename T>
    static const char* durationName() {
        using Duration = typename std::tuple_element<1, T>::type;
        if constexpr (std::is_same_v<Duration, std::chrono::nanoseconds>) {
            return "ns";
        } else if constexpr (std::is_same_v<Duration, std::chrono::microseconds>) {
            return "us";
        } else if constexpr (std::is_same_v<Duration, std::chrono::milliseconds>) {
            return "ms";
        } else if constexpr (std::is_same_v<Duration, std::chrono::seconds>) {
            return "s";
        } else if constexpr (std::is_same_v<Duration, std::chrono::minutes>) {
            return "m";
        } else if constexpr (std::is_same_v<Duration, std::chrono::hours>) {
            return "h";
        } else if constexpr (std::is_same_v<Duration, kotlin::nanoseconds>) {
            return "sat_ns";
        } else if constexpr (std::is_same_v<Duration, kotlin::microseconds>) {
            return "sat_us";
        } else if constexpr (std::is_same_v<Duration, kotlin::milliseconds>) {
            return "sat_ms";
        } else if constexpr (std::is_same_v<Duration, kotlin::seconds>) {
            return "sat_s";
        } else if constexpr (std::is_same_v<Duration, kotlin::minutes>) {
            return "sat_m";
        } else if constexpr (std::is_same_v<Duration, kotlin::hours>) {
            return "sat_h";
        } else {
            return "unknown";
        }
    }
};

} // namespace

template <typename T>
class ClockTypesTest : public testing::Test {
public:
    using Clock = typename std::tuple_element<0, T>::type;
    using Duration = typename std::tuple_element<1, T>::type;
};

using ClockTypesTestTypes = testing::Types<
        std::tuple<kotlin::steady_clock, std::chrono::nanoseconds>,
        std::tuple<kotlin::steady_clock, std::chrono::microseconds>,
        std::tuple<kotlin::steady_clock, std::chrono::milliseconds>,
        std::tuple<kotlin::steady_clock, std::chrono::seconds>,
        std::tuple<kotlin::steady_clock, std::chrono::minutes>,
        std::tuple<kotlin::steady_clock, std::chrono::hours>,
        std::tuple<kotlin::steady_clock, kotlin::nanoseconds>,
        std::tuple<kotlin::steady_clock, kotlin::microseconds>,
        std::tuple<kotlin::steady_clock, kotlin::milliseconds>,
        std::tuple<kotlin::steady_clock, kotlin::seconds>,
        std::tuple<kotlin::steady_clock, kotlin::minutes>,
        std::tuple<kotlin::steady_clock, kotlin::hours>,
        std::tuple<kotlin::test_support::manual_clock, std::chrono::nanoseconds>,
        std::tuple<kotlin::test_support::manual_clock, std::chrono::microseconds>,
        std::tuple<kotlin::test_support::manual_clock, std::chrono::milliseconds>,
        std::tuple<kotlin::test_support::manual_clock, std::chrono::seconds>,
        std::tuple<kotlin::test_support::manual_clock, std::chrono::minutes>,
        std::tuple<kotlin::test_support::manual_clock, std::chrono::hours>,
        std::tuple<kotlin::test_support::manual_clock, kotlin::nanoseconds>,
        std::tuple<kotlin::test_support::manual_clock, kotlin::microseconds>,
        std::tuple<kotlin::test_support::manual_clock, kotlin::milliseconds>,
        std::tuple<kotlin::test_support::manual_clock, kotlin::seconds>,
        std::tuple<kotlin::test_support::manual_clock, kotlin::minutes>,
        std::tuple<kotlin::test_support::manual_clock, kotlin::hours>>;
TYPED_TEST_SUITE(ClockTypesTest, ClockTypesTestTypes, ClockTypesTestNames);

TYPED_TEST(ClockTypesTest, SleepFor) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<void, decltype(Clock::sleep_for(std::declval<Duration>()))>);
}

TYPED_TEST(ClockTypesTest, SleepUntil) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<void, decltype(Clock::sleep_until(std::declval<std::chrono::time_point<Clock, Duration>>()))>);
}

TYPED_TEST(ClockTypesTest, CVWaitFor) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  bool,
                  decltype(Clock::wait_for(
                          std::declval<std::condition_variable&>(), std::declval<std::unique_lock<std::mutex>&>(), std::declval<Duration>(),
                          std::declval<std::function<bool()>>()))>);
}

TYPED_TEST(ClockTypesTest, CVWaitUntil) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  bool,
                  decltype(Clock::wait_until(
                          std::declval<std::condition_variable&>(), std::declval<std::unique_lock<std::mutex>&>(),
                          std::declval<std::chrono::time_point<Clock, Duration>>(), std::declval<std::function<bool()>>()))>);
}

TYPED_TEST(ClockTypesTest, CVAnyWaitFor) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  bool,
                  decltype(Clock::wait_for(
                          std::declval<std::condition_variable_any&>(), std::declval<std::unique_lock<std::shared_mutex>&>(),
                          std::declval<Duration>(), std::declval<std::function<bool()>>()))>);
}

TYPED_TEST(ClockTypesTest, CVAnyWaitUntil) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  bool,
                  decltype(Clock::wait_until(
                          std::declval<std::condition_variable_any&>(), std::declval<std::unique_lock<std::shared_mutex>&>(),
                          std::declval<std::chrono::time_point<Clock, Duration>>(), std::declval<std::function<bool()>>()))>);
}

TYPED_TEST(ClockTypesTest, FutureWaitFor) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  std::future_status, decltype(Clock::wait_for(std::declval<const std::future<int>&>(), std::declval<Duration>()))>);
}

TYPED_TEST(ClockTypesTest, FutureWaitUntil) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  std::future_status,
                  decltype(Clock::wait_until(
                          std::declval<const std::future<int>&>(), std::declval<std::chrono::time_point<Clock, Duration>>()))>);
}

TYPED_TEST(ClockTypesTest, SharedFutureWaitFor) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  std::future_status, decltype(Clock::wait_for(std::declval<const std::shared_future<int>&>(), std::declval<Duration>()))>);
}

TYPED_TEST(ClockTypesTest, SharedFutureWaitUntil) {
    using Clock = typename ClockTypesTest<TypeParam>::Clock;
    using Duration = typename ClockTypesTest<TypeParam>::Duration;
    static_assert(std::is_same_v<
                  std::future_status,
                  decltype(Clock::wait_until(
                          std::declval<const std::shared_future<int>&>(), std::declval<std::chrono::time_point<Clock, Duration>>()))>);
}

TEST(ManualClockTest, SleepUntil) {
    test_support::manual_clock::reset();

    auto before = test_support::manual_clock::now();
    test_support::manual_clock::sleep_until(before + seconds(2));
    EXPECT_THAT(test_support::manual_clock::now() - before, seconds(2));
    // Sleep until current time.
    test_support::manual_clock::sleep_until(before + seconds(2));
    EXPECT_THAT(test_support::manual_clock::now() - before, seconds(2));
    // Sleep until moment in the past.
    test_support::manual_clock::sleep_until(before);
    EXPECT_THAT(test_support::manual_clock::now() - before, seconds(2));
}

TEST(ManualClockTest, Pending) {
    test_support::manual_clock::reset();

    // Nothing pending at start.
    EXPECT_THAT(test_support::manual_clock::pending(), std::nullopt);
    std::promise<int> promise;
    std::future<int> future = promise.get_future();
    ScopedThread thread([&] { test_support::manual_clock::wait_for(future, seconds(1)); });
    test_support::manual_clock::waitForPending(test_support::manual_clock::now() + seconds(1));

    // Unblocks the thread.
    test_support::manual_clock::sleep_for(seconds(1));
    thread.join();

    // Nothing pending anymore.
    EXPECT_THAT(test_support::manual_clock::pending(), std::nullopt);
}

TEST(ManualClockTest, ConcurrentSleepUntil) {
    test_support::manual_clock::reset();

    constexpr auto threadCount = kDefaultThreadCount;
    std::vector<ScopedThread> threads;
    std::atomic<bool> run = false;
    std::atomic<int> ready = 0;
    for (int i = 0; i < threadCount; ++i) {
        threads.emplace_back([&, i] {
            auto now = test_support::manual_clock::now();
            ++ready;
            while (!run.load()) {
            }
            test_support::manual_clock::sleep_until(now + seconds(i));
        });
    }
    auto before = test_support::manual_clock::now();
    while (ready.load() < threadCount) {
    }
    run = true;
    threads.clear();
    auto after = test_support::manual_clock::now();
    EXPECT_THAT(after - before, seconds(threadCount - 1));
}

TEST(ManualClockTest, ConcurrentWaits) {
    test_support::manual_clock::reset();

    constexpr auto threadCount = kDefaultThreadCount;
    std::vector<ScopedThread> threads;
    std::mutex mutex;
    std::condition_variable cv;
    std::condition_variable_any cvAny;
    std::promise<int> promise1;
    std::promise<int> promise2;
    std::future<int> future = promise1.get_future();
    std::shared_future<int> shared_future = promise2.get_future().share();
    std::atomic<bool> run = false;
    std::atomic<int> ready = 0;
    for (int i = 0; i < threadCount; ++i) {
        threads.emplace_back([&, i] {
            auto now = test_support::manual_clock::now();
            ++ready;
            while (!run.load()) {
            }
            switch (i % 4) {
                case 0: {
                    std::unique_lock guard(mutex);
                    test_support::manual_clock::wait_until(cv, guard, now + seconds(i / 3 + 1), [] { return false; });
                }
                case 1: {
                    std::unique_lock guard(mutex);
                    test_support::manual_clock::wait_until(cvAny, guard, now + seconds(i / 3 + 1), [] { return false; });
                }
                case 2: {
                    test_support::manual_clock::wait_until(future, now + seconds(i / 3 + 1));
                }
                case 3: {
                    test_support::manual_clock::wait_until(shared_future, now + seconds(i / 3 + 1));
                }
            }
        });
    }
    auto before = test_support::manual_clock::now();
    while (ready.load() < threadCount) {
    }
    run = true;

    test_support::manual_clock::sleep_until(before + seconds(1));
    // Now the first 3 threads will be unblocked.
    threads[0].join();
    threads[1].join();
    threads[2].join();
    // Make sure at least one other thread is waiting.
    while (!test_support::manual_clock::pending()) {
    }
    auto pendingAfterSecond = *test_support::manual_clock::pending();
    EXPECT_THAT(pendingAfterSecond, testing::Ge(before + seconds(2)));

    // Unblock all the threads.
    test_support::manual_clock::sleep_until(before + seconds((threadCount - 1) / 3 + 1));
    threads.clear();
    // All threads are gone, nothing can possibly be pending.
    EXPECT_THAT(test_support::manual_clock::pending(), std::nullopt);
}
