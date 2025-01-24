/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ExtraObjectData.hpp"

namespace kotlin::alloc {

struct ExtraObjectCell {
    static constexpr AllocationSize size() {
        return AllocationSize::bytesExactly(sizeof(ExtraObjectCell));
    }

    static ExtraObjectCell* fromExtraObject(mm::ExtraObjectData* extraObjectData) {
        return reinterpret_cast<ExtraObjectCell*>(reinterpret_cast<uint8_t*>(extraObjectData) - offsetof(ExtraObjectCell, data_));
    }

    mm::ExtraObjectData* Data() { return reinterpret_cast<mm::ExtraObjectData*>(data_); }

    // This is used to build a finalizers queue.
    std::atomic<ExtraObjectCell*> next_;
    struct alignas(mm::ExtraObjectData) {
        uint8_t data_[sizeof(mm::ExtraObjectData)];
    };
};

} // namespace kotlin::alloc