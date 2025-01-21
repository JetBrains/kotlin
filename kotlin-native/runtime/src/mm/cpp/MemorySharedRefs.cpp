/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include <shared_mutex>

using namespace kotlin;

void KRefSharedHolder::initLocal(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = nullptr;
    obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = mm::createRetainedExternalRCRef(obj);
    obj_ = obj;
}

ObjHeader* KRefSharedHolder::ref() const {
    AssertThreadState(ThreadState::kRunnable);
    // ref_ may be null if created with initLocal.
    return obj_;
}

void KRefSharedHolder::dispose() {
    // Handles the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    if (!ref_) {
        return;
    }
    auto ref = std::move(ref_);
    mm::releaseAndDisposeExternalRCRef(static_cast<mm::RawExternalRCRef*>(ref));
    // obj_ is dangling now.
}

void BackRefFromAssociatedObject::initForPermanentObject(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(obj->permanent(), "Can only be called with permanent object");
    permanentObj_ = obj;
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(!obj->permanent(), "Can only be called with non-permanent object");
    ref_ = mm::ObjCBackRef::create(obj);
    deallocMutex_.construct();
}

bool BackRefFromAssociatedObject::initWithExternalRCRef(mm::RawExternalRCRef* ref) noexcept {
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        permanentObj_ = obj;
        return true;
    }
    ref_ = mm::ObjCBackRef(mm::externalRCRefNonPermanent(ref));
    deallocMutex_.construct();
    return false;
}

void BackRefFromAssociatedObject::addRef() {
    ref_.retain();
}

bool BackRefFromAssociatedObject::tryAddRef() {
    // Only this method can be called in parallel with dealloc.
    std::shared_lock guard(*deallocMutex_, std::try_to_lock);
    if (!guard) {
        // That means `dealloc` is running in parallel, so
        // cannot possibly retain.
        return false;
    }
    CalledFromNativeGuard threadStateGuard;
    return ref_.tryRetain();
}

void BackRefFromAssociatedObject::releaseRef() {
    ref_.release();
}

void BackRefFromAssociatedObject::dealloc() {
    // This will wait for all `tryAddRef` to finish.
    std::unique_lock guard(*deallocMutex_);
    std::move(ref_).dispose();
}

ObjHeader* BackRefFromAssociatedObject::ref() const {
    return *ref_;
}

ObjHeader* BackRefFromAssociatedObject::refPermanent() const {
    return permanentObj_;
}

mm::RawExternalRCRef* BackRefFromAssociatedObject::externalRCRef(bool permanent) const noexcept {
    if (permanent) {
        return mm::permanentObjectAsExternalRCRef(permanentObj_);
    }
    return externalRCRef(static_cast<mm::RawExternalRCRefNonPermanent*>(ref_));
}
