/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include <atomic>
#include "Porting.h"
#include "Types.h"

// Marsaglia's xorwow algorithm, matching kotlin.random.XorWowRandom.
namespace {

class XorWowState {
public:
    explicit XorWowState(uint64_t seed) :
        x_(static_cast<uint32_t>(seed)),
        y_(static_cast<uint32_t>(seed >> 32)),
        v_(~x_),
        addend_((x_ << 10) ^ (y_ >> 4)) {
        // x_ and its complement v_ guarantee a nonzero xorshift state.
        // Discard the first 64 outputs, as in XorWowRandom.
        for (int i = 0; i < 64; ++i) nextUInt();
    }

    uint32_t nextUInt() {
        auto t = x_;
        t ^= t >> 2;
        x_ = y_;
        y_ = z_;
        z_ = w_;
        auto v0 = v_;
        w_ = v0;
        t = (t ^ (t << 1)) ^ v0 ^ (v0 << 4);
        v_ = t;
        addend_ += 362437;
        return t + addend_;
    }

private:
    uint32_t x_;
    uint32_t y_;
    uint32_t z_ = 0;
    uint32_t w_ = 0;
    uint32_t v_;
    uint32_t addend_;
};

constexpr uint64_t kSeedIncrement = 0xbb67ae8584caa73b;
std::atomic<uint64_t> seedAllocator{konan::getTimeNanos()};

// Allocate a seed once per thread; subsequent calls only access that thread's state.
thread_local XorWowState tl_state{seedAllocator.fetch_add(kSeedIncrement, std::memory_order_relaxed)};

} // namespace

extern "C" KUInt Kotlin_random_nextRandomUIntTL() {
    return tl_state.nextUInt();
}

extern "C" void Kotlin_random_overrideSeedTL(KULong seed) {
    tl_state = XorWowState(seed);
}
