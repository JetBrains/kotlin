#pragma once

#include <cstddef>

#include "CollectionScope.hpp"
#include "ConcurrentMark.hpp"

namespace kotlin::gc::internal {

struct CmsGCTraits {
    static constexpr auto kName = "Concurrent Mark & Sweep";
    static constexpr bool kConcurrentSweep = true;
    static constexpr bool kGenerational = false;
    using Mark = mark::ConcurrentMark;

    static gc::CollectionScope onCollectionStart(gc::CollectionScope scope) noexcept { return scope; }
    static void onCollectionFinish(gc::CollectionScope, size_t) noexcept {}
};

} // namespace kotlin::gc::internal
