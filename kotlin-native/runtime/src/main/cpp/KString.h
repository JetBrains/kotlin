/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Common.h"
#include "Memory.h"
#include "Types.h"
#include "TypeInfo.h"

#ifdef __cplusplus
extern "C" {
#endif

OBJ_GETTER(CreateStringFromCString, const char* cstring);
OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes);
OBJ_GETTER(CreateStringFromUtf8OrThrow, const char* utf8, uint32_t lengthBytes);
OBJ_GETTER(CreateStringFromUtf16, const KChar* utf16, uint32_t lengthChars);

char* CreateCStringFromString(KConstRef kstring);
void DisposeCString(char* cstring);

KRef CreatePermanentStringFromCString(const char* nullTerminatedUTF8);
// In real-world uses, permanent strings created by `CreatePermanentStringFromCString` are referenced until termination
// and don't need to be deallocated. To make address sanitizer not complain about "memory leaks" in hostRuntimeTests,
// though, they should be deallocated using this function.
void FreePermanentStringForTests(ArrayHeader* header);

#ifdef __cplusplus
}
#endif

enum class KStringConversionMode { UNCHECKED, CHECKED, REPLACE_INVALID };

namespace kotlin {

std::string to_string(KString kstring, KStringConversionMode mode = KStringConversionMode::UNCHECKED,
    size_t start = 0, size_t size = std::string::npos);

}
