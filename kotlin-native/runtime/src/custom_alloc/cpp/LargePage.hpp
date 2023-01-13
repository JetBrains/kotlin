/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_LARGEPAGE_HPP_
#define CUSTOM_ALLOC_CPP_LARGEPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"

namespace kotlin::alloc {

class alignas(8) LargePage {
public:
    static LargePage* Create(uint64_t cellCount) noexcept;

    void Destroy() noexcept;

    uint8_t* TryAllocate() noexcept;

    uint8_t* Data() noexcept;

    bool Sweep() noexcept;

private:
    friend class AtomicStack<LargePage>;
    LargePage* next_;
    bool isAllocated_ = false;
    struct alignas(8) {
        uint8_t data_[];
    };
};

} // namespace kotlin::alloc

#endif
