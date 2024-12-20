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
    uint16_t flags_;
    alignas(KChar) char data_[];

    enum {
        HASHCODE_IS_ZERO = 1 << 0,
    };

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
        return offsetof(StringHeader, data_) - sizeof(ArrayHeader);
    }
};

static_assert(StringHeader::extraLength(0) % 2 == 0, "String's data is not aligned to Char");

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring);
OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes);
OBJ_GETTER(CreateStringFromUtf8OrThrow, const char* utf8, uint32_t lengthBytes);
OBJ_GETTER(CreateStringFromUtf16, const KChar* utf16, uint32_t lengthChars);

// The string returned by this method contains undefined data; users should fill the array
// returned by `StringRawData`, which is guaranteed to have a size of `lengthChars * 2`.
OBJ_GETTER(CreateUninitializedUtf16String, uint32_t lengthChars);

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
