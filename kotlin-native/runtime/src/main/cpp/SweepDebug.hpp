/*
 * Diagnostic ring buffer for tracing mark/sweep events on PMCS.
 *
 * Each event records the object's ObjectData address along with thread id,
 * GC epoch, kind, and a per-event auxiliary payload. The dump functions scan
 * matching events from all thread-local buffers and print them to stderr.
 *
 * Designed not to mask the bug:
 *   - Per-thread ring buffers eliminate contention on a global seq counter.
 *   - The hot mark-success path (MARK_OK) is still logged, but each thread
 *     writes only to its own buffer; ARM64 cache lines stay private.
 *   - KEEP and RECLAIM are intentionally NOT logged: they are the
 *     highest-volume events and removing them is what made the previous
 *     diagnostic reproduce the bug.
 *
 * Remove before landing.
 */

#pragma once

#include <atomic>
#include <cinttypes>
#include <cstdint>
#include <cstdio>

#include "Porting.h"

namespace kotlin::gc::debug {

enum SweepEventKind : uint32_t {
    kKeep = 0,       // sweep observed next_ != null; mark cleared, object kept alive (not currently logged)
    kDead = 1,       // sweep observed next_ == null; object about to be reclaimed
    kCASFail = 2,    // mark CAS failed because next_ was already non-null (possibly stale)
    kReclaim = 3,    // trySweepElement returned true; cell is about to be memset'd (not currently logged)
    kMarkOk = 4,     // mark CAS succeeded (not currently logged - too high volume)
    kArrayTrav = 5,  // processArrayInMark entered for an object-array. Lets us tell whether
                     // the parent of a failing Vector was actually traversed in the failing cycle.
};

struct SweepEvent {
    // Data fields: written by one thread only (the owner of the per-thread buffer);
    // plain non-atomic stores are sufficient and cheaper than relaxed atomics.
    uintptr_t objectDataAddr{0};
    uintptr_t aux{0};
    uintptr_t threadId{0};
    uint32_t kind{0};
    // Publication marker: atomic because readers (lldb post-crash) may need to detect
    // torn writes. Written last with release ordering.
    std::atomic<uint64_t> seq{0};
};

inline constexpr size_t kMaxThreads = 16;
inline constexpr size_t kPerThreadLogSize = 1u << 19; // 512K events per thread

struct alignas(128) PerThreadLog {  // 128-byte aligned to keep different threads on separate cache lines
    SweepEvent events[kPerThreadLogSize];
    std::atomic<uint64_t> localSeq{0};
    std::atomic<uintptr_t> threadIdSnapshot{0}; // 0 = unclaimed
};

inline PerThreadLog g_perThread[kMaxThreads];
inline std::atomic<size_t> g_nextSlot{0};

inline constexpr size_t kInvalidSlot = static_cast<size_t>(-1);

inline size_t claimMyThreadSlot() noexcept {
    size_t slot = g_nextSlot.fetch_add(1, std::memory_order_relaxed);
    if (slot >= kMaxThreads) {
        // ran out of slots; recording from this thread will be dropped
        return kInvalidSlot;
    }
    g_perThread[slot].threadIdSnapshot.store(konan::currentThreadId(), std::memory_order_relaxed);
    return slot;
}

inline size_t myThreadSlot() noexcept {
    // First call on each thread claims a slot; subsequent calls return the cached value.
    // The lambda runs once per thread (thread_local static initialization).
    static thread_local size_t cached = claimMyThreadSlot();
    return cached;
}

inline void recordSweepEvent(uintptr_t objectDataAddr, SweepEventKind kind, uintptr_t aux) noexcept {
    size_t slot = myThreadSlot();
    if (slot == kInvalidSlot) return;
    auto& tlog = g_perThread[slot];
    // localSeq is touched only by this thread → fetch_add is uncontended → ~free.
    uint64_t seq = tlog.localSeq.fetch_add(1, std::memory_order_relaxed);
    auto& evt = tlog.events[seq % kPerThreadLogSize];
    // Plain stores — single-writer, no atomic needed.
    evt.objectDataAddr = objectDataAddr;
    evt.aux = aux;
    evt.threadId = tlog.threadIdSnapshot.load(std::memory_order_relaxed);
    evt.kind = static_cast<uint32_t>(kind);
    // Release-publish so the reader (in lldb post-crash) sees a consistent slot.
    evt.seq.store(seq, std::memory_order_release);
}

inline const char* sweepEventName(uint32_t kind) noexcept {
    switch (kind) {
        case kKeep:      return "KEEP";
        case kDead:      return "DEAD";
        case kCASFail:   return "CAS_FAIL";
        case kReclaim:   return "RECLAIM";
        case kMarkOk:    return "MARK_OK";
        case kArrayTrav: return "ARRAY_TRAV";
        default:         return "?";
    }
}

inline void dumpSweepEventSlot(size_t threadSlot, const SweepEvent& evt, uint64_t seq) noexcept {
    // Reader: plain reads OK because either we're post-crash (other threads paused)
    // or the seq load already provided acquire ordering.
    std::fprintf(
            stderr,
            "[Diag]   tslot=%zu seq=%" PRIu64 " tid=0x%" PRIxPTR " kind=%s obj=0x%" PRIxPTR " aux=0x%" PRIxPTR "\n",
            threadSlot, seq, evt.threadId, sweepEventName(evt.kind), evt.objectDataAddr, evt.aux);
}

inline void dumpSweepHistoryForObjectData(uintptr_t objectDataAddr) noexcept {
    std::fprintf(
            stderr,
            "[Diag] sweep-history objectData=0x%" PRIxPTR "\n",
            objectDataAddr);
    size_t matched = 0;
    for (size_t s = 0; s < kMaxThreads; ++s) {
        auto& tlog = g_perThread[s];
        uint64_t limit = tlog.localSeq.load(std::memory_order_acquire);
        if (limit == 0) continue;
        for (size_t i = 0; i < kPerThreadLogSize; ++i) {
            auto& evt = tlog.events[i];
            uint64_t seq = evt.seq.load(std::memory_order_acquire);
            if (seq + kPerThreadLogSize <= limit) continue; // wrapped: older than window
            if (evt.objectDataAddr != objectDataAddr) continue;
            dumpSweepEventSlot(s, evt, seq);
            ++matched;
        }
    }
    std::fprintf(stderr, "[Diag] sweep-history end matched=%zu\n", matched);
    std::fflush(stderr);
}

// Dump the most-recent `count` events from each thread's buffer. Useful from
// lldb post-crash when you don't know which object's history to ask about.
inline void dumpRecentSweepEvents(size_t count) noexcept {
    if (count == 0) {
        std::fprintf(stderr, "[Diag] recent-events count=0 (no-op)\n");
        std::fflush(stderr);
        return;
    }
    std::fprintf(stderr, "[Diag] recent-events count=%zu per-thread\n", count);
    for (size_t s = 0; s < kMaxThreads; ++s) {
        auto& tlog = g_perThread[s];
        uint64_t limit = tlog.localSeq.load(std::memory_order_acquire);
        if (limit == 0) continue;
        size_t take = count;
        if (take > kPerThreadLogSize) take = kPerThreadLogSize;
        if (take > limit) take = static_cast<size_t>(limit);
        uint64_t start = limit - take;
        std::fprintf(stderr, "[Diag]   thread-slot=%zu window=[%" PRIu64 ", %" PRIu64 ")\n", s, start, limit);
        for (uint64_t seq = start; seq < limit; ++seq) {
            auto& evt = tlog.events[seq % kPerThreadLogSize];
            uint64_t slotSeq = evt.seq.load(std::memory_order_acquire);
            if (slotSeq != seq) continue; // overwritten by newer write
            dumpSweepEventSlot(s, evt, seq);
        }
    }
    std::fprintf(stderr, "[Diag] recent-events end\n");
    std::fflush(stderr);
}

} // namespace kotlin::gc::debug
