/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <shared_mutex>

#include "ExternalRCRef.hpp"
#include "Memory.h"
#include "RawPtr.hpp"
#include "Utils.hpp"
#include "concurrent/Mutex.hpp"

namespace kotlin::mm {

// Reference from an ObjC associated object back into a Kotlin object.
// GC automatically tracks references with refcount > 0 as roots, and invalidates references with refcount = 0 when the Kotlin object is
// collected. Use `create` and `dispose` to create and destroy the back reference.
class ObjCBackRef : private Pinned {
public:
    ObjCBackRef() noexcept = default;

    // Cast raw ref into a back reference.
    explicit ObjCBackRef(RawExternalRCRefNonPermanent* raw) noexcept : raw_(raw) {}

    // Create new retained back reference for `obj`.
    explicit ObjCBackRef(KRef obj) noexcept : raw_(mm::externalRCRefNonPermanent(mm::createRetainedExternalRCRef(obj))) {}

    ~ObjCBackRef() {
        // This will wait for all `tryRetain` to finish.
        std::unique_lock guard(deallocMutex_);
        disposeExternalRCRef(static_cast<RawExternalRCRefNonPermanent*>(raw_));
    }

    // Cast back reference into a raw ref
    explicit operator RawExternalRCRefNonPermanent*() const noexcept {
        return static_cast<RawExternalRCRefNonPermanent*>(raw_);
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
    [[nodiscard("refcount change must be processed")]] bool tryRetain() noexcept {
        // NOTE: In objc export if ObjCClass is objc_setAssociatedObject with KtClass
        // calling [KtClass _tryRetain] inside [ObjCClass dealloc] will lead to
        // this->tryRetain() being called after this->dispose()

        // Only this method can be called in parallel with `dispose`.
        std::shared_lock guard(deallocMutex_, std::try_to_lock);
        if (!guard) {
            // That means `dispose` is running in parallel, so
            // cannot possibly retain.
            return false;
        }

        CalledFromNativeGuard threadStateGuard(/* reentrant= */ true);
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

private:
    raw_ptr<RawExternalRCRefNonPermanent> raw_;
    kotlin::RWSpinLock deallocMutex_;
};

} // namespace kotlin::mm
