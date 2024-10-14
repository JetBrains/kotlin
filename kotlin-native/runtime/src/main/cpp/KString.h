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

// The encoding of this data is undefined unless it is known how the string was constructed,
// in which case it can be `reinterpret_cast`ed to the appropriate type.
static inline char* StringRawData(KRef kstring) {
    return reinterpret_cast<char*>(CharArrayAddressOfElementAt(kstring->array(), 0));
}

static inline const char* StringRawData(KConstRef kstring) {
    return reinterpret_cast<const char*>(CharArrayAddressOfElementAt(kstring->array(), 0));
}

static inline size_t StringRawSize(KConstRef kstring) {
    return kstring->array()->count_ * sizeof(KChar);
}

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
