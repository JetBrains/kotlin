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

struct StringHeader {
    ARRAY_HEADER_FIELDS
    int32_t hashCode_;
    uint16_t flags_;
    char data_[];

    enum {
        IGNORE_LAST_BYTE = 1 << 0,
        HASHCODE_COMPUTED = 1 << 1,

        ENCODING_OFFSET = 12,
        ENCODING_UTF16 = 0,
        ENCODING_LATIN1 = 1,
        // ENCODING_UTF8 = 2 ?
    };

    ALWAYS_INLINE int encoding() const { return flags_ >> ENCODING_OFFSET; }

    ALWAYS_INLINE char *data() { return data_; }
    ALWAYS_INLINE const char *data() const { return data_; }
    ALWAYS_INLINE size_t size() const { return count_ * sizeof(KChar) - extraLength(flags_); }

    ALWAYS_INLINE static StringHeader* of(KRef string) { return reinterpret_cast<StringHeader*>(string); }
    ALWAYS_INLINE static const StringHeader* of(KConstRef string) { return reinterpret_cast<const StringHeader*>(string); }

    ALWAYS_INLINE constexpr static size_t extraLength(int flags) {
        return (offsetof(StringHeader, data_) - sizeof(ArrayHeader)) + !!(flags & IGNORE_LAST_BYTE);
    }
};

static_assert(offsetof(StringHeader, data_) >= sizeof(ArrayHeader));
static_assert((offsetof(StringHeader, data_) - sizeof(ArrayHeader)) % 2 == 0);

} // namespace

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring);
OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t length);
OBJ_GETTER(CreateStringFromUtf8OrThrow, const char* utf8, uint32_t length);
OBJ_GETTER(CreateStringFromUtf16, const KChar* utf16, uint32_t length);

OBJ_GETTER(CreateUninitializedUtf16String, uint32_t length);
OBJ_GETTER(CreateUninitializedLatin1String, uint32_t length);

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
