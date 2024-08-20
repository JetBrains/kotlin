/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MUTEX_H
#define RUNTIME_MUTEX_H

#include <atomic>
#include <cinttypes>
#include <thread>

#include "CallsChecker.hpp"
#include "KAssert.h"
#include "Memory.h"
#include "Utils.hpp"

namespace kotlin {

enum class MutexThreadStateHandling {
    kIgnore, kSwitchIfRegistered
};

class SpinLock : private Pinned {
public:
    bool try_lock() noexcept {
        return !flag_.test_and_set(std::memory_order_acquire);
    }

    void lock() noexcept {
        while(!try_lock()) {
            std::this_thread::yield();
        }
    }

    void unlock() noexcept {
        flag_.clear(std::memory_order_release);
    }

private:
    std::atomic_flag flag_ = ATOMIC_FLAG_INIT;
};

template<typename Mutex>
class ThreadStateAware : private Pinned {
public:
    void lock() noexcept(noexcept(std::declval<Mutex>().lock())) {
        // Fast path without thread state switching.
        if (try_lock()) {
            return;
        }

        kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        mutex_.lock();
    }

    bool try_lock() noexcept(noexcept(std::declval<Mutex>().lock())) {
        return mutex_.try_lock();
    }

    void unlock() noexcept(noexcept(std::declval<Mutex>().unlock())) {
        mutex_.unlock();
    }

private:
    Mutex mutex_{};
};

class RWSpinLock : private Pinned {
    using State = uint64_t;

public:
    void lock() noexcept {
        auto state = state_.load(std::memory_order_relaxed);
        while (!try_lock_impl(state)) {
            if (state % 2 == 0) {
                // There're some readers and no writers are pending. Try announcing the writer.
                state_.compare_exchange_strong(state, state + 1, std::memory_order_relaxed, std::memory_order_relaxed);
                continue;
            }
            // Lock is busy, wait.
            yield();
            state = state_.load(std::memory_order_relaxed);
        }
    }

    bool try_lock() noexcept {
        auto state = state_.load(std::memory_order_relaxed);
        return try_lock_impl(state);
    }

    void unlock() noexcept {
        auto actual = state_.exchange(0, std::memory_order_release);
        RuntimeAssert(
                actual == kLocked, "Broken RWSpinLock@%p::unlock. Expected state to be %" PRIu64 " actual %" PRIu64, this, kLocked, actual);
    }

    void lock_shared() noexcept {
        auto state = state_.load(std::memory_order_relaxed);
        while (!try_lock_shared_impl(state)) {
            // Lock is busy, wait.
            yield();
            state = state_.load(std::memory_order_relaxed);
        }
    }

    // Fails only if there're pending writers.
    bool try_lock_shared() noexcept {
        auto state = state_.load(std::memory_order_relaxed);
        return try_lock_shared_impl(state);
    }

    void unlock_shared() noexcept { state_.fetch_sub(2, std::memory_order_release); }

private:
    bool try_lock_impl(State& state) noexcept {
        do {
            if (state > 1) {
                // Readers are pending, or it's blocked by another writer.
                return false;
            }
            // Only writers are pending. Let's try locking.
        } while (!state_.compare_exchange_weak(state, kLocked, std::memory_order_acquire, std::memory_order_relaxed));
        return true;
    }

    bool try_lock_shared_impl(State& state) noexcept {
        do {
            if (state % 2 != 0) {
                // There's a pending or active writer. Fail.
                return false;
            }
            // Only readers
        } while (!state_.compare_exchange_weak(state, state + 2, std::memory_order_acquire, std::memory_order_relaxed));
        return true;
    }

    // No need to check for external calls, because we explicitly ignore thread state.
    static NO_EXTERNAL_CALLS_CHECK NO_INLINE void yield() { std::this_thread::yield(); }

    static inline constexpr State kLocked = std::numeric_limits<State>::max();
    static_assert(kLocked % 2 == 1, "Must be odd");
    std::atomic<State> state_{0};
};

} // namespace kotlin

#endif // RUNTIME_MUTEX_H
