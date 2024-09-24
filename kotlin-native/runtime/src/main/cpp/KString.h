/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Common.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"
#include "TypeInfo.h"

enum class StringEncoding : uint8_t {
    kUTF16 = 0,
    kLatin1 = 1,
};

// Strings are effectively this:
//     class String {
//         private val hashCode_: Int
//         private val flags_: Short
//         private val data_: CharArray
//     }
// In order to avoid allocating a separate object for the data array, all other fields are packed
// into it at the beginning, so a String's ObjHeader is also an ArrayHeader, and the space taken
// up by other fields is counted in its length.
struct StringHeader {
    ARRAY_HEADER_FIELDS
    int32_t hashCode_; // if ArrayHeader has padding, this will go into it instead of the array's data
    /**
     * Layout:     | Encoding | <unused> | Ignore last byte | HC computed |
     * bit number: | 15 .. 12 |          |                1 |           0 |
     */
    uint16_t flags_;
    alignas(KChar) char data_[];

    enum {
        // Set on first hashcode computation if the result is 0. Unlike having a "hashcode computed" flag,
        // this means there's always exactly one atomic write, so it needs no synchronization.
        HASHCODE_IS_ZERO = 1 << 0,
        // Set if the string's encoding has 1-byte characters and the string encodes to an odd-length
        // byte sequence (the byte sequence has to be padded to an even length to become a Char array).
        IGNORE_LAST_BYTE = 1 << 1,
        ENCODING_OFFSET = 12,
    };

    ALWAYS_INLINE StringEncoding encoding() const { return static_cast<StringEncoding>(flags_ >> ENCODING_OFFSET); }

    ALWAYS_INLINE char *data() { return data_; }
    ALWAYS_INLINE const char *data() const { return data_; }
    ALWAYS_INLINE size_t size() const { return count_ * sizeof(KChar) - extraLength(flags_); }

    ALWAYS_INLINE static StringHeader* of(KRef string) {
        RuntimeAssert(string != nullptr && string->type_info() == theStringTypeInfo, "Must use String");
        return reinterpret_cast<StringHeader*>(string);
    }

    ALWAYS_INLINE static const StringHeader* of(KConstRef string) {
        RuntimeAssert(string != nullptr && string->type_info() == theStringTypeInfo, "Must use String");
        return reinterpret_cast<const StringHeader*>(string);
    }

    ALWAYS_INLINE constexpr static size_t extraLength(int flags) {
        return (offsetof(StringHeader, data_) - sizeof(ArrayHeader)) + !!(flags & IGNORE_LAST_BYTE);
    }
};

static_assert(StringHeader::extraLength(0) % 2 == 0, "String's data is not aligned to Char");

template <StringEncoding encoding>
struct StringData;

template <StringEncoding encoding_, typename unit_>
struct FixedLengthUnitStringData {
    using unit = unit_;
    static constexpr const StringEncoding encoding = encoding_;
    static constexpr bool canEncode(KChar c) { return c <= std::numeric_limits<unit>::max(); }

    struct Iterator {
        using difference_type = size_t;
        using value_type = KChar;
        using pointer = const KChar*;
        using reference = const KChar&;
        using iterator_category = std::bidirectional_iterator_tag;

        const unit* p_;

        const unit* ptr() const { return p_; }
        KChar operator*() const { return *p_; }
        Iterator& operator++() { ++p_; return *this; };
        Iterator& operator--() { --p_; return *this; };
        Iterator operator++(int) { return {p_++}; };
        Iterator operator--(int) { return {p_--}; };
        Iterator operator+(size_t offset) const { return {p_ + offset}; }
        bool operator==(const Iterator& other) const { return p_ == other.p_; }
        bool operator!=(const Iterator& other) const { return p_ != other.p_; }
        size_t operator-(const Iterator& other) const { return p_ - other.p_; }
    };

    const unit* data_;
    const size_t size_;

    FixedLengthUnitStringData(const StringHeader* header) :
        data_(reinterpret_cast<const unit*>(header->data())),
        size_(header->size() / sizeof(unit))
    {}

    Iterator begin() const { return {data_}; }
    Iterator end() const { return {data_ + size_}; }
    Iterator at(const unit* ptr) const { return {ptr}; }
    size_t sizeInUnits() const { return size_; }
    size_t sizeInChars() const { return size_; }
};

template <>
struct StringData<StringEncoding::kUTF16> : FixedLengthUnitStringData<StringEncoding::kUTF16, KChar> {
    using FixedLengthUnitStringData::FixedLengthUnitStringData;
};

template <>
struct StringData<StringEncoding::kLatin1> : FixedLengthUnitStringData<StringEncoding::kLatin1, uint8_t> {
    using FixedLengthUnitStringData::FixedLengthUnitStringData;
};

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring);
OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t length);
OBJ_GETTER(CreateStringFromUtf8OrThrow, const char* utf8, uint32_t length);
OBJ_GETTER(CreateStringFromUtf16, const KChar* utf16, uint32_t length);
OBJ_GETTER(CreateUninitializedString, StringEncoding encoding, uint32_t length);

char* CreateCStringFromString(KConstRef kstring);
void DisposeCString(char* cstring);

KRef CreatePermanentStringFromCString(const char* nullTerminatedUTF8);
// In real-world uses, permanent strings created by `CreatePermanentStringFromCString` are referenced until termination
// and don't need to be deallocated. To make address sanitizer not complain about "memory leaks" in hostRuntimeTests,
// though, they should be deallocated using this function.
void FreePermanentStringForTests(KConstRef header);

} // extern "C"

enum class KStringConversionMode { UNCHECKED, CHECKED, REPLACE_INVALID };

namespace kotlin {

template <KStringConversionMode mode>
std::string to_string(KConstRef kstring, size_t start = 0, size_t size = std::string::npos) noexcept(mode != KStringConversionMode::CHECKED);

}
