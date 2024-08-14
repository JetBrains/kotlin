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

    const uint8_t* p_;

    const uint8_t* ptr() const { return p_; }
    KChar operator*() const { return *p_; }
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
    using unit = uint8_t;

    const uint8_t* data_;
    const size_t size_;

    Latin1StringIterator begin() const { return {data_}; }
    Latin1StringIterator end() const { return {data_ + size_}; }
    Latin1StringIterator at(const uint8_t* ptr) const { return {ptr}; }
    size_t sizeInUnits() const { return size_; }
    size_t sizeInChars() const { return size_; }

    static bool canEncode(KChar c) { return c < 256; }

    static OBJ_GETTER(createUninitialized, size_t sizeInUnits) {
        RETURN_RESULT_OF(CreateUninitializedLatin1String, sizeInUnits);
    }

    std::string toUTF8(KStringConversionMode mode, size_t start, size_t size) const {
        auto it = data_ + start;
        auto end = size == std::string::npos ? data_ + size_ : it + size;
        std::string result;
        result.resize((end - it) + std::count_if(it, end, [](unit c) { return c >= 0x80; }));
        auto out = result.begin();
        while (it != end) {
            auto latin1 = *it++;
            if (latin1 >= 0x80) {
                *out++ = 0xC0 | (latin1 >> 6);
                *out++ = latin1 & 0xBF;
            } else {
                *out++ = latin1;
            }
        }
        return result;
    }
};

template <typename T>
constexpr bool isLatin1(T&& string) {
    return std::is_same_v<std::decay_t<T>, Latin1String>;
}

}
