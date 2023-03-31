/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "RawPtr.hpp"
#include "SpecialRefRegistry.hpp"
#include "Utils.hpp"

namespace kotlin::mm {

// Weak reference to a Kotlin object.
// GC automatically invalidates the reference when the Kotlin object is collected.
// Use `create` and `dispose` to create and destroy the weak reference.
class WeakRef : private MoveOnly {
public:
    WeakRef() noexcept = default;

    // Cast raw ref into a weak reference.
    explicit WeakRef(RawSpecialRef* raw) noexcept : node_(SpecialRefRegistry::Node::fromRaw(raw)) {}

    // Cast weak reference into raw ref.
    [[nodiscard("must be manually disposed")]] explicit operator RawSpecialRef*() && noexcept {
        // Make sure to move out from node_.
        auto node = std::move(node_);
        return node->asRaw();
    }

    // Create new weak reference for `obj`.
    [[nodiscard("must be manually disposed")]] static WeakRef create(ObjHeader* obj) noexcept;

    // Dispose weak reference.
    void dispose() && noexcept {
        RuntimeAssert(node_, "Disposing null WeakRef");
        // Make sure to move out from node_.
        auto node = std::move(node_);
        // Can be safely called with any thread state.
        node->dispose();
    }

    // Safely dereference weak reference. Returns null if the underlying object
    // is not alive.
    OBJ_GETTER0(tryRef) const noexcept {
        RuntimeAssert(node_, "tryRef on null WeakRef");
        AssertThreadState(ThreadState::kRunnable);
        RETURN_RESULT_OF0(node_->tryRef);
    }

    static WeakRef& reinterpret(RawSpecialRef*& raw) noexcept { return reinterpret_cast<WeakRef&>(raw); }

    static const WeakRef& reinterpret(RawSpecialRef* const& raw) noexcept { return reinterpret_cast<const WeakRef&>(raw); }

private:
    raw_ptr<SpecialRefRegistry::Node> node_;
};

static_assert(sizeof(WeakRef) == sizeof(void*), "WeakRef must be a thin wrapper around pointer");
static_assert(alignof(WeakRef) == alignof(void*), "WeakRef must be a thin wrapper around pointer");
static_assert(std::is_trivially_destructible_v<WeakRef>, "WeakRef must be trivially destructible. Destruction is manual via dispose()");

} // namespace kotlin::mm
