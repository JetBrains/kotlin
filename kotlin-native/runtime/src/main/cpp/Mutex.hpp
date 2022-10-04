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

} // namespace kotlin

#endif // RUNTIME_MUTEX_H
