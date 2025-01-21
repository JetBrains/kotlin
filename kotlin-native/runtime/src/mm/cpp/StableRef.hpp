/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ExternalRCRef.hpp"
#include "RawPtr.hpp"
#include "Types.h"
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
    explicit StableRef(RawExternalRCRef* raw) noexcept : raw_(raw) {}

    // Cast stable reference into raw ref.
    explicit operator RawExternalRCRef*() noexcept {
        return static_cast<RawExternalRCRef*>(raw_);
    }

    // Create new stable reference for `obj`.
    [[nodiscard("must be manually disposed")]] static StableRef create(KRef obj) noexcept {
        return StableRef(createRetainedExternalRCRef(obj));
    }

    // Dispose stable reference.
    void dispose() && noexcept {
        // Make sure to move out of the field.
        auto raw = std::move(raw_);
        mm::releaseAndDisposeExternalRCRef(static_cast<RawExternalRCRef*>(raw));
    }

    // Get the underlying object.
    // Always safe, because the object is guaranteed to be in the root set.
    [[nodiscard("expensive pure function")]] ObjHeader* operator*() const noexcept {
        return dereferenceExternalRCRef(static_cast<mm::RawExternalRCRef*>(raw_));
    }

    static StableRef& reinterpret(RawExternalRCRef*& raw) noexcept { return reinterpret_cast<StableRef&>(raw); }

    static const StableRef& reinterpret(RawExternalRCRef* const& raw) noexcept { return reinterpret_cast<const StableRef&>(raw); }

private:
    raw_ptr<RawExternalRCRef> raw_;
};

static_assert(sizeof(StableRef) == sizeof(void*), "StableRef must be a thin wrapper around pointer");
static_assert(alignof(StableRef) == alignof(void*), "StableRef must be a thin wrapper around pointer");
static_assert(std::is_trivially_destructible_v<StableRef>, "StableRef must be trivially destructible. Destruction is manual via dispose()");

} // namespace kotlin::mm
