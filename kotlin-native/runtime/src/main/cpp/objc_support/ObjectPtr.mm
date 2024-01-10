/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "ObjectPtr.hpp"

#import <Foundation/NSObject.h>

using namespace kotlin;

objc_support::internal::NSObjectPtrImpl::NSObjectPtrImpl() noexcept : object_(nil) {}

objc_support::internal::NSObjectPtrImpl::NSObjectPtrImpl(NSObject* object) noexcept : object_(object) {}

objc_support::internal::NSObjectPtrImpl::NSObjectPtrImpl(object_ptr_retain_t, NSObject* object) noexcept : object_([object retain]) {}

objc_support::internal::NSObjectPtrImpl::NSObjectPtrImpl(const NSObjectPtrImpl& rhs) noexcept : object_([rhs.object_ retain]) {}

objc_support::internal::NSObjectPtrImpl::NSObjectPtrImpl(NSObjectPtrImpl&& rhs) noexcept : object_(rhs.object_) {
    rhs.object_ = nil;
}

objc_support::internal::NSObjectPtrImpl::~NSObjectPtrImpl() {
    @autoreleasepool {
        [object_ release];
    }
}

void objc_support::internal::NSObjectPtrImpl::swap(NSObjectPtrImpl& rhs) noexcept {
    using std::swap;
    swap(object_, rhs.object_);
}

NSObject* objc_support::internal::NSObjectPtrImpl::get() const noexcept {
    return object_;
}

bool objc_support::internal::NSObjectPtrImpl::valid() const noexcept {
    return object_ != nil;
}

void objc_support::internal::NSObjectPtrImpl::reset() noexcept {
    reset(nil);
}

void objc_support::internal::NSObjectPtrImpl::reset(NSObject* object) noexcept {
    @autoreleasepool {
        [object_ release];
        object_ = object;
    }
}

void objc_support::internal::NSObjectPtrImpl::reset(object_ptr_retain_t, NSObject* object) noexcept {
    reset([object retain]);
}

bool objc_support::internal::NSObjectPtrImpl::operator==(const NSObjectPtrImpl& rhs) const noexcept {
    return object_ == rhs.object_;
}

bool objc_support::internal::NSObjectPtrImpl::operator<(const NSObjectPtrImpl& rhs) const noexcept {
    return std::less<>()(object_, rhs.object_);
}

objc_support::internal::CFObjectPtrImpl::~CFObjectPtrImpl() {
    if (object_) {
        @autoreleasepool {
            CFRelease(object_);
        }
    }
}

void objc_support::internal::CFObjectPtrImpl::reset(CFTypeRef object) noexcept {
    if (object_) {
        @autoreleasepool {
            CFRelease(object_);
        }
    }
    object_ = object;
}

#endif
