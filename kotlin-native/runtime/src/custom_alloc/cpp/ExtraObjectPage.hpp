/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_
#define CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"

namespace kotlin::alloc {

struct ExtraObjectCell {
    mm::ExtraObjectData* Data() { return reinterpret_cast<mm::ExtraObjectData*>(data_); }

    // This is used to simultaneously build two lists: a free list and a finalizers queue.
    // A cell cannot exist in both of them, but can be in neither when it's alive.
    ExtraObjectCell* next_;
    struct alignas(mm::ExtraObjectData) {
        uint8_t data_[sizeof(mm::ExtraObjectData)];
    };
};

class alignas(8) ExtraObjectPage {
public:
    static ExtraObjectPage* Create() noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    mm::ExtraObjectData* TryAllocate() noexcept;

    bool Sweep(AtomicStack<ExtraObjectCell>& finalizerQueue) noexcept;

private:
    friend class AtomicStack<ExtraObjectPage>;

    ExtraObjectPage() noexcept;

    // Used for linking pages together in `pages` queue or in `unswept` queue.
    ExtraObjectPage* next_;
    ExtraObjectCell* nextFree_;
    ExtraObjectCell cells_[];
};

} // namespace kotlin::alloc

#endif
