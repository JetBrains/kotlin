/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFRunLoop.h>

#include "RawPtr.hpp"
#include "Utils.hpp"
#include "objc_support/CFClock.hpp"
#include "objc_support/ObjectPtr.hpp"

namespace kotlin::objc_support {

// Smart pointer around `CFRunLoopTimer`.
//
// To attach to the current run loop use `attachToCurrentRunLoop`, it'll be detached when the returned `Subscription`
// is destroyed.
// `RunLoopTimer` is initialized with:
// - `std::function<void()>`, which will be called every time the run loop processes this timer.
// - `interval`, which is the desired average interval between firings
// - `initialFiring`, which is the minimum time before the first timer can fire.
// To override when the next firing can be performed, use `setNextFiring`.
// The underlying raw pointer is available via `handle`.
class RunLoopTimer : private Pinned {
public:
    // A token that `RunLoopTimer` is attached to a run loop.
    //
    // Must be destroyed on the same thread that called `attachToCurrentRunLoop`.
    class [[nodiscard]] Subscription : private Pinned {
        // TODO: Consider making it movable.
    public:
        ~Subscription() {
            RuntimeAssert(*runLoop_ == CFRunLoopGetCurrent(), "Must be destroyed on the same thread as created");
            CFRunLoopRemoveTimer(*runLoop_, *owner_->timer_, mode_);
            owner_->unregisterSubscription(*this);
        }

    private:
        friend class RunLoopTimer;

        Subscription(RunLoopTimer& owner, CFRunLoopMode mode) noexcept :
            owner_(&owner), runLoop_(object_ptr_retain, CFRunLoopGetCurrent()), mode_(mode) {
            owner_->registerSubscription(*this);
            CFRunLoopAddTimer(*runLoop_, *owner_->timer_, mode);
        }

        raw_ptr<RunLoopTimer> owner_;
        object_ptr<CFRunLoopRef> runLoop_;
        CFRunLoopMode mode_;
    };

    // Create `RunLoopSource` with `callback` that will be invoked each time, when an attached run loop processes this timer.
    // `interval` sets the desired time between timer firing (the system will try to make the average time between firings be at least
    // `interval`, but the time between 2 consecutive tasks may be smaller if the current average is larger).
    // `fireDate` sets the minimum time before the timer can be fired for the first time.
    RunLoopTimer(std::function<void()> callback, std::chrono::duration<double> interval, cf_clock::time_point fireDate) noexcept :
        callback_(std::move(callback)),
        timerContext_{0, &callback_, nullptr, nullptr, nullptr},
        timer_(CFRunLoopTimerCreate(nullptr, cf_clock::toCFAbsoluteTime(fireDate), interval.count(), 0, 0, &perform, &timerContext_)) {}

    ~RunLoopTimer() {
        auto* subscription = activeSubscription_.load(std::memory_order_relaxed);
        RuntimeAssert(subscription == nullptr, "Expected no active subscription, but was %p", subscription);
    }

    // Raw pointer to `CFRunLoopTimer`.
    auto handle() noexcept { return timer_; }

    // `at` overrides the minimum time before the next timer firing. The override is for the next firing
    // only, after it, the initially supplied `interval` will be used again.
    void setNextFiring(cf_clock::time_point at) noexcept { CFRunLoopTimerSetNextFireDate(*timer_, cf_clock::toCFAbsoluteTime(at)); }

    // `interval` overrides the minimum time before the next timer firing. The override is for the next firing
    // only, after it, the initially supplied `interval` will be used again.
    void setNextFiring(cf_clock::duration interval) noexcept { setNextFiring(cf_clock::now() + interval); }

    // Attach this `RunLoopTimer` to the current thread's run loop.
    [[nodiscard]] std::unique_ptr<Subscription> attachToCurrentRunLoop(CFRunLoopMode mode = kCFRunLoopDefaultMode) noexcept {
        return std::unique_ptr<Subscription>(new Subscription(*this, mode));
    }

private:
    static void perform(CFRunLoopTimerRef, void* callback) noexcept { static_cast<decltype(callback_)*>(callback)->operator()(); }

    void registerSubscription(Subscription& subscription) noexcept {
        Subscription* actual = nullptr;
        activeSubscription_.compare_exchange_strong(actual, &subscription, std::memory_order_relaxed);
        RuntimeAssert(
                actual == nullptr, "Cannot have more than one active subscription. Trying to regiser %p but %p is already active",
                &subscription, actual);
    }

    void unregisterSubscription(Subscription& subscription) noexcept {
        Subscription* actual = &subscription;
        activeSubscription_.compare_exchange_strong(actual, nullptr, std::memory_order_relaxed);
        RuntimeAssert(actual == &subscription, "Expected %p to be active subscription. But was %p", &subscription, actual);
    }

    std::function<void()> callback_; // TODO: std::function_ref?
    CFRunLoopTimerContext timerContext_;
    object_ptr<CFRunLoopTimerRef> timer_;
    std::atomic<Subscription*> activeSubscription_ = nullptr;
};

} // namespace kotlin::objc_support

#endif