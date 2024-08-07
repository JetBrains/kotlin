/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "KString.h"

namespace {

struct Latin1StringIterator {
    using difference_type = size_t;
    using value_type = KChar;
    using pointer = const KChar*;
    using reference = const KChar&;
    using iterator_category = std::bidirectional_iterator_tag;

    const char* p_;

    const char* ptr() const { return p_; }
    KChar operator*() const { return static_cast<uint8_t>(*p_); }
    Latin1StringIterator& operator++() { ++p_; return *this; }
    Latin1StringIterator& operator--() { --p_; return *this; }
    Latin1StringIterator operator++(int) { return {p_++}; }
    Latin1StringIterator operator--(int) { return {p_--}; }
    Latin1StringIterator operator+(size_t offset) const { return {p_ + offset}; }
    bool operator==(const Latin1StringIterator& other) const { return p_ == other.p_; }
    bool operator!=(const Latin1StringIterator& other) const { return p_ != other.p_; }
    size_t operator-(const Latin1StringIterator& other) const { return p_ - other.p_; }
};

struct Latin1String {
    const char* data_;
    const size_t size_;

    Latin1StringIterator begin() const { return {data_}; }
    Latin1StringIterator end() const { return {data_ + size_}; }
    Latin1StringIterator at(const char* ptr) const { return {ptr}; }
    size_t sizeInBytes() const { return size_; }
    size_t sizeInUnits() const { return size_; }
    size_t sizeInChars() const { return size_; }

    static OBJ_GETTER(createUninitialized, size_t sizeInUnits) {
        RETURN_RESULT_OF(CreateUninitializedLatin1String, sizeInUnits);
    }
};

template <typename T>
constexpr bool isLatin1(T&& string) {
    return std::is_same_v<std::decay_t<T>, Latin1String>;
}

}
