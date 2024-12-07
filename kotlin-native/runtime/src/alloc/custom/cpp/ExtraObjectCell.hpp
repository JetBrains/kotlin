/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ExtraObjectData.hpp"

namespace kotlin::alloc {

struct ExtraObjectCell {
    mm::ExtraObjectData* Data() { return reinterpret_cast<mm::ExtraObjectData*>(data_); }

    // This is used to simultaneously build two lists: a free list and a finalizers queue.
    // A cell cannot exist in both of them, but can be in neither when it's alive.
    std::atomic<ExtraObjectCell*> next_;
    struct alignas(mm::ExtraObjectData) {
        uint8_t data_[sizeof(mm::ExtraObjectData)];
    };

    static ExtraObjectCell* fromExtraObject(mm::ExtraObjectData* extraObjectData) {
        return reinterpret_cast<ExtraObjectCell*>(reinterpret_cast<uint8_t*>(extraObjectData) - offsetof(ExtraObjectCell, data_));
    }
};

} // namespace kotlin::alloc