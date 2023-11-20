/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include <condition_variable>
#include <mutex>
#include <CoreFoundation/CFRunLoop.h>

#include "ScopedThread.hpp"
#include "Utils.hpp"
#include "objc_support/ObjectPtr.hpp"

namespace kotlin::objc_support::test_support {

class RunLoopInScopedThread : private Pinned {
public:
    template <typename Init>
    explicit RunLoopInScopedThread(Init init) noexcept :
        thread_([&]() noexcept {
            [[maybe_unused]] auto state = init();
            {
                std::unique_lock guard{stateMutex_};
                runLoop_.reset(object_ptr_retain, CFRunLoopGetCurrent());
                RuntimeAssert(*runLoop_ != nullptr, "Current run loop cannot be null");
                RuntimeAssert(state_ == State::kInitial, "Expected state to be %d but was %d", State::kInitial, state_);
                state_ = State::kRunning;
            }
            initializedCV_.notify_one();
            while (true) {
                CFRunLoopRun();
                std::unique_lock guard{stateMutex_};
                if (state_ != State::kRunning) {
                    RuntimeAssert(state_ == State::kWillStop, "Expected state to be %d but was %d", State::kWillStop, state_);
                    RuntimeAssert(*runLoop_ == nullptr, "Stored run loop must have been nulled");
                    state_ = State::kStopped;
                    break;
                }
            }
        }) {
        std::unique_lock guard{stateMutex_};
        initializedCV_.wait(guard, [this]() noexcept { return state_ >= State::kRunning; });
    }

    ~RunLoopInScopedThread() {
        object_ptr<CFRunLoopRef> runLoop;
        {
            std::unique_lock guard{stateMutex_};
            runLoop = std::move(runLoop_);
            RuntimeAssert(state_ == State::kRunning, "Expected state to be %d but was %d", State::kRunning, state_);
            state_ = State::kWillStop;
        }
        CFRunLoopStop(*runLoop);
    }

    CFRunLoopRef handle() noexcept { return *runLoop_; }

    void wakeUp() noexcept { CFRunLoopWakeUp(*runLoop_); }

    void schedule(std::function<void()> f) noexcept {
        CFRunLoopPerformBlock(*runLoop_, kCFRunLoopDefaultMode, ^{
            f();
        });
        CFRunLoopWakeUp(*runLoop_);
    }

private:
    enum class State {
        kInitial,
        kRunning,
        kWillStop,
        kStopped,
    };

    std::mutex stateMutex_;
    std::condition_variable initializedCV_;
    object_ptr<CFRunLoopRef> runLoop_;
    State state_ = State::kInitial;
    ScopedThread thread_;
};

} // namespace kotlin::objc_support::test_support

#endif