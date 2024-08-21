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

// Having this structure as packed serves two purposes:
// 1. it avoids trailing padding due to `ArrayHeader` starting with a pointer;
// 2. it makes the padding before `hashCode_` explicit, which is necessary for
//  emitting it properly in KotlinStaticData.
struct __attribute__((packed)) StringHeader {
    ArrayHeader array_;
    union {
        char arrayData_[];
        struct __attribute__((packed)) {
            uint16_t flags_;
            union {
                char dataWithoutHashCode_[];
                struct __attribute__((packed)) {
                    int16_t padding_; // align to 4 bytes
                    int32_t hashCode_;
                    char data_[];
                };
            };
        };
    };

    enum {
        // Don't forget to modify KotlinStaticData.createKotlinStringLiteral if changing these.
        HASHCODE_CACHEABLE = 1 << 0,
        HASHCODE_COMPUTED = 1 << 1,
        IGNORE_LAST_BYTE = 1 << 2,

        ENCODING_OFFSET = 12,
        ENCODING_UTF16 = 0,
        ENCODING_LATIN1 = 1,
        // ENCODING_UTF8 = 2 ?
    };

    ALWAYS_INLINE int encoding() const {
        return array_.count_ == 0 ? ENCODING_UTF16 : flags_ >> ENCODING_OFFSET;
    }

    ALWAYS_INLINE char *data() {
        return array_.count_ == 0 ? arrayData_ : flags_ & HASHCODE_CACHEABLE ? data_ : dataWithoutHashCode_;
    }

    ALWAYS_INLINE const char *data() const {
        return const_cast<StringHeader*>(this)->data();
    }

    PERFORMANCE_INLINE size_t size() const {
        return array_.count_ == 0 ? 0 : array_.count_ * sizeof(KChar) - extraLength(flags_);
    }

    ALWAYS_INLINE static StringHeader* of(KRef string) {
        return reinterpret_cast<StringHeader*>(string);
    }

    ALWAYS_INLINE static const StringHeader* of(KConstRef string) {
        return reinterpret_cast<const StringHeader*>(string);
    }

    ALWAYS_INLINE constexpr static size_t extraLength(int flags) {
        return (flags & HASHCODE_CACHEABLE ? offsetof(StringHeader, data_) : offsetof(StringHeader, dataWithoutHashCode_))
            - offsetof(StringHeader, arrayData_) + !!(flags & IGNORE_LAST_BYTE);
    }
};

// These constants are hardcoded here because they're also hardcoded there...
// ...can LLVM APIs be used to get these values from Kotlin?..
// ...but this also ensures the header is as small as possible, so maybe constants are fine...
static_assert(StringHeader::extraLength(0) == 1 * 2, "check createKotlinStringLiteral before changing this assert");
static_assert(StringHeader::extraLength(StringHeader::HASHCODE_CACHEABLE) == 4 * 2,
              "check createKotlinStringLiteral before changing this assert");

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
