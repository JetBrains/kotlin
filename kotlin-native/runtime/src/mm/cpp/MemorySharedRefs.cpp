/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include <shared_mutex>

#include "ExternalRCRef.hpp"
#include "ObjCBackRef.hpp"

using namespace kotlin;

void BackRefFromAssociatedObject::initForPermanentObject(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(obj->permanent(), "Can only be called with permanent object");
    permanentObj_ = obj;
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(!obj->permanent(), "Can only be called with non-permanent object");
    ref_ = static_cast<mm::ExternalRCRefImpl*>(mm::ObjCBackRef::create(obj));
    deallocMutex_.construct();
}

bool BackRefFromAssociatedObject::initWithExternalRCRef(mm::RawExternalRCRef* ref) noexcept {
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        permanentObj_ = obj;
        return true;
    }
    ref_ = mm::ExternalRCRefImpl::fromRaw(ref);
    deallocMutex_.construct();
    return false;
}

void BackRefFromAssociatedObject::addRef() {
    mm::ObjCBackRef::reinterpret(ref_).retain();
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
    return mm::ObjCBackRef::reinterpret(ref_).tryRetain();
}

void BackRefFromAssociatedObject::releaseRef() {
    mm::ObjCBackRef::reinterpret(ref_).release();
}

void BackRefFromAssociatedObject::dealloc() {
    // This will wait for all `tryAddRef` to finish.
    std::unique_lock guard(*deallocMutex_);
    std::move(mm::ObjCBackRef::reinterpret(ref_)).dispose();
}

ObjHeader* BackRefFromAssociatedObject::ref() const {
    return *mm::ObjCBackRef::reinterpret(ref_);
}

ObjHeader* BackRefFromAssociatedObject::refPermanent() const {
    return permanentObj_;
}

mm::RawExternalRCRef* BackRefFromAssociatedObject::externalRCRef(bool permanent) const noexcept {
    if (permanent) {
        return mm::permanentObjectAsExternalRCRef(permanentObj_);
    }
    return ref_->toRaw();
}
