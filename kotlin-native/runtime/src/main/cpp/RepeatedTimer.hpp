/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifndef KONAN_NO_THREADS

#include <chrono>
#include <condition_variable>
#include <mutex>
#include <string_view>

#include "KAssert.h"
#include "ScopedThread.hpp"
#include "Utils.hpp"

namespace kotlin {

class RepeatedTimer : private Pinned {
public:
    template <typename Rep, typename Period, typename F>
    RepeatedTimer(std::string_view name, std::chrono::duration<Rep, Period> interval, F&& f) noexcept :
        thread_(ScopedThread::attributes().name(name), &RepeatedTimer::Run<Rep, Period, F>, this, std::move(interval), std::forward<F>(f)) {
    }

    template <typename Rep, typename Period, typename F>
    RepeatedTimer(std::chrono::duration<Rep, Period> interval, F&& f) noexcept :
        RepeatedTimer("Timer thread", interval, std::forward<F>(f)) {}

    ~RepeatedTimer() {
        {
            std::unique_lock lock(mutex_);
            run_ = false;
        }
        wait_.notify_all();
    }

private:
    template <typename Rep, typename Period, typename F>
    void Run(std::chrono::duration<Rep, Period> interval, F f) noexcept {
        while (true) {
            std::unique_lock lock(mutex_);
            if (wait_.wait_for(lock, interval, [this]() noexcept { return !run_; })) {
                RuntimeAssert(!run_, "Can only happen once run_ is set to false");
                return;
            }
            RuntimeAssert(run_, "Can only happen if we timed out on waiting and run_ is still true");
            auto newInterval = f();
            // The next waiting will use the new interval.
            interval = std::chrono::duration_cast<std::chrono::duration<Rep, Period>>(newInterval);
        }
    }

    std::mutex mutex_;
    std::condition_variable wait_;
    bool run_ = true;
    ScopedThread thread_;
};

} // namespace kotlin

#endif // !KONAN_NO_THREADS
