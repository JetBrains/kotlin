/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <future>
#include <mutex>
#include <thread>

#include "Saturating.hpp"

namespace kotlin {

namespace internal {

template <typename NowF, typename StepDuration, typename Clock, typename Rep, typename Period, typename Ret, typename WaitForF>
Ret waitUntilViaFor(
        NowF&& nowF,
        StepDuration step,
        std::chrono::time_point<Clock, std::chrono::duration<saturating<Rep>, Period>> until,
        Ret timeoutValue,
        WaitForF&& waitForF) {
    while (true) {
        auto current = std::invoke(std::forward<NowF>(nowF));
        if (current >= until) {
            return timeoutValue;
        }
        auto left = until - current;
        // Shield standard library from saturating types.
        auto interval = left > step ? std::chrono::duration<Rep, Period>(step) : std::chrono::duration<Rep, Period>(left);
        if (auto value = std::invoke(std::forward<WaitForF>(waitForF), interval); value != timeoutValue) {
            return value;
        }
    }
}

template <typename NowF, typename StepDuration, typename Clock, typename Rep, typename Period, typename WaitForF>
void waitUntilViaFor(
        NowF&& nowF,
        StepDuration step,
        std::chrono::time_point<Clock, std::chrono::duration<saturating<Rep>, Period>> until,
        WaitForF&& waitForF) {
    while (true) {
        auto current = std::invoke(std::forward<NowF>(nowF));
        if (current >= until) {
            return;
        }
        auto left = until - current;
        // Shield standard library from saturating types.
        auto interval = left > step ? std::chrono::duration<Rep, Period>(step) : std::chrono::duration<Rep, Period>(left);
        std::invoke(std::forward<WaitForF>(waitForF), interval);
    }
}

template <typename T, typename Lock>
struct IsStdCV : public std::false_type {};

template <>
struct IsStdCV<std::condition_variable, std::unique_lock<std::mutex>> : public std::true_type {};

template <typename Lock>
struct IsStdCV<std::condition_variable_any, Lock> : public std::true_type {};

template <typename T, typename Lock>
inline constexpr bool isStdCV = IsStdCV<T, Lock>::value;

template <typename T>
struct IsStdFuture : public std::false_type {};

template <typename T>
struct IsStdFuture<std::future<T>> : public std::true_type {};

template <typename T>
struct IsStdFuture<std::shared_future<T>> : public std::true_type {};

template <typename T>
inline constexpr bool isStdFuture = IsStdFuture<T>::value;

template <typename Clock>
class ClockWaitImpl {
public:
    template <typename Rep, typename Period>
    static void sleep_for(std::chrono::duration<Rep, Period> interval) {
        // Not using this_thread::sleep_for, because it may mishandle "infinite" intervals. Use saturating arithmetics to address this.
        return ClockWaitImpl<Clock>::sleep_until(Clock::now() + interval);
    }

    template <typename Rep, typename Period>
    static void sleep_until(std::chrono::time_point<Clock, std::chrono::duration<Rep, Period>> until) {
        if constexpr (is_saturating_v<Rep>) {
            return Clock::sleepImpl(until);
        } else {
            return ClockWaitImpl<Clock>::sleep_until(std::chrono::time_point<Clock, std::chrono::duration<saturating<Rep>, Period>>(until));
        }
    }

    template <typename CV, typename Lock, typename Rep, typename Period, typename F, typename = std::enable_if_t<isStdCV<CV, Lock>>>
    static bool wait_for(CV& cv, Lock& lock, std::chrono::duration<Rep, Period> interval, F&& f) {
        // Not using cv.wait_for, because it may mishandle "infinite" intervals. Use saturating arithmetics to address this.
        return ClockWaitImpl<Clock>::wait_until(cv, lock, Clock::now() + interval, std::forward<F>(f));
    }

    template <typename CV, typename Lock, typename Rep, typename Period, typename F, typename = std::enable_if_t<isStdCV<CV, Lock>>>
    static bool wait_until(CV& cv, Lock& lock, std::chrono::time_point<Clock, std::chrono::duration<Rep, Period>> until, F&& f) {
        if constexpr (is_saturating_v<Rep>) {
            [[maybe_unused]] auto pendingWait = Clock::addPendingWait(until);
            // Implement in terms of repeated cv.wait_for of non-"infinite" intervals.
            return internal::waitUntilViaFor(&Clock::now, Clock::wait_step, until, false, [&](auto interval) {
                return cv.wait_for(lock, interval, std::forward<F>(f));
            });
        } else {
            return ClockWaitImpl<Clock>::wait_until(
                    cv, lock, std::chrono::time_point<Clock, std::chrono::duration<saturating<Rep>, Period>>(until), std::forward<F>(f));
        }
    }

    template <typename Future, typename Rep, typename Period, typename = std::enable_if_t<isStdFuture<Future>>>
    static std::future_status wait_for(const Future& future, std::chrono::duration<Rep, Period> interval) {
        // Not using future.wait_for, because it may mishandle "infinite" intervals. Use saturating arithmetics to address this.
        return ClockWaitImpl<Clock>::wait_until(future, Clock::now() + interval);
    }

    template <typename Future, typename Rep, typename Period, typename = std::enable_if_t<isStdFuture<Future>>>
    static std::future_status wait_until(const Future& future, std::chrono::time_point<Clock, std::chrono::duration<Rep, Period>> until) {
        if constexpr (is_saturating_v<Rep>) {
            [[maybe_unused]] auto pendingWait = Clock::addPendingWait(until);
            // Implement in terms of repeated future.wait_for of non-"infinite" intervals.
            return internal::waitUntilViaFor(&Clock::now, Clock::wait_step, until, std::future_status::timeout, [&](auto interval) {
                return future.wait_for(interval);
            });
        } else {
            return ClockWaitImpl<Clock>::wait_until(
                    future, std::chrono::time_point<Clock, std::chrono::duration<saturating<Rep>, Period>>(until));
        }
    }
};

} // namespace internal

using nanoseconds = std::chrono::duration<saturating<std::chrono::nanoseconds::rep>, std::chrono::nanoseconds::period>;
using microseconds = std::chrono::duration<saturating<std::chrono::microseconds::rep>, std::chrono::microseconds::period>;
using milliseconds = std::chrono::duration<saturating<std::chrono::milliseconds::rep>, std::chrono::milliseconds::period>;
using seconds = std::chrono::duration<saturating<std::chrono::seconds::rep>, std::chrono::seconds::period>;
using minutes = std::chrono::duration<saturating<std::chrono::minutes::rep>, std::chrono::minutes::period>;
using hours = std::chrono::duration<saturating<std::chrono::hours::rep>, std::chrono::hours::period>;

class steady_clock : public internal::ClockWaitImpl<steady_clock> {
public:
    using rep = saturating<std::chrono::steady_clock::rep>;
    using period = std::chrono::steady_clock::period;
    using duration = std::chrono::duration<rep, period>;
    using time_point = std::chrono::time_point<steady_clock>;

    static constexpr bool is_steady = true;

    static time_point now() noexcept {
        auto time = std::chrono::steady_clock::now().time_since_epoch();
        return time_point(time);
    }

private:
    friend class internal::ClockWaitImpl<steady_clock>;

    // Use non-saturating type here, because step may be fed into the standard library.
    static inline constexpr auto wait_step = std::chrono::hours(24);

    template <typename Rep, typename Period>
    static void sleepImpl(std::chrono::time_point<steady_clock, std::chrono::duration<saturating<Rep>, Period>> until) {
        // Implement in terms of repeated this_thread::sleep_for of non-"infinite" intervals.
        return internal::waitUntilViaFor(&now, wait_step, until, [&](auto interval) { std::this_thread::sleep_for(interval); });
    }

    template <typename Rep, typename Period>
    static int addPendingWait(std::chrono::time_point<steady_clock, std::chrono::duration<saturating<Rep>, Period>> until) {
        // No need to register here.
        return 0;
    }
};

} // namespace kotlin
