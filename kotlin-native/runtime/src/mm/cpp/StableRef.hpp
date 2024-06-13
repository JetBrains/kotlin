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

class ThreadData;

// TODO(KT-67741): Unify different SpecialRefs

// Stable reference to a Kotlin object.
// Every stable reference makes Kotlin object be in the root set.
// Use `create` and `dispose` to create and destroy the stable reference.
class StableRef : private MoveOnly {
public:
    StableRef() noexcept = default;

    // Cast raw ref into a stable reference.
    explicit StableRef(RawSpecialRef* raw) noexcept : node_(SpecialRefRegistry::Node::fromRaw(raw)) {}

    // Cast stable reference into raw ref.
    [[nodiscard("must be manually disposed")]] explicit operator RawSpecialRef*() && noexcept {
        // Make sure to move out from node_.
        auto node = std::move(node_);
        return node->asRaw();
    }

    // Create new stable reference for `obj`.
    [[nodiscard("must be manually disposed")]] static StableRef create(ObjHeader* obj) noexcept;

    // Dispose stable reference.
    void dispose() && noexcept {
        std::move(*this).disposeImpl();
    }

    // Get the underlying object.
    // Always safe, because the object is guaranteed to be in the root set.
    [[nodiscard("expensive pure function")]] ObjHeader* operator*() const noexcept {
        RuntimeAssert(node_, "operator* on null StableRef");
        return node_->ref();
    }

    static StableRef& reinterpret(RawSpecialRef*& raw) noexcept { return reinterpret_cast<StableRef&>(raw); }

    static const StableRef& reinterpret(RawSpecialRef* const& raw) noexcept { return reinterpret_cast<const StableRef&>(raw); }

private:
    raw_ptr<SpecialRefRegistry::Node> disposeImpl() && noexcept {
        RuntimeAssert(node_, "Disposing null StableRef");
        // Make sure to move out from node_.
        auto node = std::move(node_);
        // Can be safely called with any thread state.
        node->releaseRef();
        // Can be safely called with any thread state.
        node->dispose();
        return node;
    }

    raw_ptr<SpecialRefRegistry::Node> node_;
};

static_assert(sizeof(StableRef) == sizeof(void*), "StableRef must be a thin wrapper around pointer");
static_assert(alignof(StableRef) == alignof(void*), "StableRef must be a thin wrapper around pointer");
static_assert(std::is_trivially_destructible_v<StableRef>, "StableRef must be trivially destructible. Destruction is manual via dispose()");

} // namespace kotlin::mm
