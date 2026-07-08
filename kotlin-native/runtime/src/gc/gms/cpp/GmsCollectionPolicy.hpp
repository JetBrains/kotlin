/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <cstdint>

#include "CollectionScope.hpp"

namespace kotlin::gc::gms {

// Generational collection policy and per-scope statistics for the gms collector. Process-global
// (there is one GC per process).
//
// Policy: run a Full collection to (re)establish a baseline of live "old" bytes, then run Eden
// collections until the live heap has grown past that baseline by `kFullGrowthTriggerPercent`, at
// which point a Full collection runs again. Growth comes from promoted survivors and from floating
// garbage that Eden cannot reclaim, so this bounds floating garbage while keeping Full collections
// infrequent. Mirrors the intent of JSC's minEdenToOldGenerationRatio (~1/3).
//
// Full is also forced out-of-band on remembered-set overflow and explicit GC.collect() (handled via
// gc::barriers::requestFullCollection / takeFullCollectionRequest), independently of this heuristic.
//
// The percentage is a runtime knob (default below), overridable via setFullGrowthTriggerPercent so the
// knee can be tuned per workload without recompiling. It is surfaced to Kotlin through GC.kt.
inline constexpr uint64_t kDefaultFullGrowthTriggerPercent = 33;

// The live-heap growth (percent over the post-Full baseline) that triggers the next Full collection.
uint64_t fullGrowthTriggerPercent() noexcept;
void setFullGrowthTriggerPercent(uint64_t percent) noexcept;

// Chooses Eden or Full for the next collection from the current heap-growth state.
CollectionScope choosePolicyScope() noexcept;

// Records a finished collection's live ("kept") byte count; updates the policy baseline and stats.
void onCollectionFinished(CollectionScope scope, size_t keptBytes) noexcept;

// --- Statistics (for observability / tests) ---
uint64_t edenCollectionCount() noexcept;
uint64_t fullCollectionCount() noexcept;
size_t oldGenerationBaselineBytes() noexcept; // live bytes measured after the last Full collection
size_t lastLiveBytes() noexcept; // live bytes measured after the last collection

// --- Test support ---
// Pins the scope returned by choosePolicyScope() so tests can drive deterministic Eden/Full
// collections. `has == false` restores the heap-growth heuristic. Not for production use.
void setForcedScopeForTests(bool has, CollectionScope scope) noexcept;

// Resets the heap-growth baseline and per-scope counters to their initial (process-start) state, so
// each test starts from a clean policy (baseline == 0 -> the first collection is Full). The policy is
// process-global, so without this it would leak across tests in a single test binary. Not for
// production use.
void resetForTests() noexcept;

} // namespace kotlin::gc::gms
