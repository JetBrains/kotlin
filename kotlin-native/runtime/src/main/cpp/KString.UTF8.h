/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "KString.h"

#include "utf8.h"

template <>
struct StringData<StringEncoding::kUTF8> {
    using unit = uint8_t;
    static constexpr const StringEncoding encoding = StringEncoding::kUTF8;
    static constexpr bool canEncode(KChar) { return true; }

    struct Iterator {
        using difference_type = size_t;
        using value_type = KChar;
        using pointer = void;
        using reference = void;
        using iterator_category = std::bidirectional_iterator_tag;

        const unit* p_;
        bool low_;

        KChar operator*() const {
            auto c = utf8::unchecked::peek_next(p_);
            return c < 0x10000 ? c : low_ ? 0xDC00u + (c & 0x3FF) : 0xD800u + ((c >> 10) - 0x40);
        }

        Iterator& operator++() {
            auto p = p_;
            auto c = utf8::unchecked::next(p);
            low_ = !low_ && c >= 0x10000;
            if (!low_) p_ = p;
            return *this;
        };

        Iterator& operator--() {
            if (low_) {
                low_ = false;
            } else {
                p_ = align(p_ - 1);
                low_ = utf8::unchecked::peek_next(p_) >= 0x10000;
            }
            return *this;
        };

        Iterator operator++(int) {
            Iterator copy = *this;
            operator++();
            return copy;
        };

        Iterator operator--(int) {
            Iterator copy = *this;
            operator--();
            return copy;
        };

        Iterator operator+(size_t offset) const {
            if (!offset) return *this;

            auto p = p_;
            for (offset += low_; offset > 0; --offset) {
                auto q = p;
                if (utf8::unchecked::next(p) >= 0x10000 && --offset == 0) {
                    return {q, true};
                }
            }
            return {p, false};
        }

        bool operator==(const Iterator& other) const { return p_ == other.p_ && low_ == other.low_; }
        bool operator!=(const Iterator& other) const { return p_ != other.p_ || low_ != other.low_; }
        size_t operator-(const Iterator& other) const { return utf8::unchecked::utf16_length(other.p_, p_) + (low_ - other.low_); }

        const uint8_t* ptr() const { return low_ ? nullptr : p_; }
    };

    const unit* const data_;
    const size_t size_;

    StringData(const StringHeader* header) :
        data_(reinterpret_cast<const unit*>(header->data())),
        size_(header->size() / sizeof(unit))
    {}

    Iterator begin() const { return {data_, false}; }
    Iterator end() const { return {data_ + size_, false}; }
    Iterator at(const unit* ptr) const { return {ptr == data_ + size_ ? ptr : align(ptr), false}; }

    size_t sizeInUnits() const { return size_; }
    size_t sizeInChars() const { return utf8::unchecked::utf16_length(data_, data_ + size_); }

    static const unit* align(const unit* ptr) {
        while (utf8::internal::is_trail(*ptr)) ptr--;
        return ptr;
    }
};
