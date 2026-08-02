#pragma once

#include <cstddef>

#include "CollectionScope.hpp"
#include "SingleThreadMark.hpp"

namespace kotlin::gc::internal {

struct StwmsGCTraits {
    static constexpr auto kName = "Stop-the-world Mark & Sweep";
    static constexpr bool kConcurrentSweep = false;
    static constexpr bool kGenerational = false;
    using Mark = SingleThreadMark;

    static gc::CollectionScope onCollectionStart(gc::CollectionScope scope) noexcept { return scope; }
    static void onCollectionFinish(gc::CollectionScope, size_t) noexcept {}
};

} // namespace kotlin::gc::internal
