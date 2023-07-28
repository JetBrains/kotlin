/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <type_traits>

#include "AllocatorImpl.hpp"
#include "GC.hpp"
#include "IntrusiveList.hpp"
#include "KAssert.h"

namespace kotlin::gc {

class GC::ObjectData {
public:
    bool tryMark() noexcept { return trySetNext(reinterpret_cast<ObjectData*>(1)); }

    bool marked() const noexcept { return next_ != nullptr; }

    bool tryResetMark() noexcept {
        if (next_ == nullptr) return false;
        next_ = nullptr;
        return true;
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

    ObjectData* next() const noexcept { return next_; }
    void setNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_ = next;
    }
    bool trySetNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (next_ != nullptr) {
            return false;
        }
        next_ = next;
        return true;
    }

    ObjectData* next_ = nullptr;
};
static_assert(std::is_trivially_destructible_v<GC::ObjectData>);

} // namespace kotlin::gc
