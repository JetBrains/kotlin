/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_REPEATED_TIMER_H
#define RUNTIME_REPEATED_TIMER_H

#include <chrono>
#include <condition_variable>
#include <mutex>
#include <thread>

#include "KAssert.h"
#include "Utils.hpp"

namespace kotlin {

class RepeatedTimer : private Pinned {
public:
    template <typename Rep, typename Period, typename F>
    RepeatedTimer(std::chrono::duration<Rep, Period> interval, F f) noexcept :
        thread_([this, interval, f]() noexcept { Run(interval, f); }) {}

    ~RepeatedTimer() {
        {
            std::unique_lock lock(mutex_);
            run_ = false;
        }
        wait_.notify_all();
        thread_.join();
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
    std::thread thread_;
};

} // namespace kotlin

#endif // RUNTIME_REPEATED_TIMER_H
