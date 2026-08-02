/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

namespace kotlin::gc {

// The kind of a garbage collection cycle for generational collectors.
//
// - Full: a major collection. All mark state is (logically) cleared, the whole live heap is
//   re-traced from roots, and the whole heap is swept. Non-generational collectors always run Full.
// - Eden: a minor collection. Survivors of the previous collection are treated as implicitly live
//   ("old", via sticky marks) and are not re-traced; only objects allocated since the last collection
//   ("young") plus objects reachable from the remembered set (old->young edges recorded by the write
//   barrier) are traced, and only eden pages are swept.
//
// This mirrors WebKit JavaScriptCore's CollectionScope. It is meaningful only for generational
// collectors (GCTraits::kGenerational == true); others always use Full.
enum class CollectionScope {
    Eden,
    Full,
};

inline const char* toString(CollectionScope scope) noexcept {
    switch (scope) {
        case CollectionScope::Eden:
            return "Eden";
        case CollectionScope::Full:
            return "Full";
    }
    return "Unknown";
}

} // namespace kotlin::gc
