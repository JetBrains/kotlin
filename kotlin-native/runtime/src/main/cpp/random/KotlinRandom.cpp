/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include <atomic>
#include "Porting.h"
#include "Types.h"

// The default implementation of pseudo-random generator using the linear congruential generator.

namespace {

constexpr uint64_t MULTIPLIER = 0x5deece66d;
constexpr uint64_t INCREMENT = 0xb;
constexpr auto MODULUS = 48;
constexpr uint64_t MASK = (uint64_t{1} << MODULUS) - 1;

constexpr uint64_t SEED_INCREMENT = 0xbb67ae8584caa73bL;
std::atomic<uint64_t> seedAllocator{konan::getTimeNanos()};

uint64_t inline state_from_seed(uint64_t seed) {
    return (seed ^ MULTIPLIER) & MASK;
}

// Atomic seed allocator with a fixed odd increment is used to calculate the initial value of the thread local tl_state.
thread_local uint64_t tl_state = state_from_seed(seedAllocator.fetch_add(SEED_INCREMENT, std::memory_order_relaxed));

} // namespace

extern "C" {

KULong Kotlin_random_nextRandomULongTL() {
    auto next_state = (tl_state * MULTIPLIER + INCREMENT) & MASK;
    tl_state = next_state;
    return next_state;
}

void Kotlin_random_overrideSeedTL(KULong seed) {
    tl_state = state_from_seed(seed);
}

}
