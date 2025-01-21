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

// TODO(KT-67741): Unify different SpecialRefs

// Reference from an ObjC associated object back into a Kotlin object.
// GC automatically tracks references with refcount > 0 as roots, and invalidates references with refcount = 0 when the Kotlin object is
// collected. Use `create` and `dispose` to create and destroy the back reference.
class ObjCBackRef : private MoveOnly {
public:
    ObjCBackRef() noexcept = default;

    // Cast raw ref into a back reference.
    explicit ObjCBackRef(RawExternalRCRefNonPermanent* raw) noexcept : raw_(raw) {}

    // Cast back reference into a raw ref
    [[nodiscard("must be manually disposed")]] explicit operator RawExternalRCRefNonPermanent*() && noexcept {
        // Make sure to move out from raw_.
        auto raw = std::move(raw_);
        return static_cast<RawExternalRCRefNonPermanent*>(raw);
    }

    // Create new back reference for `obj`.
    [[nodiscard("must be manually disposed")]] static ObjCBackRef create(ObjHeader* obj) noexcept {
        RuntimeAssert(!obj || !obj->permanent(), "ObjCBackRef only works with non-permanent objects");
        return ObjCBackRef(mm::externalRCRefNonPermanent(mm::createRetainedExternalRCRef(obj)));
    }

    // Dispose back reference.
    void dispose() && noexcept {
        // Make sure to move out from raw_.
        auto raw = std::move(raw_);
        disposeExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw));
    }

    // Increment refcount.
    void retain() noexcept {
        // NOTE: In objc import if KtClass inherits from ObjCClass
        // calling [self retain] inside [ObjCClass dealloc] will lead to
        // this->retain() being called after this->dispose()
        retainExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw_));
    }

    // Decrement refcount.
    void release() noexcept {
        // NOTE: In objc import if KtClass inherits from ObjCClass
        // calling [self release] inside [ObjCClass dealloc] will lead to
        // this->release() being called after this->dispose()
        releaseExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw_));
    }

    // Try incrementing refcount. Will fail if the underlying object is not alive.
    // Must be called in the runnable state.
    [[nodiscard("refcount change must be processed")]] bool tryRetain() noexcept {
        AssertThreadState(ThreadState::kRunnable);
        // NOTE: In objc export if ObjCClass is objc_setAssociatedObject with KtClass
        // calling [KtClass _tryRetain] inside [ObjCClass dealloc] will lead to
        // this->tryRetain() being called after this->dispose()
        ObjHolder holder;
        if (mm::tryRefExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw_), holder.slot())) {
            mm::retainExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw_));
            return true;
        }
        return false;
    }

    // Get the underlying object.
    // The result is only safe to use only with refcount > 0.
    [[nodiscard("expensive pure function")]] ObjHeader* operator*() const noexcept {
        // NOTE: In objc import if KtClass inherits from ObjCClass
        // calling [self retain] inside [ObjCClass dealloc] and then passing the retained
        // reference back to Kotlin will lead to
        // this->operator*() being called after this->dispose()
        return mm::dereferenceExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw_));
    }

    static ObjCBackRef& reinterpret(RawExternalRCRefNonPermanent*& raw) noexcept { return reinterpret_cast<ObjCBackRef&>(raw); }

    static const ObjCBackRef& reinterpret(RawExternalRCRefNonPermanent* const& raw) noexcept { return reinterpret_cast<const ObjCBackRef&>(raw); }

private:
    raw_ptr<RawExternalRCRefNonPermanent> raw_;
};

static_assert(sizeof(ObjCBackRef) == sizeof(void*), "ObjCBackRef must be a thin wrapper around pointer");
static_assert(alignof(ObjCBackRef) == alignof(void*), "ObjCBackRef must be a thin wrapper around pointer");
static_assert(
        std::is_trivially_destructible_v<ObjCBackRef>, "ObjCBackRef must be trivially destructible. Destruction is manual via dispose()");

} // namespace kotlin::mm
