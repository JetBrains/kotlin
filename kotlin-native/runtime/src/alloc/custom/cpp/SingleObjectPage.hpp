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
#include "GCStatistics.hpp"
#include "CustomLogging.hpp"
#include "CustomFinalizerProcessor.hpp"

namespace kotlin::alloc {

class SingleObjectPage;

class alignas(kPageAlignment) SingleObjectPage : public AnyPage<SingleObjectPage> {
public:

    static SingleObjectPage* Create(uint64_t cellCount) noexcept;

    uint8_t* Data() noexcept;

    uint8_t* Allocate() noexcept;

    template<typename SweepTraits>
    bool SweepAndDestroy(typename SweepTraits::GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept {
        CustomAllocDebug("SingleObjectPage@%p::SweepAndDestroy()", this);
        if (!SweepTraits::trySweepElement(Data(), finalizerQueue, sweepHandle)) {
            return true;
        }

        Destroy<SweepTraits>();

        return false;
    }

    template<typename SweepTraits>
    void Destroy() noexcept {
        auto objectSize = SweepTraits::elementSize(data_);
        destroyImpl(objectSize);
    }

    template <typename F>
    void TraverseAllocatedBlocks(F process) noexcept(noexcept(process(std::declval<uint8_t*>()))) {
        process(data_);
    }

private:
    friend class Heap;

    explicit SingleObjectPage(AllocationSize objectSize) noexcept;

    static AllocationSize pageSize(AllocationSize objectSize) noexcept;

    void destroyImpl(AllocationSize objectSize) noexcept;

    // Testing method
    std::vector<uint8_t*> GetAllocatedBlocks() noexcept;

    struct alignas(8) {
        uint8_t data_[];
    };
};

} // namespace kotlin::alloc

#endif
