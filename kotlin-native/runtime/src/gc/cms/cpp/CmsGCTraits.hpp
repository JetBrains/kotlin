#pragma once

#include "ConcurrentMark.hpp"

namespace kotlin::gc::internal {

struct CmsGCTraits {
    static constexpr auto kName = "Concurrent Mark & Sweep";
    static constexpr bool kConcurrentSweep = true;
    using Mark = mark::ConcurrentMark;
};

}