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
char* CreateCStringFromString(KConstRef kstring);
void DisposeCString(char* cstring);
ObjHeader* CreatePermanentStringFromCString(const char* nullTerminatedUTF8);
void FreePermanentStringForTests(ArrayHeader* header);  // to make ASAN happy, in hostRuntimeTests call FreePermanentStringForTests() after CreatePermanentStringFromCString()

OBJ_GETTER(StringFromUtf8Buffer, const char* start, size_t size);

#ifdef __cplusplus
}
#endif

namespace kotlin {

std::string to_string(KString kstring);

}
