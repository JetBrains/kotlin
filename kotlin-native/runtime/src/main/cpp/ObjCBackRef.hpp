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
//
// Does not support permanent objects.
//
// See also: [OwningExternalRCRef]
class ObjCBackRef : private Pinned {
public:
    ObjCBackRef() noexcept = default;
    ObjCBackRef(std::nullptr_t) noexcept {}

    // Adopt `ExternalRCRefImpl*` without affecting reference count.
    explicit ObjCBackRef(ExternalRCRefImpl* raw) noexcept : raw_(raw) {}

    // Create new retained `ExternalRCRefImpl` pointing to `obj`.
    explicit ObjCBackRef(KRef obj) noexcept : raw_(&mm::ExternalRCRefImpl::create(obj, 1)) {
        RuntimeAssert(!obj->permanent(), "ObjCBackRef does not support permanent objects");
    }

    ~ObjCBackRef() {
        if (!raw_)
            return;
        // This will wait for all concurrent `tryRetain` calls to finish.
        std::unique_lock guard(deallocMutex_);
        // Can be safely called with any thread state.
        raw_->dispose();
        // Null out to make `ref`, `retain`, `release`, `tryRetain` safe to call after the destructor.
        // This can happen in both objc import and export. See the methods for more explanation.
        raw_ = nullptr;
    }

    mm::ExternalRCRefImpl* get() const noexcept { return static_cast<mm::ExternalRCRefImpl*>(raw_); }

    // Return the underlying object.
    // The result is only safe to use, when reference count is >0, or there is a guarantee
    // that the object is in roots in some other way (e.g. on stack)
    KRef ref() const noexcept {
        // In objc import if KtClass inherits from ObjCClass
        // calling [self retain] inside [ObjCClass dealloc] and then passing the retained
        // reference back to Kotlin will lead to
        // this->ref() being called after this->~ObjCBackRef()
        if (auto ref = raw_) {
            return ref->ref();
        }
        return nullptr;
    }

    KRef operator*() const noexcept { return ref(); }

    // Increment refcount.
    void retain() noexcept {
        // In objc import if KtClass inherits from ObjCClass
        // calling [self retain] inside [ObjCClass dealloc] will lead to
        // this->retain() being called after this->~ObjCBackRef()
        if (auto ref = raw_) {
            // Can be safely called with any thread state.
            ref->retainRef();
        }
    }

    // Decrement refcount.
    void release() noexcept {
        // In objc import if KtClass inherits from ObjCClass
        // calling [self release] inside [ObjCClass dealloc] will lead to
        // this->release() being called after this->~ObjCBackRef()
        if (auto ref = raw_) {
            // Can be safely called with any thread state.
            ref->releaseRef();
        }
    }

    // Try incrementing refcount. Will fail if the underlying object is not alive.
    // Must be called in the native state.
    [[nodiscard("refcount change must be processed")]] bool tryRetain() noexcept {
        // Only this method can be called in parallel with the destructor.
        std::shared_lock guard(deallocMutex_, std::try_to_lock);
        if (!guard) {
            // That means `dealloc` is running in parallel, so cannot possibly retain
            return false;
        }
        CalledFromNativeGuard threadStateGuard;
        // In objc export if ObjCClass is objc_setAssociatedObject with KtClass
        // calling [KtClass _tryRetain] inside [ObjCClass dealloc] will lead to
        // this->tryRetain() being called after this->~ObjCBackRef()
        if (auto ref = raw_) {
            ObjHolder holder;
            if (ref->tryRef(holder.slot())) {
                ref->retainRef();
                return true;
            }
        }
        return false;
    }

private:
    raw_ptr<ExternalRCRefImpl> raw_;
    RWSpinLock deallocMutex_;
};

} // namespace kotlin::mm
