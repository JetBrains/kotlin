#include "Types.h"

namespace {
thread_local uint64_t tl_seed;
uint64_t MULTIPLIER = 0x5deece66d;
}

extern "C" {

void Kotlin_random_seedRandomIntTL(KLong seed) {
    tl_seed = seed;
}

KLong Kotlin_random_nextRandomLongTL() {
    auto seed = tl_seed;
    auto next = (seed * MULTIPLIER + 0xbL) & ((1LL << 48) - 1LL);
    tl_seed = next;
    return next;
}
}
