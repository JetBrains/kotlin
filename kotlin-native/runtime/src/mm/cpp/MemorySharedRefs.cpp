/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include "ExternalRCRef.hpp"

using namespace kotlin;

void BackRefFromAssociatedObject::initForPermanentObject(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(obj->permanent(), "Can only be called with permanent object");
    ref_.emplace<PermanentRef>(obj);
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(!obj->permanent(), "Can only be called with non-permanent object");
    ref_.emplace<RegularRef>(obj);
}

void BackRefFromAssociatedObject::initWithExternalRCRef(mm::RawExternalRCRef* ref) noexcept {
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        ref_.emplace<PermanentRef>(obj);
    }
    ref_.emplace<RegularRef>(mm::ExternalRCRefImpl::fromRaw(ref));
}

void BackRefFromAssociatedObject::addRef() {
    if (auto* ref = std::get_if<RegularRef>(&ref_)) {
        ref->retain();
    }
}

bool BackRefFromAssociatedObject::tryAddRef() {
    if (auto* ref = std::get_if<RegularRef>(&ref_)) {
        return ref->tryRetain();
    }
    return false;
}

void BackRefFromAssociatedObject::releaseRef() {
    if (auto* ref = std::get_if<RegularRef>(&ref_)) {
        ref->release();
    }
}

ObjHeader* BackRefFromAssociatedObject::ref() const {
    return std::visit([](auto&& arg) noexcept -> KRef {
        using T = std::decay_t<decltype(arg)>;
        if constexpr (std::is_same_v<T, PermanentRef>) {
            return arg;
        } else if constexpr (std::is_same_v<T, RegularRef>) {
            return *arg;
        }
    }, ref_);
}

mm::RawExternalRCRef* BackRefFromAssociatedObject::externalRCRef() const noexcept {
    return std::visit([](auto&& arg) noexcept -> mm::RawExternalRCRef*{
        using T = std::decay_t<decltype(arg)>;
        if constexpr (std::is_same_v<T, PermanentRef>) {
            return mm::permanentObjectAsExternalRCRef(arg);
        } else if constexpr (std::is_same_v<T, RegularRef>) {
            return arg.get()->toRaw();
        }
    }, ref_);
}
