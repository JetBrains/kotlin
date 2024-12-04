/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <type_traits>

#include "Allocator.hpp"
#include "GC.hpp"
#include "IntrusiveList.hpp"
#include "KAssert.h"

namespace kotlin::gc {

class GC::ObjectData {
    static constexpr intptr_t kNoQueueMark = 1;
public:
    bool tryMark() noexcept { return trySetNext(reinterpret_cast<ObjectData*>(kNoQueueMark)); }

    void markUncontended() noexcept {
        RuntimeAssert(!marked(), "Must not be marked previously");
        auto nextVal = reinterpret_cast<ObjectData*>(kNoQueueMark);
        setNext(nextVal);
        RuntimeAssert(next() == nextVal, "Non-atomic marking must not be contended");
    }

    bool marked() const noexcept { return next() != nullptr; }

    bool tryResetMark() noexcept {
        if (next() == nullptr) return false;
        next_.store(nullptr, std::memory_order_relaxed);
        return true;
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

    ObjectData* next() const noexcept { return next_.load(std::memory_order_relaxed); }
    void setNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_.store(next, std::memory_order_relaxed);
    }
    bool trySetNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        ObjectData* expected = nullptr;
        return next_.compare_exchange_strong(expected, next, std::memory_order_relaxed);
    }

    std::atomic<ObjectData*> next_ = nullptr;
};
static_assert(std::is_trivially_destructible_v<GC::ObjectData>);

} // namespace kotlin::gc
