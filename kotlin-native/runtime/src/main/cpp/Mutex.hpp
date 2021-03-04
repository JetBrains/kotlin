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

#include <cstdint>

#include "KAssert.h"
#include "Utils.hpp"

namespace kotlin {

class SpinLock : private Pinned {
public:
    void lock() noexcept {
        while (!__sync_bool_compare_and_swap(&atomicInt, 0, 1)) {
            // TODO: yield.
        }
    }

    void unlock() noexcept {
        if (!__sync_bool_compare_and_swap(&atomicInt, 1, 0)) {
            RuntimeAssert(false, "Unable to unlock");
        }
    }

private:
    // TODO: Consider using `std::atomic_flag`.
    int32_t atomicInt = 0;
};

} // namespace kotlin

#endif // RUNTIME_MUTEX_H
