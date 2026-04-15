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
#include <cstdlib>
#include <cstring>
#include <functional>

#include <llvm/ADT/StringRef.h>
#include <clang/Basic/LLVM.h>
#include "clang-c/ext.h"

#if LIBCLANGEXT_ENABLE

#include <clang/AST/Attr.h>
#include <clang/AST/DeclObjC.h>
#include <clang/Frontend/ASTUnit.h>

#endif // LIBCLANGEXT_ENABLE

using namespace clang;

namespace {
  // Hash combine function derived from boost.
  // Copyright 2005-2014 Daniel James.
  // https://github.com/boostorg/container_hash/blob/b2e3beea3f44ac783765503eea133df29d11c8e8/include/boost/container_hash/hash.hpp#L159

  template <class T>
  void hash_combine(std::size_t& seed, const T& v) {
      std::hash<T> hasher;
      seed ^= hasher(v) + 0x9e3779b9 + (seed << 6) + (seed >> 2);
  }
}  // namespace

#if LIBCLANGEXT_ENABLE

static CXCursor makeObjCProtocolDeclCXCursor(const ObjCProtocolDecl* decl, CXTranslationUnit translationUnit) {
  auto kind = CXCursor_ObjCProtocolDecl;
  CXCursor result = { kind, 0, { decl, (void*)(intptr_t) 1, translationUnit } };
  return result;
}

static const Attr* getCursorAttr(CXCursor cursor) {
  return static_cast<const Attr *>(cursor.data[1]);
}

static const Decl *getCursorDecl(CXCursor Cursor) {
  return static_cast<const Decl *>(Cursor.data[0]);
}

static const QualType unwrapCXType(CXType type) {
  return QualType::getFromOpaquePtr(type.data[0]);
}

static CXTranslationUnit getTranslationUnit(CXType type) {
  return static_cast<CXTranslationUnit>(type.data[1]);
}

static ASTUnit* getASTUnit(CXTranslationUnit translationUnit) {
  return reinterpret_cast<ASTUnit**>(translationUnit)[1];
}

// The functions above are totally libclang-implementation-specific and thus version-dependent.

static CXTypeAttributes makeCXTypeAttributes(QualType qualType) {
  CXTypeAttributes result = { qualType.getAsOpaquePtr() };
  return result;
}

static QualType unwrapCXTypeAttributes(CXTypeAttributes attributes) {
  return QualType::getFromOpaquePtr(attributes.typeOpaquePtr);
}

#else // LIBCLANGEXT_ENABLE

static CXTypeAttributes makeCXTypeAttributes() {
  CXTypeAttributes result = { nullptr };
  return result;
}

#endif // LIBCLANGEXT_ENABLE

static CString createCString(llvm::StringRef str) {
  return CString { strdup(str.str().c_str()) };
}

static CString nullCString() {
  return CString { nullptr };
}

extern "C" {
  void clang_disposeCString(CString str) {
    free(str.data);
  }

  const char* clang_Cursor_getAttributeSpelling(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    if (clang_isAttribute(cursor.kind) == 0) {
      return nullptr;
    }

    return getCursorAttr(cursor)->getSpelling();
#else
    return "";
#endif
  }

  CXTypeAttributes clang_getDeclTypeAttributes(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    CXType cxType = clang_getCursorType(cursor);
    if (clang_isDeclaration(cursor.kind)) {
      const Decl *D = getCursorDecl(cursor);
      if (D) {
        if (const DeclaratorDecl *DD = dyn_cast<DeclaratorDecl>(D))
          return makeCXTypeAttributes(DD->getType());
      }
    }
    return makeCXTypeAttributes(QualType());
#else
    return makeCXTypeAttributes();
#endif
  }

  CXTypeAttributes clang_getResultTypeAttributes(CXTypeAttributes typeAttributes) {
#if LIBCLANGEXT_ENABLE
    QualType qualType = unwrapCXTypeAttributes(typeAttributes);
    if (qualType.isNull())
      return makeCXTypeAttributes(qualType);

    if (const FunctionType *functionType = qualType->getAs<FunctionType>())
      return makeCXTypeAttributes(functionType->getReturnType());

    return makeCXTypeAttributes(QualType());
#else
    return makeCXTypeAttributes();
#endif
  }

  CXTypeAttributes clang_getCursorResultTypeAttributes(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    if (clang_isDeclaration(cursor.kind)) {
      const Decl *decl = getCursorDecl(cursor);
      if (const ObjCMethodDecl *methodDecl = dyn_cast_or_null<ObjCMethodDecl>(decl))
        return makeCXTypeAttributes(methodDecl->getReturnType());

      return clang_getResultTypeAttributes(clang_getDeclTypeAttributes(cursor));
    }

    return makeCXTypeAttributes(QualType());
#else
    return makeCXTypeAttributes();
#endif
  }

  enum CXNullabilityKind clang_Type_getNullabilityKind(CXType type, CXTypeAttributes attributes) {
#if LIBCLANGEXT_ENABLE
    CXTranslationUnit translationUnit = getTranslationUnit(type);
    ASTContext& astContext = getASTUnit(translationUnit)->getASTContext();

    QualType qualType = unwrapCXTypeAttributes(attributes);

    auto kind = qualType->getNullability();
    if (!kind) {
      return CXNullabilityKind_Unspecified;
    }

    switch (*kind) {
      case NullabilityKind::NonNull: return CXNullabilityKind_NonNull;
      case NullabilityKind::Nullable: return CXNullabilityKind_Nullable;
      case NullabilityKind::Unspecified: return CXNullabilityKind_Unspecified;
      default: assert(false);
    }
#else
    return CXNullabilityKind_Unspecified;
#endif
  }

  CString clang_Cursor_getObjCProtocolRuntimeName(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
      if (cursor.kind == CXCursor_ObjCProtocolDecl) {
        if (const ObjCProtocolDecl *decl = dyn_cast_or_null<ObjCProtocolDecl>(getCursorDecl(cursor))) {
          return createCString(decl->getObjCRuntimeNameAsString());
        }
      }
#endif
      return nullCString();
  }

      CString clang_Cursor_getObjCInterfaceRuntimeName(CXCursor cursor) {
        #if LIBCLANGEXT_ENABLE
        if (cursor.kind == CXCursor_ObjCInterfaceDecl) {
      if (const ObjCInterfaceDecl *decl = dyn_cast_or_null<ObjCInterfaceDecl>(getCursorDecl(cursor))) {
        return createCString(decl->getObjCRuntimeNameAsString());
      }
    }
         #endif
          return nullCString();
      }

    CString clang_Cursor_getDefinedIn(CXCursor cursor) {
    #if LIBCLANGEXT_ENABLE
        if (const NamedDecl *decl = dyn_cast_or_null<NamedDecl>(getCursorDecl(cursor))) {
            if (auto *attr = decl->getExternalSourceSymbolAttr()) {
                return createCString(attr->getDefinedIn());
            }
            return nullCString();
        }
    #endif
        return nullCString();
    }

  unsigned clang_Cursor_isObjCInitMethod(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    if (cursor.kind == CXCursor_ObjCInstanceMethodDecl) {
      const Decl *decl = getCursorDecl(cursor);
      if (const ObjCMethodDecl *methodDecl = dyn_cast_or_null<ObjCMethodDecl>(decl)) {
        return methodDecl->getMethodFamily() == OMF_init;
      }
    }
#endif
    return 0;
  }

  unsigned clang_Cursor_isObjCReturningRetainedMethod(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    if (cursor.kind == CXCursor_ObjCInstanceMethodDecl) {
      const Decl *decl = getCursorDecl(cursor);
      if (const ObjCMethodDecl *methodDecl = dyn_cast_or_null<ObjCMethodDecl>(decl)) {
        return methodDecl->hasAttr<NSReturnsRetainedAttr>();
      }
    }
#endif
    return 0;
  }

  unsigned clang_Cursor_isObjCConsumingSelfMethod(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    if (cursor.kind == CXCursor_ObjCInstanceMethodDecl) {
      const Decl *decl = getCursorDecl(cursor);
      if (const ObjCMethodDecl *methodDecl = dyn_cast_or_null<ObjCMethodDecl>(decl)) {
        return methodDecl->hasAttr<NSConsumesSelfAttr>();
      }
    }
#endif
    return 0;
  }

  CString clang_Cursor_getSwiftName(CXCursor cursor) {
#if LIBCLANGEXT_ENABLE
    if (clang_isDeclaration(cursor.kind)) {
      const Decl *decl = getCursorDecl(cursor);
      if (decl) {
        if (const auto *attr = decl->getAttr<SwiftNameAttr>()) {
          return createCString(attr->getName());
        }
      }
    }
#endif
    return nullCString();
  }

  unsigned clang_visitObjectLikeMacroDefinitions(
    CXTranslationUnit translationUnit,
    bool excludeSystemHeaders,
    MacroVisitor visitor,
    CXClientData client_data
    ) {
    struct VisitMacro {
      bool excludeSystemHeaders;
      MacroVisitor visitor;
      CXClientData clientData;
    };

    auto parent = clang_getTranslationUnitCursor(translationUnit);
    auto data = VisitMacro { excludeSystemHeaders, visitor, client_data };
    return clang_visitChildren(parent, [](CXCursor cursor, CXCursor parent, CXClientData data) {
      if (cursor.kind != CXCursor_MacroDefinition || clang_Cursor_isMacroFunctionLike(cursor)) {
        return CXChildVisit_Continue;
      }
      auto* visitMacroData = reinterpret_cast<VisitMacro*>(data);
      auto location = clang_getCursorLocation(cursor);
      if (visitMacroData->excludeSystemHeaders && clang_Location_isInSystemHeader(location)) {
        return CXChildVisit_Continue;
      }
      CXFile file;
      clang_getFileLocation(location, &file, nullptr, nullptr, nullptr);
      if (!file) {
        return CXChildVisit_Continue;
      }
      auto spelling = clang_getCursorSpelling(cursor);
      auto spellingCStr = clang_getCString(spelling);
      visitMacroData->visitor(visitMacroData->clientData, spellingCStr, location, file);
      clang_disposeString(spelling);
      return CXChildVisit_Continue;
    }, &data);
  }

  int32_t clang_getFileUniqueIDHash(CXFile file) {
    CXFileUniqueID id;
    if (clang_getFileUniqueID(file, &id) != 0) {
      return 0;
    }
    std::size_t hash = 0;
    for (auto part : id.data) {
      hash_combine(hash, part);
    }
    static_assert(sizeof(hash) == 8);
    return static_cast<int32_t>(hash ^ (hash >> 32));
  }
}
