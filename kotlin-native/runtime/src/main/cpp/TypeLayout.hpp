/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifndef KONAN_WASM

#include <algorithm>
#include <cinttypes>
#include <cstddef>
#include <cstdint>
#include <tuple>

#include "Alignment.hpp"
#include "KAssert.h"
#include "RawPtr.hpp"

namespace kotlin::type_layout {

namespace internal {

template <size_t index, typename... FieldDescriptors>
constexpr uint64_t fieldOffsetFromBase(uint64_t baseOffset, FieldDescriptors... fieldDescriptors) noexcept {
    constexpr auto fieldsDescriptorsSize = sizeof...(FieldDescriptors);
    static_assert(index <= fieldsDescriptorsSize);

    if constexpr (fieldsDescriptorsSize == 0) {
        return baseOffset;
    } else {
        return [](uint64_t baseOffset, auto head, auto... tail) constexpr noexcept {
            baseOffset = AlignUp<uint64_t>(baseOffset, head.alignment());
            if constexpr (index == 0) {
                return baseOffset;
            } else {
                return fieldOffsetFromBase<index - 1>(baseOffset + head.size(), tail...);
            }
        }
        (baseOffset, fieldDescriptors...);
    }
}

template <typename... FieldDescriptors>
constexpr size_t alignment(FieldDescriptors... fieldDescriptors) noexcept {
    if constexpr (sizeof...(FieldDescriptors) == 0) {
        return 1;
    } else {
        return [](auto head, auto... tail) constexpr noexcept { return std::max(head.alignment(), alignment(tail...)); }
        (fieldDescriptors...);
    }
}

template <typename... FieldDescriptors>
constexpr void construct(uint8_t* ptr, FieldDescriptors... fieldDescriptors) noexcept {
    if constexpr (sizeof...(FieldDescriptors) == 0) {
        return;
    } else {
        [](uint8_t * ptr, auto head, auto... tail) constexpr noexcept {
            head.construct(ptr);
            auto offset = fieldOffsetFromBase<1>(0, head, tail...);
            construct(ptr + offset, tail...);
        }
        (ptr, fieldDescriptors...);
    }
}

template <typename T, typename = void>
struct has_descriptor : std::false_type {};

template <typename T>
struct has_descriptor<T, std::void_t<typename T::descriptor>> : std::true_type {};

} // namespace internal

template <typename T, bool = internal::has_descriptor<T>::value>
struct descriptor {
    using type = typename T::descriptor;
};

template <typename T>
struct descriptor<T, false> {
    struct type {
        using value_type = T;

        static constexpr size_t alignment() noexcept { return alignof(T); }
        static constexpr size_t size() noexcept { return sizeof(T) == 0 ? 0 : AlignUp(sizeof(T), alignment()); }

        static constexpr value_type* construct(uint8_t* ptr) noexcept { return new (ptr) T(); }
    };
};

// Get descriptor of type `T`.
// If `T` has `descriptor` member type, it'll be used as a descriptor.
// Implementations may also add their own specializations of `descriptor<T>`.
template <typename T>
using descriptor_t = typename descriptor<T>::type;

// Descriptor for a composite type `T` consisting of `Fields...`.
// Common usage would be:
// ```
// struct MyClass {
//     using descriptor = Composite<MyClass, Fields...>;
//     // Helper methods for accessing fields and constructing from fields
//     // that forward to Composite::field and Composite::fromField
// private:
//     MyClass() = delete;
//     ~MyClass() = delete;
// };
// ```
template <typename T, typename... Fields>
class Composite {
public:
    using value_type = T;

    constexpr Composite() noexcept = default;

    constexpr explicit Composite(descriptor_t<Fields>... fields) noexcept : fields_(fields...) {}

    // This will be the maximum alignment across all fields.
    // Alignment of an empty composite type is 1.
    constexpr size_t alignment() const noexcept { return std::apply(internal::alignment<descriptor_t<Fields>...>, fields_); }

    // The size of an empty composite type will be 0, unlike C++ where it's 1.
    constexpr uint64_t size() const noexcept {
        if (auto offset = fieldOffset<sizeof...(Fields)>()) {
            return AlignUp<uint64_t>(offset, alignment());
        }
        return 0;
    }

    // Construct this at address `ptr`.
    constexpr value_type* construct(uint8_t* ptr) noexcept {
        std::apply(internal::construct<descriptor_t<Fields>...>, std::tuple_cat(std::make_tuple(ptr), fields_));
        return reinterpret_cast<value_type*>(ptr);
    }

    // Get offset of field at `index` from start of this.
    // `index` can be equal to count of `Fields...` This will point to
    // right after the last field.
    template <size_t index>
    constexpr uint64_t fieldOffset() const noexcept {
        return std::apply(
                internal::fieldOffsetFromBase<index, descriptor_t<Fields>...>, std::tuple_cat(std::make_tuple<uint64_t>(0), fields_));
    }

    template <size_t index>
    using FieldDescriptor = std::tuple_element_t<index, std::tuple<descriptor_t<Fields>...>>;

    // Get descriptor of field at `index` and its address given address of this.
    template <size_t index>
    constexpr std::pair<FieldDescriptor<index>, typename FieldDescriptor<index>::value_type*> field(value_type* ptr) noexcept {
        auto offset = fieldOffset<index>();
        auto field = std::get<index>(fields_);
        return {field, reinterpret_cast<typename decltype(field)::value_type*>(reinterpret_cast<uint8_t*>(ptr) + offset)};
    }

    // Get address of this given address of a field at `index`.
    template <size_t index>
    constexpr value_type* fromField(typename FieldDescriptor<index>::value_type* ptr) noexcept {
        auto offset = fieldOffset<index>();
        return reinterpret_cast<value_type*>(reinterpret_cast<uint8_t*>(ptr) - offset);
    }

private:
    std::tuple<descriptor_t<Fields>...> fields_;
};

template <typename T>
class Composite<T> {
public:
    using value_type = T;

    constexpr Composite() noexcept = default;

    // This will be the maximum alignment across all fields.
    // Alignment of an empty composite type is 1.
    constexpr size_t alignment() const noexcept { return 1; }

    // The size of an empty composite type will be 0, unlike C++ where it's 1.
    constexpr uint64_t size() const noexcept { return 0; }

    // Construct this at address `ptr`.
    constexpr value_type* construct(uint8_t* ptr) noexcept {}

    // Get offset of field at `index` from start of this.
    // `index` can be equal to count of `Fields...` This will point to
    // right after the last field.
    template <size_t index>
    constexpr uint64_t fieldOffset() const noexcept {
        static_assert(index == 0);
        return 0;
    }
};

} // namespace kotlin::type_layout

#endif
