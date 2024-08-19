/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Types.h"

namespace kotlin {

// Must match MemoryOrdering.kt
enum class MemoryOrdering : KInt {
    kRelaxed = 0,
    kAcquire = 1,
    kRelease = 2,
    kAcquireRelease = 3,
    kSequentiallyConsistent = 4,
};

inline constexpr auto toStdMemoryOrder(MemoryOrdering ordering) -> std::memory_order {
    switch (ordering) {
        case MemoryOrdering::kRelaxed:
            return std::memory_order_relaxed;
        case MemoryOrdering::kAcquire:
            return std::memory_order_acquire;
        case MemoryOrdering::kRelease:
            return std::memory_order_release;
        case MemoryOrdering::kAcquireRelease:
            return std::memory_order_acq_rel;
        case MemoryOrdering::kSequentiallyConsistent:
            return std::memory_order_seq_cst;
    }
}

} // namespace kotlin
