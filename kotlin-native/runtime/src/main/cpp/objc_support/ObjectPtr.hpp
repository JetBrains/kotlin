/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#if defined(__has_feature) && __has_feature(objc_arc)
#error "Assumes that ARC is not used"
#endif

#include <CoreFoundation/CFBase.h>
#include <functional>
#include <utility>

#include "ObjCForward.hpp"

OBJC_FORWARD_DECLARE(NSObject);

namespace kotlin::objc_support {

struct object_ptr_retain_t {};
inline constexpr object_ptr_retain_t object_ptr_retain{};

namespace internal {

class NSObjectPtrImpl {
public:
    NSObjectPtrImpl() noexcept;
    explicit NSObjectPtrImpl(NSObject* object) noexcept;
    NSObjectPtrImpl(object_ptr_retain_t, NSObject* object) noexcept;

    NSObjectPtrImpl(const NSObjectPtrImpl& rhs) noexcept;
    NSObjectPtrImpl(NSObjectPtrImpl&& rhs) noexcept;

    ~NSObjectPtrImpl();

    NSObjectPtrImpl& operator=(const NSObjectPtrImpl& rhs) noexcept {
        NSObjectPtrImpl tmp(rhs);
        swap(tmp);
        return *this;
    }

    NSObjectPtrImpl& operator=(NSObjectPtrImpl&& rhs) noexcept {
        NSObjectPtrImpl tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    void swap(NSObjectPtrImpl& rhs) noexcept;

    NSObject* get() const noexcept;
    bool valid() const noexcept;

    void reset() noexcept;
    void reset(NSObject* object) noexcept;
    void reset(object_ptr_retain_t, NSObject* object) noexcept;

    bool operator==(const NSObjectPtrImpl& rhs) const noexcept;
    bool operator<(const NSObjectPtrImpl& rhs) const noexcept;

private:
    NSObject* object_;
};

class CFObjectPtrImpl {
public:
    CFObjectPtrImpl() noexcept : object_(nullptr) {}
    explicit CFObjectPtrImpl(CFTypeRef object) noexcept : object_(object) {}
    CFObjectPtrImpl(object_ptr_retain_t, CFTypeRef object) noexcept : object_(CFRetain(object)) {}

    CFObjectPtrImpl(const CFObjectPtrImpl& rhs) noexcept : object_(CFRetain(rhs.object_)) {}
    CFObjectPtrImpl(CFObjectPtrImpl&& rhs) noexcept : object_(rhs.object_) { rhs.object_ = nullptr; }

    ~CFObjectPtrImpl();

    CFObjectPtrImpl& operator=(const CFObjectPtrImpl& rhs) noexcept {
        CFObjectPtrImpl tmp(rhs);
        swap(tmp);
        return *this;
    }

    CFObjectPtrImpl& operator=(CFObjectPtrImpl&& rhs) noexcept {
        CFObjectPtrImpl tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    void swap(CFObjectPtrImpl& rhs) noexcept {
        using std::swap;
        swap(object_, rhs.object_);
    }

    CFTypeRef get() const noexcept { return object_; }
    bool valid() const noexcept { return object_ != nullptr; }

    void reset() noexcept { reset(nullptr); }
    void reset(CFTypeRef object) noexcept;
    void reset(object_ptr_retain_t, CFTypeRef object) noexcept { reset(CFRetain(object)); }

    bool operator==(const CFObjectPtrImpl& rhs) const noexcept { return object_ == rhs.object_; }
    bool operator<(const CFObjectPtrImpl& rhs) const noexcept { return std::less<>()(object_, rhs.object_); }

private:
    CFTypeRef object_;
};

} // namespace internal

// `std::shared_ptr`-like smart pointer for ObjC/CF objects.
//
// IMPORTANT: By default constructed from a retained ObjC/CF object.
// This optimizes for the common case of constructing directly from `[[T alloc] init...]`.
// To construct and additionally retain the object, use `object_ptr_retain` as the first argument
// to the constructor.
//
// Unlike a regular C++ smart pointer `operator*` does not return a reference but return a pointer
// for convenient syntax of calling ObjC methods: `[*smartPtr methodName]`.
// Use `(*smartPtr).propertyName` to access properties.
// Use `smartPtr->fieldName` to access fields.
//
// Implements hashes and comparisons making it suitable to store in associative containers.
// Equality comparison is shallow (does not use `[NSObject isEqual:]`) mirroring regular C++ smart pointers.
//
// When transfering ownership, prefer to move because it avoids calling `retain` and `release`.
template <typename T>
class object_ptr {
    // All CF types are known only as CF...Ref which are always pointers.
    static inline constexpr bool useCF = std::is_pointer_v<T>;

public:
    using element_type = std::conditional_t<useCF, std::remove_pointer_t<T>, T>;

    // Construct empty `object_ptr`.
    object_ptr() noexcept = default;

    // Construct from ObjC/CF object.
    //
    // IMPORTANT: this does not `retain` `ptr`.
    explicit object_ptr(element_type* ptr) noexcept : impl_(ptr) {}

    // Construct from ObjC/CF object, additionally retaining `ptr`.
    object_ptr(object_ptr_retain_t, element_type* ptr) noexcept : impl_(object_ptr_retain, ptr) {}

    void swap(object_ptr<T>& rhs) noexcept { impl_.swap(rhs.impl_); }

    element_type* get() const noexcept { return (element_type*)impl_.get(); }
    element_type* operator*() const noexcept { return (element_type*)impl_.get(); }
    element_type* operator->() const noexcept { return (element_type*)impl_.get(); }

    explicit operator bool() const noexcept { return impl_.valid(); }

    // Release stored pointer and become empty.
    void reset() noexcept { impl_.reset(); }

    // Release stored pointer and store `ptr` instead.
    //
    // IMPORTANT: this does not `retain` `ptr`.
    void reset(element_type* ptr) noexcept { impl_.reset(ptr); }

    // Release stored pointer and store `ptr` instead, additionally retaining `ptr`.
    void reset(object_ptr_retain_t, element_type* ptr) noexcept { impl_.reset(object_ptr_retain, ptr); }

    template <typename U>
    bool operator==(const object_ptr<U>& rhs) const noexcept {
        return impl_ == rhs.impl_;
    }

    template <typename U>
    bool operator<(const object_ptr<U>& rhs) const noexcept {
        return impl_ < rhs.impl_;
    }

private:
    friend struct std::hash<object_ptr>;

    template <typename U>
    friend class object_ptr;

    std::conditional_t<useCF, internal::CFObjectPtrImpl, internal::NSObjectPtrImpl> impl_;
};

template <typename T, typename U>
bool operator!=(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return !(lhs == rhs);
}

template <typename T, typename U>
bool operator>(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return rhs < lhs;
}

template <typename T, typename U>
bool operator<=(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return !(lhs > rhs);
}

template <typename T, typename U>
bool operator>=(const object_ptr<T>& lhs, const object_ptr<U>& rhs) noexcept {
    return !(lhs < rhs);
}

} // namespace kotlin::objc_support

namespace std {

template <typename T>
void swap(kotlin::objc_support::object_ptr<T>& lhs, kotlin::objc_support::object_ptr<T>& rhs) noexcept {
    lhs.swap(rhs);
}

template <typename T>
struct hash<kotlin::objc_support::object_ptr<T>> {
    std::size_t operator()(const kotlin::objc_support::object_ptr<T>& value) { return std::hash<const void*>()(value.impl_.get()); }
};

// TODO: std::atomic specialization?

} // namespace std

#endif
