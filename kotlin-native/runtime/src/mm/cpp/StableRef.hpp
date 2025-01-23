/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "ExternalRCRef.hpp"
#include "Memory.h"
#include "RawPtr.hpp"
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
    explicit StableRef(ExternalRCRefImpl* raw) noexcept : ref_(raw) {}

    // Cast stable reference into raw ref.
    [[nodiscard("must be manually disposed")]] explicit operator ExternalRCRefImpl*() && noexcept {
        // Make sure to move out from ref_.
        auto ref = std::move(ref_);
        return static_cast<ExternalRCRefImpl*>(ref);
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
        RuntimeAssert(ref_, "operator* on null StableRef");
        return ref_->ref();
    }

    static StableRef& reinterpret(ExternalRCRefImpl*& raw) noexcept { return reinterpret_cast<StableRef&>(raw); }

    static const StableRef& reinterpret(ExternalRCRefImpl* const& raw) noexcept { return reinterpret_cast<const StableRef&>(raw); }

private:
    raw_ptr<ExternalRCRefImpl> disposeImpl() && noexcept {
        RuntimeAssert(ref_, "Disposing null StableRef");
        // Make sure to move out from ref_.
        auto ref = std::move(ref_);
        // Can be safely called with any thread state.
        ref->releaseRef();
        // Can be safely called with any thread state.
        ref->dispose();
        return ref;
    }

    raw_ptr<ExternalRCRefImpl> ref_;
};

static_assert(sizeof(StableRef) == sizeof(void*), "StableRef must be a thin wrapper around pointer");
static_assert(alignof(StableRef) == alignof(void*), "StableRef must be a thin wrapper around pointer");
static_assert(std::is_trivially_destructible_v<StableRef>, "StableRef must be trivially destructible. Destruction is manual via dispose()");

} // namespace kotlin::mm
