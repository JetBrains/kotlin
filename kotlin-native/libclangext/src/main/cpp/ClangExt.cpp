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

#include <clang/AST/Attr.h>
#include <clang/AST/DeclObjC.h>
#include <clang/Frontend/ASTUnit.h>
#include "clang-c/ext.h"

using namespace clang;

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

extern "C" {

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

    auto kind = qualType->getNullability(astContext);
    if (!kind.hasValue()) {
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

  unsigned clang_Type_getNumProtocols(CXType type) {
#if LIBCLANGEXT_ENABLE
    QualType qualType = unwrapCXType(type);
    if (auto objCObjectPointerType = qualType->getAs<ObjCObjectPointerType>()) {
      return objCObjectPointerType->getObjectType()->getNumProtocols();
    }
#endif
    return 0;
  }

  CXCursor clang_Type_getProtocol(CXType type, unsigned index) {
#if LIBCLANGEXT_ENABLE
    QualType qualType = unwrapCXType(type);
    if (auto objCObjectPointerType = qualType->getAs<ObjCObjectPointerType>()) {
      auto objectType = objCObjectPointerType->getObjectType();
      unsigned n = objectType->getNumProtocols();
      if (index < n) {
        auto protocolDecl = objectType->getProtocol(index);
        auto kind = CXCursor_ObjCProtocolDecl;
        return makeObjCProtocolDeclCXCursor(protocolDecl, getTranslationUnit(type));
      }
    }
#endif
    return clang_getNullCursor();
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

}
