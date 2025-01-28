/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

using namespace kotlin;

void BackRefFromAssociatedObject::initForPermanentObject(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(obj->permanent(), "Can only be called with permanent object");
    permanentObj_ = obj;
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(!obj->permanent(), "Can only be called with non-permanent object");
    ref_.construct(obj);
}

bool BackRefFromAssociatedObject::initWithExternalRCRef(mm::RawExternalRCRef* ref) noexcept {
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        permanentObj_ = obj;
        return true;
    }
    ref_.construct(mm::ExternalRCRefImpl::fromRaw(ref));
    return false;
}

void BackRefFromAssociatedObject::addRef() {
    ref_->retain();
}

bool BackRefFromAssociatedObject::tryAddRef() {
    return ref_->tryRetain();
}

void BackRefFromAssociatedObject::releaseRef() {
    ref_->release();
}

void BackRefFromAssociatedObject::dealloc() {
    ref_.destroy();
}

ObjHeader* BackRefFromAssociatedObject::ref() const {
    return **ref_;
}

ObjHeader* BackRefFromAssociatedObject::refPermanent() const {
    return permanentObj_;
}

mm::RawExternalRCRef* BackRefFromAssociatedObject::externalRCRef(bool permanent) const noexcept {
    if (permanent) {
        return mm::permanentObjectAsExternalRCRef(permanentObj_);
    }
    return ref_->get()->toRaw();
}
