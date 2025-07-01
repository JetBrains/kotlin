#pragma once

#include "SingleThreadMark.hpp"

namespace kotlin::gc::internal {

struct StwmsGCTraits {
    static constexpr auto kName = "Stop-the-world Mark & Sweep";
    static constexpr bool kConcurrentSweep = false;
    using Mark = SingleThreadMark;
};

}