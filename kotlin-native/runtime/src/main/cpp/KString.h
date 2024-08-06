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

namespace {

// Header for strings. The extra data added here is counted as part of the array's size;
// if the array is not large enough to contain this header as well as at least one Char
// of data, then the header is inferred to be zero. In particular, the only way to encode
// an empty string is an empty array, while strings shorter than the header have a "short"
// form without the header and a "long" form with it.
struct StringHeader {
    int32_t hashCode_;
    int32_t flags_;

    enum {
        HASHCODE_COMPUTED = 1,
    };
};

static constexpr const size_t STRING_HEADER_SIZE = (sizeof(StringHeader) + sizeof(KChar) - 1) / sizeof(KChar);

inline size_t StringRawDataOffset(KConstRef kstring) {
    return kstring->array()->count_ > STRING_HEADER_SIZE ? STRING_HEADER_SIZE : 0;
}

inline const StringHeader* StringHeaderOf(KConstRef kstring) {
    return kstring->array()->count_ > STRING_HEADER_SIZE
        ? reinterpret_cast<const StringHeader*>(CharArrayAddressOfElementAt(kstring->array(), 0)) : nullptr;
}

// The encoding of this data is undefined unless it is known how the string was constructed,
// in which case it can be `reinterpret_cast`ed to the appropriate type.
inline char* StringRawData(KRef kstring) {
    return reinterpret_cast<char*>(CharArrayAddressOfElementAt(kstring->array(), StringRawDataOffset(kstring)));
}

inline const char* StringRawData(KConstRef kstring) {
    return reinterpret_cast<const char*>(CharArrayAddressOfElementAt(kstring->array(), StringRawDataOffset(kstring)));
}

inline size_t StringRawSize(KConstRef kstring) {
    return (kstring->array()->count_ - StringRawDataOffset(kstring)) * sizeof(KChar);
}

} // namespace

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

std::string to_string(KConstRef kstring, KStringConversionMode mode = KStringConversionMode::UNCHECKED,
    size_t start = 0, size_t size = std::string::npos);

}
