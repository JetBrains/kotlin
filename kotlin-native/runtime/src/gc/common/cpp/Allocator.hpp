/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <utility>

#include "std_support/CStdlib.hpp"

namespace kotlin {
namespace gc {

class AlignedAllocator {
public:
    void* Alloc(size_t size, size_t alignment) noexcept { return std_support::aligned_calloc(alignment, 1, size); }
    static void Free(void* instance) noexcept { std_support::free(instance); }
};

template <typename BaseAllocator, typename GCThreadData>
class AllocatorWithGC {
public:
    AllocatorWithGC(BaseAllocator base, GCThreadData& gc) noexcept : base_(std::move(base)), gc_(gc) {}

    void* Alloc(size_t size, size_t alignment) noexcept {
        gc_.SafePointAllocation(size);
        if (void* ptr = base_.Alloc(size, alignment)) {
            return ptr;
        }
        // Tell GC that we failed to allocate, and try one more time.
        gc_.OnOOM(size);
        return base_.Alloc(size, alignment);
    }

    static void Free(void* instance) noexcept { BaseAllocator::Free(instance); }

private:
    BaseAllocator base_;
    GCThreadData& gc_;
};

} // namespace gc
} // namespace kotlin
