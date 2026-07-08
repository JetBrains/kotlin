/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <cstdint>
#include <type_traits>

#include "Allocator.hpp"
#include "GC.hpp"
#include "IntrusiveList.hpp"
#include "KAssert.h"

namespace kotlin::gc {

// Per-object GC header for the generational mark & sweep collector.
//
// This is a SINGLE atomic pointer-sized word that encodes both the CMS mark/mark-stack state and the
// generational "old" bit, so the header stays 8 bytes (no growth over CMS). The word is partitioned:
//
//   bit 1 (kOldBit)         : generational "old" (sticky) flag. Orthogonal to everything below.
//   the rest (mark/link)    : inherited verbatim from CMS --
//       0                   : white  (unmarked, not on the mark stack)
//       kNoQueueMark (== 1) : marked, terminal (not linked into the mark stack)
//       real ObjectData*    : marked, and linked into the intrusive mark stack (points at successor)
//
// Real ObjectData* values are 8-aligned, so their low 3 bits are free; the mark/link component uses
// only bit 0 (the kNoQueueMark sentinel) and the pointer bits, leaving bit 1 exclusively for old.
// This is why old can share the word without ever colliding with the CMS mark-stack linkage.
//
// CRITICAL INVARIANT that makes the single-word packing safe: the mark-stack CAS
// (intrusive_forward_list::try_push_front -> trySetNext) and marking (tryMark) both expect the
// "unmarked + unlinked" state. In CMS that state is exactly next_ == nullptr; here it is
// raw_ == <old bit only>. Every marking/linking mutation below therefore computes its CAS `expected`
// and its stored value by carrying the object's *current* old bit through unchanged. The old bit is
// only ever written during sweep (single writer, no concurrent marking), so reading it once and
// folding it into a store/CAS during the mark phase races with nothing.
//
// Semantics of the (marked, old) pair between collections:
//   (marked=0, old=0) : young, live-if-reachable  -> traced & swept every collection
//   (marked=0, old=1) : old survivor              -> implicitly live in Eden, re-traced in Full
// During a collection `marked` is additionally set on reached objects.
class GC::ObjectData {
    // Mark/link sentinel: "marked, but not linked into the mark stack" (no real successor).
    static constexpr uintptr_t kNoQueueMark = 1;
    // Generational sticky "old" flag. Bit 1 is free because kNoQueueMark uses bit 0 and real
    // ObjectData* successors are >= 8-aligned (low 3 bits clear).
    static constexpr uintptr_t kOldBit = 2;
    // The mark/link component with the old bit masked out. Reproduces the CMS `next_` value exactly:
    // 0 == white, kNoQueueMark == marked-terminal, real pointer == marked-linked.
    static uintptr_t markLink(uintptr_t raw) noexcept { return raw & ~kOldBit; }

public:
    bool tryMark() noexcept { return trySetNext(reinterpret_cast<ObjectData*>(kNoQueueMark)); }

    void markUncontended() noexcept {
        RuntimeAssert(!marked(), "Must not be marked previously");
        // Preserve the old bit; set the mark/link component to the terminal sentinel.
        raw_.store(kNoQueueMark | (raw_.load(std::memory_order_relaxed) & kOldBit), std::memory_order_relaxed);
        RuntimeAssert(markLink(raw_.load(std::memory_order_relaxed)) == kNoQueueMark, "Non-atomic marking must not be contended");
    }

    bool marked() const noexcept { return markLink(raw_.load(std::memory_order_relaxed)) != 0; }

    bool tryResetMark() noexcept {
        auto raw = raw_.load(std::memory_order_relaxed);
        if (markLink(raw) == 0) return false;
        // Clear the mark/link component, keep the old bit. Called only during sweep (single writer).
        raw_.store(raw & kOldBit, std::memory_order_relaxed);
        return true;
    }

    // --- Generational state ---

    bool isOld() const noexcept { return (raw_.load(std::memory_order_relaxed) & kOldBit) != 0; }
    void setOld() noexcept { raw_.fetch_or(kOldBit, std::memory_order_relaxed); }
    void resetOld() noexcept { raw_.fetch_and(~kOldBit, std::memory_order_relaxed); }

    // Eden-collection liveness decision, applied while sweeping an eden page. Old survivors are
    // unconditionally kept (they are not traced in Eden). Young objects marked this cycle are kept
    // and promoted to old. Everything else (unmarked young) is reclaimable.
    // Returns true if the object must be KEPT; on keep, normalizes state to (marked=0, old=1).
    bool edenSweepKeep() noexcept {
        if (isOld()) {
            // Old survivor sharing an eden page. Marks are never set on old objects during Eden.
            RuntimeAssert(!marked(), "Old object must not be marked during an Eden collection");
            return true;
        }
        if (marked()) {
            // Young object reached this cycle: promote.
            tryResetMark();
            setOld();
            return true;
        }
        // Unmarked young object: dead.
        return false;
    }

    // Full-collection liveness decision. Keeps objects reached this cycle (regardless of age),
    // reclaims the rest (including dead old objects). On keep, normalizes to (marked=0, old=1).
    bool fullSweepKeep() noexcept {
        if (tryResetMark()) {
            setOld();
            return true;
        }
        return false;
    }

private:
    friend struct DefaultIntrusiveForwardListTraits<ObjectData>;

    // Mark-stack linkage view (used by the intrusive list). Returns the successor with the old bit
    // hidden, exactly reproducing the CMS `next_` semantics: nullptr for white/unlinked, (ObjectData*)1
    // for the marked-terminal sentinel, or the real successor pointer when linked.
    ObjectData* next() const noexcept { return reinterpret_cast<ObjectData*>(markLink(raw_.load(std::memory_order_relaxed))); }
    void setNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        // Preserve the old bit; overwrite the mark/link component. Concurrency matches CMS: the list
        // never invokes setNext() on a node concurrently, and old is not written during marking.
        raw_.store(reinterpret_cast<uintptr_t>(next) | (raw_.load(std::memory_order_relaxed) & kOldBit), std::memory_order_relaxed);
    }
    bool trySetNext(ObjectData* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        // CAS from "unmarked + unlinked" (mark/link component == 0) to the requested successor, carrying
        // the current old bit through both sides. old is stable during the mark phase (only written at
        // sweep), so a single read-then-CAS needs no retry loop: a lost CAS means another thread won the
        // mark/link, which is the correct "return false".
        const uintptr_t oldBit = raw_.load(std::memory_order_relaxed) & kOldBit;
        uintptr_t expected = oldBit;
        const uintptr_t desired = reinterpret_cast<uintptr_t>(next) | oldBit;
        return raw_.compare_exchange_strong(expected, desired, std::memory_order_relaxed);
    }

    std::atomic<uintptr_t> raw_ = 0;
};
static_assert(std::is_trivially_destructible_v<GC::ObjectData>);
static_assert(sizeof(GC::ObjectData) == sizeof(void*), "gms ObjectData must stay a single word (old bit packed into the mark word)");

} // namespace kotlin::gc
