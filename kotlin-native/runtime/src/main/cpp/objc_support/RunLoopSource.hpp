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
#include "objc_support/ObjectPtr.hpp"

namespace kotlin::objc_support {

// Smart pointer around `CFRunLoopSource`.
//
// To attach to the current run loop use `attachToCurrentRunLoop`, it'll be detached when the returned `Subscription`
// is destroyed.
// `RunLoopSource` is initialized with `std::function<void()>`, which will be called every time the run loop processes
// this source.
// To notify attaced run loops that this source has more work, use `signal`.
// The underlying raw pointer is available via `handle`.
class RunLoopSource : private Pinned {
public:
    // A token that `RunLoopSource` is attached to a run loop.
    //
    // Must be destroyed on the same thread that called `attachToCurrentRunLoop`.
    class [[nodiscard]] Subscription : private Pinned {
        // TODO: Consider making it movable.
    public:
        ~Subscription() {
            RuntimeAssert(*runLoop_ == CFRunLoopGetCurrent(), "Must be destroyed on the same thread as created");
            CFRunLoopRemoveSource(*runLoop_, *owner_->source_, mode_);
            owner_->unregisterSubscription(*this);
        }

    private:
        friend class RunLoopSource;

        Subscription(RunLoopSource& owner, CFRunLoopMode mode) noexcept :
            owner_(&owner), runLoop_(object_ptr_retain, CFRunLoopGetCurrent()), mode_(mode) {
            owner_->registerSubscription(*this);
            CFRunLoopAddSource(*runLoop_, *owner_->source_, mode);
        }

        raw_ptr<RunLoopSource> owner_;
        object_ptr<CFRunLoopRef> runLoop_;
        CFRunLoopMode mode_;
    };

    // Create `RunLoopSource` with `callback` that will be invoked each time, when an attached run loop processes this source.
    explicit RunLoopSource(std::function<void()> callback) noexcept :
        callback_(std::move(callback)),
        sourceContext_{0, &callback_, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, &perform},
        source_(CFRunLoopSourceCreate(nullptr, 0, &sourceContext_)) {}

    ~RunLoopSource() {
        auto* subscription = activeSubscription_.load(std::memory_order_relaxed);
        RuntimeAssert(subscription == nullptr, "Expected no active subscription, but was %p", subscription);
    }

    // Raw pointer to `CFRunLoopSource`.
    auto handle() noexcept { return *source_; }

    // Signal the attached run loops, that this `RunLoopSource` has some work.
    void signal() noexcept { CFRunLoopSourceSignal(*source_); }

    // Attach this `RunLoopSource` to the current thread's run loop.
    [[nodiscard]] std::unique_ptr<Subscription> attachToCurrentRunLoop(CFRunLoopMode mode = kCFRunLoopDefaultMode) noexcept {
        return std::unique_ptr<Subscription>(new Subscription(*this, mode));
    }

private:
    static void perform(void* callback) noexcept { static_cast<decltype(callback_)*>(callback)->operator()(); }

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
    CFRunLoopSourceContext sourceContext_;
    object_ptr<CFRunLoopSourceRef> source_;
    std::atomic<Subscription*> activeSubscription_ = nullptr;
};

} // namespace kotlin::objc_support

#endif