/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <clang-c/Index.h>

// TODO: the API declared below should eventually be refined and contributed to libclang.


// libclang doesn't include the type attributes when constructing `CXType`,
// so attributes have to be represented separately:
typedef struct {
  const void* typeOpaquePtr;
} CXTypeAttributes;

enum CXNullabilityKind {
  CXNullabilityKind_Nullable,
  CXNullabilityKind_NonNull,
  CXNullabilityKind_Unspecified
};

#ifdef __cplusplus
extern "C" {
#endif

const char* clang_Cursor_getAttributeSpelling(CXCursor cursor);

CXTypeAttributes clang_getDeclTypeAttributes(CXCursor cursor);

CXTypeAttributes clang_getResultTypeAttributes(CXTypeAttributes typeAttributes);

CXTypeAttributes clang_getCursorResultTypeAttributes(CXCursor cursor);

enum CXNullabilityKind clang_Type_getNullabilityKind(CXType type, CXTypeAttributes attributes);

unsigned clang_Type_getNumProtocols(CXType type);

CXCursor clang_Type_getProtocol(CXType type, unsigned index);

unsigned clang_Cursor_isObjCInitMethod(CXCursor cursor);

unsigned clang_Cursor_isObjCReturningRetainedMethod(CXCursor cursor);

unsigned clang_Cursor_isObjCConsumingSelfMethod(CXCursor cursor);

#ifdef __cplusplus
}
#endif
