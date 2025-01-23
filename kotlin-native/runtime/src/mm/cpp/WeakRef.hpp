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

// TODO(KT-67741): Unify different ExternalRCRefs

// Weak reference to a Kotlin object.
// GC automatically invalidates the reference when the Kotlin object is collected.
// Use `create` and `dispose` to create and destroy the weak reference.
class WeakRef : private MoveOnly {
public:
    WeakRef() noexcept = default;

    // Cast raw ref into a weak reference.
    explicit WeakRef(ExternalRCRefImpl* raw) noexcept : ref_(raw) {}

    // Cast weak reference into raw ref.
    [[nodiscard("must be manually disposed")]] explicit operator ExternalRCRefImpl*() && noexcept {
        // Make sure to move out from ref_.
        auto ref = std::move(ref_);
        return static_cast<ExternalRCRefImpl*>(ref);
    }

    // Create new weak reference for `obj`.
    [[nodiscard("must be manually disposed")]] static WeakRef create(ObjHeader* obj) noexcept;

    // Dispose weak reference.
    void dispose() && noexcept {
        RuntimeAssert(ref_, "Disposing null WeakRef");
        // Make sure to move out from ref_.
        auto ref = std::move(ref_);
        // Can be safely called with any thread state.
        ref->dispose();
    }

    // Safely dereference weak reference. Returns null if the underlying object
    // is not alive.
    OBJ_GETTER0(tryRef) const noexcept {
        RuntimeAssert(ref_, "tryRef on null WeakRef");
        AssertThreadState(ThreadState::kRunnable);
        RETURN_RESULT_OF0(ref_->tryRef);
    }

    static WeakRef& reinterpret(ExternalRCRefImpl*& raw) noexcept { return reinterpret_cast<WeakRef&>(raw); }

    static const WeakRef& reinterpret(ExternalRCRefImpl* const& raw) noexcept { return reinterpret_cast<const WeakRef&>(raw); }

private:
    raw_ptr<ExternalRCRefImpl> ref_;
};

static_assert(sizeof(WeakRef) == sizeof(void*), "WeakRef must be a thin wrapper around pointer");
static_assert(alignof(WeakRef) == alignof(void*), "WeakRef must be a thin wrapper around pointer");
static_assert(std::is_trivially_destructible_v<WeakRef>, "WeakRef must be trivially destructible. Destruction is manual via dispose()");

} // namespace kotlin::mm
