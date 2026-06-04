/*
 * Diagnostic ring buffer for tracing mark/sweep events on PMCS.
 *
 * Intended for confirming or falsifying the consecutive-cycle race hypothesis
 * in the PMCS-only "live FloatArray observed as empty/null" bug. Each event
 * records the object's ObjectData address along with thread id, GC epoch,
 * kind, and a per-event auxiliary payload. The dump function scans the ring
 * for entries matching a given ObjectData address and prints them in seq order.
 *
 * Designed not to mask the bug:
 *   - Only the sweep-side hooks (KEEP / DEAD / RECLAIM) are universal.
 *   - The mark-side hook (CAS_FAIL) fires only on the rare contended path,
 *     never on the bulk mark-success path.
 *
 * Remove before landing.
 */

#pragma once

#include <atomic>
#include <cinttypes>
#include <cstdint>
#include <cstdio>

#include "GCStatistics.hpp"
#include "Porting.h"

namespace kotlin::gc::debug {

enum SweepEventKind : uint32_t {
    kKeep = 0,       // sweep observed next_ != null; mark cleared, object kept alive
    kDead = 1,       // sweep observed next_ == null; object about to be reclaimed
    kCASFail = 2,    // mark CAS failed because next_ was already non-null (possibly stale)
    kReclaim = 3,    // trySweepElement returned true; cell is about to be memset'd
    kMarkOk = 4,     // mark CAS succeeded: this thread transitioned next_ from null to non-null
};

struct SweepEvent {
    std::atomic<uintptr_t> objectDataAddr{0};
    std::atomic<uintptr_t> aux{0};
    std::atomic<uintptr_t> threadId{0};
    std::atomic<uint64_t> epoch{0};
    std::atomic<uint32_t> kind{0};
    std::atomic<uint64_t> seq{0};
};

inline constexpr size_t kSweepLogSize = 1u << 22; // 4M entries, ~192 MB

inline SweepEvent g_sweepLog[kSweepLogSize];
inline std::atomic<uint64_t> g_nextSeq{0};

inline uint64_t currentEpochOrZero() noexcept {
    auto handle = GCHandle::currentEpoch();
    return handle.has_value() ? handle->getEpoch() : 0;
}

inline void recordSweepEvent(uintptr_t objectDataAddr, SweepEventKind kind, uintptr_t aux) noexcept {
    uint64_t seq = g_nextSeq.fetch_add(1, std::memory_order_relaxed);
    auto& slot = g_sweepLog[seq % kSweepLogSize];
    slot.objectDataAddr.store(objectDataAddr, std::memory_order_relaxed);
    slot.aux.store(aux, std::memory_order_relaxed);
    slot.threadId.store(konan::currentThreadId(), std::memory_order_relaxed);
    slot.epoch.store(currentEpochOrZero(), std::memory_order_relaxed);
    slot.kind.store(static_cast<uint32_t>(kind), std::memory_order_relaxed);
    slot.seq.store(seq, std::memory_order_release);
}

inline const char* sweepEventName(uint32_t kind) noexcept {
    switch (kind) {
        case kKeep:    return "KEEP";
        case kDead:    return "DEAD";
        case kCASFail: return "CAS_FAIL";
        case kReclaim: return "RECLAIM";
        case kMarkOk:  return "MARK_OK";
        default:       return "?";
    }
}

inline void dumpSweepEventSlot(const SweepEvent& slot, uint64_t seq) noexcept {
    uint32_t kind = slot.kind.load(std::memory_order_relaxed);
    uintptr_t aux = slot.aux.load(std::memory_order_relaxed);
    uintptr_t threadId = slot.threadId.load(std::memory_order_relaxed);
    uint64_t epoch = slot.epoch.load(std::memory_order_relaxed);
    uintptr_t obj = slot.objectDataAddr.load(std::memory_order_relaxed);
    std::fprintf(
            stderr,
            "[Diag]   seq=%" PRIu64 " epoch=%" PRIu64 " tid=0x%" PRIxPTR " kind=%s obj=0x%" PRIxPTR " aux=0x%" PRIxPTR "\n",
            seq, epoch, threadId, sweepEventName(kind), obj, aux);
}

inline void dumpSweepHistoryForObjectData(uintptr_t objectDataAddr) noexcept {
    uint64_t limit = g_nextSeq.load(std::memory_order_acquire);
    std::fprintf(
            stderr,
            "[Diag] sweep-history objectData=0x%" PRIxPTR " window=[%" PRIu64 ", %" PRIu64 ")\n",
            objectDataAddr,
            limit > kSweepLogSize ? limit - kSweepLogSize : 0,
            limit);
    size_t matched = 0;
    for (size_t i = 0; i < kSweepLogSize; ++i) {
        auto& slot = g_sweepLog[i];
        if (slot.objectDataAddr.load(std::memory_order_relaxed) != objectDataAddr) continue;
        uint64_t seq = slot.seq.load(std::memory_order_acquire);
        if (seq + kSweepLogSize <= limit) continue; // wrapped: entry is older than our window
        dumpSweepEventSlot(slot, seq);
        ++matched;
    }
    std::fprintf(stderr, "[Diag] sweep-history end matched=%zu\n", matched);
    std::fflush(stderr);
}

// Dump the most-recent `count` events (capped at the buffer size). Useful from
// lldb post-crash when you don't know which object's history to ask about.
inline void dumpRecentSweepEvents(size_t count) noexcept {
    uint64_t limit = g_nextSeq.load(std::memory_order_acquire);
    if (count > kSweepLogSize) count = kSweepLogSize;
    if (count > limit) count = static_cast<size_t>(limit);
    uint64_t start = limit - count;
    std::fprintf(
            stderr,
            "[Diag] recent-events window=[%" PRIu64 ", %" PRIu64 ")\n",
            start, limit);
    for (uint64_t seq = start; seq < limit; ++seq) {
        auto& slot = g_sweepLog[seq % kSweepLogSize];
        uint64_t slotSeq = slot.seq.load(std::memory_order_acquire);
        if (slotSeq != seq) continue; // overwritten by a newer write
        dumpSweepEventSlot(slot, seq);
    }
    std::fprintf(stderr, "[Diag] recent-events end\n");
    std::fflush(stderr);
}

} // namespace kotlin::gc::debug
