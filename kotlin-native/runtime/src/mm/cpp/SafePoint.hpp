/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <utility>

#include "ThreadData.hpp"
#include "Utils.hpp"
#include "CallsChecker.hpp"

namespace kotlin::mm {

class ThreadData;

class SafePointActivator : private MoveOnly {
public:
    SafePointActivator() noexcept;
    ~SafePointActivator();

    SafePointActivator(SafePointActivator&& rhs) noexcept : active_(rhs.active_) { rhs.active_ = false; }

    SafePointActivator& operator=(SafePointActivator&& rhs) noexcept {
        SafePointActivator other(std::move(rhs));
        swap(other);
        return *this;
    }

    void swap(SafePointActivator& rhs) noexcept { std::swap(active_, rhs.active_); }

private:
    bool active_;
};

void safePoint(std::memory_order fastPathOrder = std::memory_order_relaxed) noexcept;
void safePoint(ThreadData& threadData, std::memory_order fastPathOrder = std::memory_order_relaxed) noexcept;

/**
 * Helps in implementation and execution of actions which should be performed by each thread exactly once.
 * e.g. GC barriers enablement.
 *
 * Ensures that either the mutator thread performs the action itself, or the requester thread thread does it for them
 * (in case mutator thread is not reachable e.g. in the native code at the moment).
 */
template<typename Impl, typename SafePointActivator = mm::SafePointActivator>
class OncePerThreadAction {
public:
    class ThreadData final : private Pinned {
        friend OncePerThreadAction;
    public:
        explicit ThreadData(OncePerThreadAction& owner, mm::ThreadData& base) noexcept : owner_(owner), base_(base) {}
        void onSafePoint() noexcept {
            CallsCheckerIgnoreGuard guard;

            if (owner_.actionRequested_.load()) {
                if (actionPerformed_.load(std::memory_order_acquire)) return; // TODO to acquire barriers state?
                std::unique_lock lock(mutex_);
                if (actionPerformed_.load(std::memory_order_relaxed)) return; // TODO order?
                Impl::action(base_);
                actionPerformed_.store(true, std::memory_order_release);
            }
        }
    private:
        OncePerThreadAction& owner_;
        mm::ThreadData& base_;
        std::atomic<bool> actionPerformed_ = false;
        std::mutex mutex_;
    };

    void ensurePerformed(mm::ThreadRegistry::Iterable& threads) {
        RuntimeAssert(actionRequested_ == false, "no pending action expected");

        // reset action performed flag
        for (auto& thread: threads) {
            OncePerThreadAction::ThreadData& utilityData = Impl::getUtilityData(thread);
            std::unique_lock lock(utilityData.mutex_);
            utilityData.actionPerformed_.store(false, std::memory_order_relaxed); // TODO order?
        }

        AutoReset scopedRequestGuard(&actionRequested_, true);
        SafePointActivator safePointActivator;

        while (!std::all_of(threads.begin(), threads.end(), [=](mm::ThreadData& thread) {
            OncePerThreadAction::ThreadData& utilityData = Impl::getUtilityData(thread);

            if (utilityData.actionPerformed_.load(std::memory_order_acquire)) return true;

            std::unique_lock lock(utilityData.mutex_, std::try_to_lock);
            if (!lock) {
                // The thread must be performing the action. Skip them for now.
                return false;
            }


            if (thread.suspensionData().suspendedOrNative()) {
                Impl::action(thread);
                utilityData.actionPerformed_.store(true, std::memory_order_release);
                return true;
            }

            // let a runnable thread perform the action themself
            return false;
        })) { std::this_thread::yield(); }
    }

    void ensurePerformedInSTW(mm::ThreadRegistry::Iterable& threads) {
        for (auto& thread: threads) {
            Impl::action(thread);
        }
    }

private:
    std::atomic<bool> actionRequested_ = false;
};

namespace test_support {

bool safePointsAreActive() noexcept;
void setSafePointAction(void (*action)(mm::ThreadData&)) noexcept;

} // namespace test_support

} // namespace kotlin::mm
