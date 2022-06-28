/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "ObjectPtr.hpp"

#import <Foundation/NSObject.h>

using namespace kotlin;

objc_support::internal::ObjectPtrImpl::ObjectPtrImpl() noexcept : object_(nil) {}

objc_support::internal::ObjectPtrImpl::ObjectPtrImpl(NSObject* object) noexcept : object_(object) {}

objc_support::internal::ObjectPtrImpl::ObjectPtrImpl(const ObjectPtrImpl& rhs) noexcept : object_([rhs.object_ retain]) {}

objc_support::internal::ObjectPtrImpl::ObjectPtrImpl(ObjectPtrImpl&& rhs) noexcept : object_(rhs.object_) {
    rhs.object_ = nil;
}

objc_support::internal::ObjectPtrImpl::~ObjectPtrImpl() {
    @autoreleasepool {
        [object_ release];
    }
}

void objc_support::internal::ObjectPtrImpl::swap(ObjectPtrImpl& rhs) noexcept {
    using std::swap;
    swap(object_, rhs.object_);
}

NSObject* objc_support::internal::ObjectPtrImpl::get() const noexcept {
    return object_;
}

bool objc_support::internal::ObjectPtrImpl::valid() const noexcept {
    return object_ != nil;
}

void objc_support::internal::ObjectPtrImpl::reset() noexcept {
    reset(nil);
}

void objc_support::internal::ObjectPtrImpl::reset(NSObject* object) noexcept {
    @autoreleasepool {
        [object_ release];
        object_ = object;
    }
}

std::size_t objc_support::internal::ObjectPtrImpl::computeHash() const noexcept {
    return object_.hash;
}

bool objc_support::internal::ObjectPtrImpl::operator==(const ObjectPtrImpl& rhs) const noexcept {
    return object_ == rhs.object_;
}

bool objc_support::internal::ObjectPtrImpl::operator<(const ObjectPtrImpl& rhs) const noexcept {
    return std::less<>()(object_, rhs.object_);
}

#endif
