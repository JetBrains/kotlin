/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_SINGLEOBJECTPAGE_HPP_
#define CUSTOM_ALLOC_CPP_SINGLEOBJECTPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"
#include "ExtraObjectPage.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc {

class alignas(8) SingleObjectPage {
public:
    using GCSweepScope = gc::GCHandle::GCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle& handle) noexcept { return handle.sweep(); }

    static SingleObjectPage* Create(uint64_t cellCount) noexcept;

    void Destroy() noexcept;

    uint8_t* Data() noexcept;

    uint8_t* TryAllocate() noexcept;

    bool Sweep(GCSweepScope& sweepHandle, FinalizerQueue& finalizerQueue) noexcept;

private:
    friend class AtomicStack<SingleObjectPage>;

    explicit SingleObjectPage(size_t size) noexcept;

    SingleObjectPage* next_;
    bool isAllocated_ = false;
    size_t size_;
    struct alignas(8) {
        uint8_t data_[];
    };
};

} // namespace kotlin::alloc

#endif
