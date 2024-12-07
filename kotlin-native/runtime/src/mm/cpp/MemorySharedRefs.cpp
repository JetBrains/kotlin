/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include <shared_mutex>

#include "ExternalRCRef.hpp"
#include "ObjCBackRef.hpp"
#include "StableRef.hpp"

using namespace kotlin;

void KRefSharedHolder::initLocal(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = nullptr;
    obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = static_cast<mm::RawSpecialRef*>(mm::StableRef::create(obj));
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
    std::move(mm::StableRef::reinterpret(ref_)).dispose();
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
    ref_ = static_cast<mm::RawSpecialRef*>(mm::ObjCBackRef::create(obj));
    deallocMutex_.construct();
}

bool BackRefFromAssociatedObject::initWithExternalRCRef(void* ref) noexcept {
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        permanentObj_ = obj;
        return true;
    }
    ref_ = static_cast<mm::RawSpecialRef*>(ref);
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

void* BackRefFromAssociatedObject::externalRCRef(bool permanent) const noexcept {
    if (permanent) {
        return mm::permanentObjectAsExternalRCRef(permanentObj_);
    }
    return ref_;
}
