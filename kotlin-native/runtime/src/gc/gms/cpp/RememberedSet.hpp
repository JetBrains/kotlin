/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>

#include "Memory.h"
#include "Utils.hpp"

namespace kotlin::gc {

// Per-thread sequential store buffer (SSB) used as the generational remembered set.
//
// The write barrier appends candidate heap-reference slots selected by `beforeHeapRefUpdate`. With a
// known, stable old owner the entry is exact and can be drained directly. Ownerless paths and stores
// during the collection window conservatively record by the value's age; those entries still require
// STW container validation before loading the slot. This captures every old->young edge created between
// collections: an old object can only come to reference a young one through a post-collection write,
// which the barrier observes.
//
// Robustness: the backing store is a fixed-capacity array allocated lazily on the first remembered
// store. Threads that never produce an old->young edge therefore do not pay the 512 KiB buffer cost.
// After that first slow-path allocation, `record()` does not allocate. When the buffer fills,
// recording stops and `record()` returns false; the caller signals a global overflow so that the
// next/in-flight collection is upgraded to Full (a Full collection re-traces the whole heap and does
// not need the remembered set), after which the buffer is reset. This is the classic
// "remembered-set overflow -> full GC" fallback.
//
// Concurrency: the buffer is only appended to by its owning mutator and only drained/cleared by the
// GC while the world is stopped, so no synchronization is required on `count_`.
//
// The conservative slot-level representation remains an over-approximation: ownerless stores and
// collection-window stores can still include young->young slots. The drain must therefore resolve each
// conservative slot back to its containing object via `alloc::HeapLayoutSnapshot` and enqueue the
// referent only when that container is genuinely old. Exact entries skip this lookup: their owner was
// known old and stable when recorded, and old objects are not reclaimed by Eden. This matches
// JSC/Riptide semantics (filter on the source object's age), while adapting it to K/N's slot buffer.
// Without this filter, draining a slot inside a dead young container would resurrect and PROMOTE dead
// young objects, collapsing the generational win. A future hot-path optimization is to make the
// container recoverable by masking (`slot & ~(SIZE-1)`), as JSC does with MarkedBlock, replacing the
// drain-time snapshot lookup for conservative entries too; that is a performance refinement, not a
// correctness requirement (M7).
class RememberedSet : private Pinned {
public:
    // Per-thread slot buffer capacity. Overflow just forces a Full collection (correctness-safe
    // fallback), so this is a throughput knob: it must comfortably exceed the number of genuine
    // old->young edges created between two collections. With the source-age barrier filter
    // (shouldRememberOldToYoung) only genuine old->young stores are recorded -- young->young churn is
    // gone -- so this is bounded by the mutation rate of the old generation, not by total store
    // volume. 64K entries = 512 KiB/thread. (A growable/object-remembering set is the next step to
    // remove the fixed cap entirely; see plan R1 follow-up.)
    static constexpr size_t kCapacity = 1u << 16;

    RememberedSet() noexcept = default;

    class Entry {
    public:
        Entry() noexcept = default;

        static Entry exact(ObjHeader** slot) noexcept { return Entry(slot, true); }
        static Entry conservative(ObjHeader** slot) noexcept { return Entry(slot, false); }

        ObjHeader** slot() const noexcept { return reinterpret_cast<ObjHeader**>(raw_ & ~kExactBit); }
        bool exact() const noexcept { return (raw_ & kExactBit) != 0; }

    private:
        static constexpr uintptr_t kExactBit = 1u;

        Entry(ObjHeader** slot, bool exact) noexcept : raw_(reinterpret_cast<uintptr_t>(slot) | (exact ? kExactBit : 0u)) {
            RuntimeAssert((reinterpret_cast<uintptr_t>(slot) & kExactBit) == 0, "Remembered-set slot must be aligned");
        }

        uintptr_t raw_ = 0;
    };

    // Appends a slot. Returns false (without recording) if the buffer is full; the caller must then
    // force a Full collection.
    ALWAYS_INLINE bool record(ObjHeader** slot, bool exact) noexcept {
        if (__builtin_expect(buffer_ == nullptr, false)) {
            allocateBuffer();
        }
        size_t i = count_;
        if (__builtin_expect(i >= kCapacity, false)) {
            return false;
        }
        buffer_[i] = exact ? Entry::exact(slot) : Entry::conservative(slot);
        count_ = i + 1;
        hasConservativeEntries_ |= !exact;
        return true;
    }

    // Invokes `consumeEntry(Entry)` for each recorded slot, then empties the buffer.
    template <typename F>
    void drainInto(F&& consumeEntry) noexcept {
        for (size_t i = 0; i < count_; ++i) {
            consumeEntry(buffer_[i]);
        }
        count_ = 0;
        hasConservativeEntries_ = false;
    }

    void clear() noexcept {
        count_ = 0;
        hasConservativeEntries_ = false;
    }

    bool empty() const noexcept { return count_ == 0; }
    size_t size() const noexcept { return count_; }
    bool hasConservativeEntries() const noexcept { return hasConservativeEntries_; }

private:
    NO_INLINE void allocateBuffer() noexcept { buffer_ = std::make_unique<Entry[]>(kCapacity); }

    std::unique_ptr<Entry[]> buffer_;
    size_t count_ = 0;
    bool hasConservativeEntries_ = false;
};

} // namespace kotlin::gc
