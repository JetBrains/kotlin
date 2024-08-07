/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "KString.h"

#include "utf8.h"

namespace {

struct UTF16StringIterator {
    using difference_type = size_t;
    using value_type = KChar;
    using pointer = const KChar*;
    using reference = const KChar&;
    using iterator_category = std::bidirectional_iterator_tag;

    const KChar* p_;

    const KChar* ptr() const { return p_; }
    KChar operator*() const { return *p_; }
    UTF16StringIterator& operator++() { ++p_; return *this; };
    UTF16StringIterator& operator--() { --p_; return *this; };
    UTF16StringIterator operator++(int) { return {p_++}; };
    UTF16StringIterator operator--(int) { return {p_--}; };
    UTF16StringIterator operator+(size_t offset) const { return {p_ + offset}; }
    bool operator==(const UTF16StringIterator& other) const { return p_ == other.p_; }
    bool operator!=(const UTF16StringIterator& other) const { return p_ != other.p_; }
    size_t operator-(const UTF16StringIterator& other) const { return p_ - other.p_; }
};

struct UTF16String {
    using unit = KChar;

    const KChar* data_;
    const size_t size_;

    UTF16StringIterator begin() const { return {data_}; }
    UTF16StringIterator end() const { return {data_ + size_}; }
    UTF16StringIterator at(const KChar* ptr) const { return {ptr}; }
    size_t sizeInUnits() const { return size_; }
    size_t sizeInChars() const { return size_; }

    static bool canEncode(KChar c) { return true; }

    static OBJ_GETTER(createUninitialized, size_t sizeInUnits) {
        RETURN_RESULT_OF(CreateUninitializedUtf16String, sizeInUnits);
    }
};

template <typename T>
constexpr bool isUTF16(T&& string) {
    return std::is_same_v<std::decay_t<T>, UTF16String>;
}

}
