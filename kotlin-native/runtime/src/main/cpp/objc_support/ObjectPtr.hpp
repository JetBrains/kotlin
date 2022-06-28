/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#if defined(__has_feature) && __has_feature(objc_arc)
#error "Assumes that ARC is not used"
#endif

#include <functional>
#include <utility>

#include "ObjCForward.hpp"

OBJC_FORWARD_DECLARE(NSObject);

namespace kotlin::objc_support {

namespace internal {

class ObjectPtrImpl {
public:
    ObjectPtrImpl() noexcept;
    explicit ObjectPtrImpl(NSObject* object) noexcept;

    ObjectPtrImpl(const ObjectPtrImpl& rhs) noexcept;
    ObjectPtrImpl(ObjectPtrImpl&& rhs) noexcept;

    ~ObjectPtrImpl();

    ObjectPtrImpl& operator=(const ObjectPtrImpl& rhs) noexcept {
        ObjectPtrImpl tmp(rhs);
        swap(tmp);
        return *this;
    }

    ObjectPtrImpl& operator=(ObjectPtrImpl&& rhs) noexcept {
        ObjectPtrImpl tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    void swap(ObjectPtrImpl& rhs) noexcept;

    NSObject* get() const noexcept;
    bool valid() const noexcept;

    void reset() noexcept;
    void reset(NSObject* object) noexcept;

    bool operator==(const ObjectPtrImpl& rhs) const noexcept;
    bool operator<(const ObjectPtrImpl& rhs) const noexcept;

    std::size_t computeHash() const noexcept;

private:
    NSObject* object_;
};

} // namespace internal

// `std::shared_ptr`-like smart pointer for ObjC objects.
//
// IMPORTANT: Must be constructed from a retained ObjC object.
// This optimizes for the common case of constructing directly from `[[T alloc] init...]`.
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
public:
    // Construct empty `object_ptr`.
    object_ptr() noexcept = default;

    // Construct from ObjC object.
    //
    // IMPORTANT: this does not `retain` `ptr`.
    explicit object_ptr(T* ptr) noexcept : impl_(ptr) {}

    void swap(object_ptr<T>& rhs) noexcept { impl_.swap(rhs.impl_); }

    T* get() const noexcept { return (T*)impl_.get(); }
    T* operator*() const noexcept { return (T*)impl_.get(); }
    T* operator->() const noexcept { return (T*)impl_.get(); }

    explicit operator bool() const noexcept { return impl_.valid(); }

    // Release stored pointer and become empty.
    void reset() noexcept { impl_.reset(); }

    // Release stored pointer and store `ptr` instead.
    //
    // IMPORTANT: this does not `retain` `ptr`.
    void reset(T* ptr) noexcept { impl_.reset(ptr); }

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

    internal::ObjectPtrImpl impl_;
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
    std::size_t operator()(const kotlin::objc_support::object_ptr<T>& value) { return std::hash<NSObject*>()(value.impl_.get()); }
};

// TODO: std::atomic specialization?

} // namespace std

#endif
