/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_SINGLEOBJECTPAGE_HPP_
#define CUSTOM_ALLOC_CPP_SINGLEOBJECTPAGE_HPP_

#include <atomic>
#include <cstdint>
#include <vector>

#include "AnyPage.hpp"
#include "AllocationSize.hpp"
#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

class SingleObjectPage;

class alignas(kPageAlignment) SingleObjectPage : public AnyPage<SingleObjectPage> {
public:
    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static SingleObjectPage* Create(uint64_t cellCount) noexcept;

    void Destroy() noexcept;

    uint8_t* Data() noexcept;

    uint8_t* Allocate() noexcept;

    bool SweepAndDestroy(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

    template <typename F>
    void TraverseAllocatedBlocks(F process) noexcept(noexcept(process(std::declval<uint8_t*>()))) {
        process(data_);
    }

private:
    friend class Heap;

    explicit SingleObjectPage(AllocationSize objectSize) noexcept;

    static AllocationSize pageSize(AllocationSize objectSize) noexcept;

    // Testing method
    std::vector<uint8_t*> GetAllocatedBlocks() noexcept;

    struct alignas(8) {
        uint8_t data_[];
    };
};

} // namespace kotlin::alloc

#endif
