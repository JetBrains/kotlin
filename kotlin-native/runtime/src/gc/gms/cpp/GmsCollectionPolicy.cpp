/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GmsCollectionPolicy.hpp"

#include <atomic>
#include <cinttypes>

#include "Allocator.hpp"
#include "Logging.hpp"

using namespace kotlin;

namespace {

// Live ("kept") bytes measured after the last Full collection; the policy baseline. Zero until the
// first Full collection has run.
std::atomic<size_t> oldGenBaselineBytes = 0;
// Live bytes measured after the most recent collection of any scope. Named distinctly from the
// public gc::gms::lastLiveBytes() accessor so that unqualified references inside namespace gms bind
// to this global rather than to the (same-named) function.
std::atomic<size_t> lastLiveBytesValue = 0;

std::atomic<uint64_t> edenCount = 0;
std::atomic<uint64_t> fullCount = 0;

// Runtime-tunable full-collection trigger (percent growth over the old-gen baseline). Initialized to
// the compile-time default; overridable via gc::gms::setFullGrowthTriggerPercent (e.g. from GC.kt).
std::atomic<uint64_t> fullGrowthTriggerPercentValue = gc::gms::kDefaultFullGrowthTriggerPercent;

// Test-only scope override.
std::atomic<bool> forcedScopeActive = false;
std::atomic<gc::CollectionScope> forcedScope = gc::CollectionScope::Full;

// [[maybe_unused]]: only referenced by RuntimeLogInfo, which compiles to nothing when runtime
// logging is disabled, leaving this internal-linkage constant otherwise unused.
[[maybe_unused]] constexpr auto kTagGC = logging::Tag::kGC;

} // namespace

gc::CollectionScope gc::gms::choosePolicyScope() noexcept {
    if (__builtin_expect(forcedScopeActive.load(std::memory_order_relaxed), false)) {
        return forcedScope.load(std::memory_order_relaxed);
    }
    // Eden's remembered-set drain must resolve each recorded slot to its containing object to reject
    // stale slots -- a young owner freed by the concurrent sweep between collections (see
    // ConcurrentMark::seedRememberedSets). A backend that cannot resolve interior pointers (the
    // legacy/std allocator) has no such filter, so running Eden there risks dereferencing a stale slot
    // (a wild mark-word CAS -- the afd34b6 crash class). Fall back to Full-only; Full re-traces the
    // whole heap and needs no remembered-set drain. Correctness on the non-default backend outweighs
    // the lost generational win. (Tests can still force Eden explicitly, handled above.)
    if (!alloc::heapLayoutResolvesInteriorPointers()) {
        return CollectionScope::Full;
    }
    size_t base = oldGenBaselineBytes.load(std::memory_order_relaxed);
    if (base == 0) {
        // No baseline yet: run a Full collection to establish one.
        return CollectionScope::Full;
    }
    size_t live = lastLiveBytesValue.load(std::memory_order_relaxed);
    size_t trigger = base + base * fullGrowthTriggerPercentValue.load(std::memory_order_relaxed) / 100;
    if (live > trigger) {
        return CollectionScope::Full;
    }
    return CollectionScope::Eden;
}

void gc::gms::setForcedScopeForTests(bool has, CollectionScope scope) noexcept {
    forcedScope.store(scope, std::memory_order_relaxed);
    forcedScopeActive.store(has, std::memory_order_relaxed);
}

void gc::gms::resetForTests() noexcept {
    oldGenBaselineBytes.store(0, std::memory_order_relaxed);
    lastLiveBytesValue.store(0, std::memory_order_relaxed);
    edenCount.store(0, std::memory_order_relaxed);
    fullCount.store(0, std::memory_order_relaxed);
    fullGrowthTriggerPercentValue.store(kDefaultFullGrowthTriggerPercent, std::memory_order_relaxed);
    forcedScope.store(CollectionScope::Full, std::memory_order_relaxed);
    forcedScopeActive.store(false, std::memory_order_relaxed);
}

void gc::gms::onCollectionFinished(CollectionScope scope, size_t keptBytes) noexcept {
    lastLiveBytesValue.store(keptBytes, std::memory_order_relaxed);
    if (scope == CollectionScope::Full) {
        oldGenBaselineBytes.store(keptBytes, std::memory_order_relaxed);
        auto n = fullCount.fetch_add(1, std::memory_order_relaxed) + 1;
        RuntimeLogInfo({kTagGC}, "GMS Full collection #%" PRIu64 " finished: live=%zu bytes (new old-gen baseline)", n, keptBytes);
    } else {
        auto n = edenCount.fetch_add(1, std::memory_order_relaxed) + 1;
        RuntimeLogInfo(
                {kTagGC}, "GMS Eden collection #%" PRIu64 " finished: live=%zu bytes (old-gen baseline=%zu)", n, keptBytes,
                oldGenBaselineBytes.load(std::memory_order_relaxed));
    }
}

uint64_t gc::gms::edenCollectionCount() noexcept {
    return edenCount.load(std::memory_order_relaxed);
}

uint64_t gc::gms::fullCollectionCount() noexcept {
    return fullCount.load(std::memory_order_relaxed);
}

size_t gc::gms::oldGenerationBaselineBytes() noexcept {
    return oldGenBaselineBytes.load(std::memory_order_relaxed);
}

size_t gc::gms::lastLiveBytes() noexcept {
    return lastLiveBytesValue.load(std::memory_order_relaxed);
}

uint64_t gc::gms::fullGrowthTriggerPercent() noexcept {
    return fullGrowthTriggerPercentValue.load(std::memory_order_relaxed);
}

void gc::gms::setFullGrowthTriggerPercent(uint64_t percent) noexcept {
    // Clamp to a sane floor: 0% would force a Full every cycle (Eden never runs). Callers pass a
    // percentage; there is no upper bound (a very large value simply makes Full collections rarer).
    fullGrowthTriggerPercentValue.store(percent == 0 ? 1 : percent, std::memory_order_relaxed);
}
