/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <type_traits>

#include "AllocatorImpl.hpp"
#include "GC.hpp"
#include "IntrusiveList.hpp"
#include "KAssert.h"

namespace kotlin::gc {

class GC::ObjectData {
    static const std::size_t kNonQueueMark = 1;
    static const std::size_t kVerificationMark = 2;
public:
    bool tryMark() noexcept { return trySetNext(reinterpret_cast<ObjectData*>(kNonQueueMark)); }

    bool marked() const noexcept { return next() != nullptr; }

    bool tryResetMark() noexcept {
        if (next() == nullptr) return false;
        next_.store(nullptr, std::memory_order_relaxed);
        return true;
    }

    bool markIsVerified() const noexcept {
        return reinterpret_cast<std::size_t>(next()) == kVerificationMark;
    }

    void remarkVerified() noexcept {
        RuntimeCheck(marked(), "Verification mark must operate only already marked objects");
        next_.store(reinterpret_cast<ObjectData*>(kVerificationMark), std::memory_order_relaxed);
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

    ObjectData* next() const noexcept { return next_.load(std::memory_order_relaxed); }
    void setNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (compiler::gcCheckMarkCorrectness()) {
            RuntimeAssert(reinterpret_cast<std::size_t>(next) != kVerificationMark, "Special verification mark tag is used by common marking");
        }
        next_.store(next, std::memory_order_relaxed);
    }
    bool trySetNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (compiler::gcCheckMarkCorrectness()) {
            RuntimeAssert(reinterpret_cast<std::size_t>(next) != kVerificationMark, "Special verification mark tag is used by common marking");
        }
        ObjectData* expected = nullptr;
        return next_.compare_exchange_strong(expected, next, std::memory_order_relaxed);
    }

    std::atomic<ObjectData*> next_ = nullptr;
};
static_assert(std::is_trivially_destructible_v<GC::ObjectData>);

} // namespace kotlin::gc
