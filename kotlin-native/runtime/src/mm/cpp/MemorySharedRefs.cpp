/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

using namespace kotlin;

BackRefFromAssociatedObject::~BackRefFromAssociatedObject() {
    if (!permanent) {
        ref_.destroy();
    }
}

void BackRefFromAssociatedObject::initForPermanentObject(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(obj->permanent(), "Can only be called with permanent object");
    permanentObj_ = obj;
    permanent = true;
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(!obj->permanent(), "Can only be called with non-permanent object");
    ref_.construct(obj);
    permanent = false;
}

void BackRefFromAssociatedObject::initWithExternalRCRef(mm::RawExternalRCRef* ref) noexcept {
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        permanentObj_ = obj;
        permanent = true;
    }
    ref_.construct(mm::ExternalRCRefImpl::fromRaw(ref));
    permanent = false;
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

ObjHeader* BackRefFromAssociatedObject::ref() const {
    if (permanent) {
        return permanentObj_;
    }
    return **ref_;
}

mm::RawExternalRCRef* BackRefFromAssociatedObject::externalRCRef() const noexcept {
    if (permanent) {
        return mm::permanentObjectAsExternalRCRef(permanentObj_);
    }
    return ref_->get()->toRaw();
}
