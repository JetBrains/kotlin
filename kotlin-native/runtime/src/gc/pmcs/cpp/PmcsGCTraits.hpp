#pragma once

#include "ParallelMark.hpp"

namespace kotlin::gc::internal {

struct PmcsGCTraits {
    static constexpr auto kName = "Parallel Mark & Concurrent Sweep";
    static constexpr bool kConcurrentSweep = true;
    using Mark = mark::ParallelMark;
};

}