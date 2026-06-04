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
#include "SweepDebug.hpp"

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
        auto prev = next_.load(std::memory_order_relaxed);
        if (prev == nullptr) {
            // Only log DEAD events. KEEP fires once per live object per cycle and
            // pushes total event volume into the millions, which masks the bug
            // via timing perturbation.
            debug::recordSweepEvent(
                    reinterpret_cast<uintptr_t>(this), debug::kDead, 0);
            return false;
        }
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
        bool ok = next_.compare_exchange_strong(expected, next, std::memory_order_relaxed);
        if (!ok) {
            // The CAS observed `next_` already non-null. `expected` was updated
            // by compare_exchange_strong to that observed value. We record this
            // as a CAS_FAIL with the observed value as aux. The hypothesis being
            // tested: this read may be a stale view of a prior cycle's mark token
            // (i.e. the previous-cycle sweep-clear of `next_` was not visible to
            // this thread), causing this marker to incorrectly conclude the
            // object is already marked and skip enqueuing it.
            debug::recordSweepEvent(
                    reinterpret_cast<uintptr_t>(this),
                    debug::kCASFail,
                    reinterpret_cast<uintptr_t>(expected));
        } else {
            // The CAS succeeded: this thread transitioned next_ from null to
            // non-null. Record so we can later determine, for the failing
            // object, whether mark ever attempted+succeeded on it in the
            // failing cycle (presence of MARK_OK in epoch N before DEAD/N
            // means the mark write didn't propagate to the sweeper's view).
            debug::recordSweepEvent(
                    reinterpret_cast<uintptr_t>(this),
                    debug::kMarkOk,
                    reinterpret_cast<uintptr_t>(next));
        }
        return ok;
    }

    std::atomic<ObjectData*> next_ = nullptr;
};
static_assert(std::is_trivially_destructible_v<GC::ObjectData>);

} // namespace kotlin::gc
