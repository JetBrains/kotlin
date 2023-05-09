/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef RUNTIME_MUTEX_H
#define RUNTIME_MUTEX_H

#include <atomic>
#include <cinttypes>
#include <thread>

#include "KAssert.h"
#include "Memory.h"
#include "Utils.hpp"

namespace kotlin {

enum class MutexThreadStateHandling {
    kIgnore, kSwitchIfRegistered
};

template <MutexThreadStateHandling threadStateHandling>
class SpinLock;

template <>
class SpinLock<MutexThreadStateHandling::kIgnore> : private Pinned {
public:
    void lock() noexcept {
        while(flag_.test_and_set(std::memory_order_acquire)) {
            yield();
        }
    }

    void unlock() noexcept {
        flag_.clear(std::memory_order_release);
    }

private:
    std::atomic_flag flag_ = ATOMIC_FLAG_INIT;

    // No need to check for external calls, because we explicitly ignore thread state.
    static NO_EXTERNAL_CALLS_CHECK NO_INLINE void yield() {
        std::this_thread::yield();
    }
};

template <>
class SpinLock<MutexThreadStateHandling::kSwitchIfRegistered> : private Pinned {
public:
    void lock() noexcept {
        // Fast path without thread state switching.
        if (!flag_.test_and_set(std::memory_order_acquire)) {
            return;
        }

        kotlin::NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        while (flag_.test_and_set(std::memory_order_acquire)) {
            std::this_thread::yield();
        }
    }

    void unlock() noexcept {
        flag_.clear(std::memory_order_release);
    }

private:
    std::atomic_flag flag_ = ATOMIC_FLAG_INIT;
};

template <MutexThreadStateHandling threadStateHandling>
class RWSpinLock;

template <>
class RWSpinLock<MutexThreadStateHandling::kIgnore> : private Pinned {
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
