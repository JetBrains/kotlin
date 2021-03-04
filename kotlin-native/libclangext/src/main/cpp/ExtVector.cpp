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


#include <cassert>

#include <clang/AST/Type.h>
#include <clang-c/Index.h>


using namespace clang;


static inline QualType GetQualType(CXType CT) {
  return QualType::getFromOpaquePtr(CT.data[0]);
}

extern "C"
int clang_isExtVectorType(CXType CT) {
  static_assert(CINDEX_VERSION < 59, "Use CXType_ExtVector for this libclang version");

  QualType T = GetQualType(CT);
  const clang::Type *TP = T.getTypePtrOrNull();
  return TP && TP->isExtVectorType();
}
