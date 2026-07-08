#pragma once

#include <cstddef>

#include "CollectionScope.hpp"
#include "ParallelMark.hpp"

namespace kotlin::gc::internal {

struct PmcsGCTraits {
    static constexpr auto kName = "Parallel Mark & Concurrent Sweep";
    static constexpr bool kConcurrentSweep = true;
    static constexpr bool kGenerational = false;
    using Mark = mark::ParallelMark;

    static gc::CollectionScope onCollectionStart(gc::CollectionScope scope) noexcept { return scope; }
    static void onCollectionFinish(gc::CollectionScope, size_t) noexcept {}
};

} // namespace kotlin::gc::internal
