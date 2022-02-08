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

#include <assert.h>
#include <llvm/IR/DebugInfo.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/DIBuilder.h>
#include <llvm/IR/DebugInfoMetadata.h>
#include <llvm/IR/Instruction.h>
#include <llvm/Support/Casting.h>
#include <llvm-c/DebugInfo.h>
#include "DebugInfoC.h"
/**
 * c++ --std=c++17 llvmDebugInfoC/src/DebugInfoC.cpp -IllvmDebugInfoC/include/ -Idependencies/all/clang+llvm-3.9.0-darwin-macos/include -Ldependencies/all/clang+llvm-3.9.0-darwin-macos/lib  -lLLVMCore -lLLVMSupport -lncurses -shared -o libLLVMDebugInfoC.dylib
 */

namespace llvm {
//DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIBuilder,        DIBuilderRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DICompileUnit,    DICompileUnitRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIFile,           DIFileRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIBasicType,      DIBasicTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DICompositeType,  DICompositeTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIType,           DITypeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIDerivedType,    DIDerivedTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIModule,         DIModuleRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIScope,          DIScopeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubroutineType, DISubroutineTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubprogram,     DISubprogramRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocation,       DILocationRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocalVariable,  DILocalVariableRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIExpression,     DIExpressionRef)

// from Module.cpp
//DEFINE_SIMPLE_CONVERSION_FUNCTIONS(Module,        LLVMModuleRef)
}

/**
 * see [DIFlags::FlagFwdDecl].
 */
#define DI_FORWARD_DECLARAION (4)
extern "C" {

void DIFinalize(DIBuilderRef builder) {
  auto diBuilder = llvm::unwrap(builder);
  diBuilder->finalize();
}

DICompileUnitRef DICreateCompilationUnit(DIBuilderRef builder, unsigned int lang,
                                         const char *file, const char* dir,
                                         const char * producer, int isOptimized,
                                         const char * flags, unsigned int rv) {
  llvm::DIBuilder *D = llvm::unwrap(builder);
  return llvm::wrap(llvm::unwrap(builder)->createCompileUnit(lang, D->createFile(file, dir), producer, isOptimized, flags, rv));
}

DIFileRef DICreateFile(DIBuilderRef builder, const char *filename, const char *directory) {
  return llvm::wrap(llvm::unwrap(builder)->createFile(filename, directory));
}

DIBasicTypeRef DICreateBasicType(DIBuilderRef builder, const char* name, uint64_t sizeInBits, uint64_t alignment, unsigned encoding) {
  return llvm::wrap(llvm::unwrap(builder)->createBasicType(name, sizeInBits, encoding));
}

DIModuleRef DICreateModule(DIBuilderRef builder, DIScopeOpaqueRef scope,
                           const char* name, const char* configurationMacro,
                           const char* includePath, const char *iSysRoot) {
  return llvm::wrap(llvm::unwrap(builder)->createModule(llvm::unwrap(scope), name, configurationMacro, includePath, iSysRoot));
}

DISubprogramRef DICreateFunction(DIBuilderRef builderRef, DIScopeOpaqueRef scope,
                                 const char* name, const char *linkageName,
                                 DIFileRef file, unsigned lineNo,
                                 DISubroutineTypeRef type, int isLocal,
                                 int isDefinition, unsigned scopeLine) {
  auto builder = llvm::unwrap(builderRef);
  auto subprogram = builder->createFunction(llvm::unwrap(scope),
                                            name,
                                            linkageName,
                                            llvm::unwrap(file),
                                            lineNo,
                                            llvm::unwrap(type),
                                            scopeLine, llvm::DINode::DIFlags::FlagZero, llvm::DISubprogram::toSPFlags(false, true, false));
  auto tmp = subprogram->getRetainedNodes().get();
  if (!tmp && tmp->isTemporary())
    llvm::MDTuple::deleteTemporary(tmp);

  builder->finalizeSubprogram(subprogram);
  return llvm::wrap(subprogram);
}

DIScopeOpaqueRef DICreateLexicalBlockFile(DIBuilderRef builderRef, DIScopeOpaqueRef scopeRef, DIFileRef fileRef) {
  return llvm::wrap(llvm::unwrap(builderRef)->createLexicalBlockFile(llvm::unwrap(scopeRef), llvm::unwrap(fileRef)));
}

DIScopeOpaqueRef DICreateLexicalBlock(DIBuilderRef builderRef, DIScopeOpaqueRef scopeRef, DIFileRef fileRef, int line, int column) {
  return llvm::wrap(llvm::unwrap(builderRef)->createLexicalBlock(llvm::unwrap(scopeRef), llvm::unwrap(fileRef), line, column));
}


DICompositeTypeRef DICreateStructType(DIBuilderRef refBuilder,
                                      DIScopeOpaqueRef scope, const char *name,
                                      DIFileRef file, unsigned lineNumber,
                                      uint64_t sizeInBits, uint64_t alignInBits,
                                      unsigned flags, DITypeOpaqueRef derivedFrom,
                                      DIDerivedTypeRef *elements,
                                      uint64_t elementsCount,
                                      DICompositeTypeRef refPlace) {
  auto builder = llvm::unwrap(refBuilder);
  if ((flags & DI_FORWARD_DECLARAION) != 0) {
    auto tmp = builder->createReplaceableCompositeType(
        llvm::dwarf::DW_TAG_structure_type, name, llvm::unwrap(scope), llvm::unwrap(file), lineNumber, 0, sizeInBits, alignInBits,
        (llvm::DINode::DIFlags)flags);
    builder->replaceTemporary(llvm::TempDIType(tmp), tmp);
    builder->retainType(tmp);
    return llvm::wrap(tmp);
  }
  assert(false);
  return nullptr;
}


DICompositeTypeRef DICreateArrayType(DIBuilderRef refBuilder,
                                      uint64_t size, uint64_t alignInBits,
                                      DITypeOpaqueRef refType,
                                     uint64_t elementsCount) {
  auto builder = llvm::unwrap(refBuilder);
  auto range = std::vector<llvm::Metadata*>({llvm::dyn_cast<llvm::Metadata>(builder->getOrCreateSubrange(0, size))});
  auto type = builder->createArrayType(size, alignInBits, llvm::unwrap(refType),
                                                           builder->getOrCreateArray(range));
  builder->retainType(type);
  return llvm::wrap(type);
}


DIDerivedTypeRef DICreateMemberType(DIBuilderRef refBuilder,
                                    DIScopeOpaqueRef refScope,
                                    const char *name,
                                    DIFileRef file,
                                    unsigned lineNum,
                                    uint64_t sizeInBits,
                                    uint64_t alignInBits,
                                    uint64_t offsetInBits,
                                    unsigned flags,
                                    DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(refBuilder)->createMemberType(
                      llvm::unwrap(refScope),
                      name,
                      llvm::unwrap(file),
                      lineNum,
                      sizeInBits,
                      alignInBits,
                      offsetInBits,
                      (llvm::DINode::DIFlags)flags,
                      llvm::unwrap(type)));
}

DICompositeTypeRef DICreateReplaceableCompositeType(DIBuilderRef refBuilder,
                                                    int tag,
                                                    const char *name,
                                                    DIScopeOpaqueRef refScope,
                                                    DIFileRef refFile,
                                                    unsigned line) {
  auto builder = llvm::unwrap(refBuilder);
  auto type = builder->createReplaceableCompositeType(
                                    tag, name, llvm::unwrap(refScope), llvm::unwrap(refFile), line);
  builder->retainType(type);
  return llvm::wrap(type);
}

DIDerivedTypeRef DICreateReferenceType(DIBuilderRef refBuilder, DITypeOpaqueRef refType) {
  auto builder = llvm::unwrap(refBuilder);
  auto type = builder->createReferenceType(
                                    llvm::dwarf::DW_TAG_reference_type,
                                    llvm::unwrap(refType));
  builder->retainType(type);
  return llvm::wrap(type);
}

DIDerivedTypeRef DICreatePointerType(DIBuilderRef refBuilder, DITypeOpaqueRef refType) {
  auto builder = llvm::unwrap(refBuilder);
  auto type = builder->createReferenceType(
                                    llvm::dwarf::DW_TAG_pointer_type,
                                    llvm::unwrap(refType));
  builder->retainType(type);
  return llvm::wrap(type);
}

DISubroutineTypeRef DICreateSubroutineType(DIBuilderRef builder,
                                           DITypeOpaqueRef* types,
                                           unsigned typesCount) {
  std::vector<llvm::Metadata *> parameterTypes;
  for (int i = 0; i != typesCount; ++i) {
    parameterTypes.push_back(llvm::unwrap(types[i]));
  }
  llvm::DIBuilder *b = llvm::unwrap(builder);
  llvm::DITypeRefArray typeArray = b->getOrCreateTypeArray(parameterTypes);
  auto type = b->createSubroutineType(typeArray);
  b->retainType(type);
  return llvm::wrap(type);
}

void DIFunctionAddSubprogram(LLVMValueRef fn, DISubprogramRef sp) {
  auto f = llvm::cast<llvm::Function>(llvm::unwrap(fn));
  auto dsp = llvm::cast<llvm::DISubprogram>(llvm::unwrap(sp));
  f->setSubprogram(dsp);
  if (!dsp->describes(f)) {
    fprintf(stderr, "error!!! f:%s, sp:%s\n", f->getName().str().c_str(), dsp->getLinkageName().str().c_str());
  }
}

DILocalVariableRef DICreateAutoVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, DIFileRef file, unsigned line, DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(builder)->createAutoVariable(
    llvm::unwrap(scope),
    name,
    llvm::unwrap(file),
    line,
    llvm::unwrap(type)));
}

DILocalVariableRef DICreateParameterVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, unsigned argNo, DIFileRef file, unsigned line, DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(builder)->createParameterVariable(
    llvm::unwrap(scope),
    name,
    argNo,
    llvm::unwrap(file),
    line,
    llvm::unwrap(type)));
}

DIExpressionRef DICreateEmptyExpression(DIBuilderRef builder) {
  return llvm::wrap(llvm::unwrap(builder)->createExpression());
}

void DIInsertDeclaration(DIBuilderRef builder, LLVMValueRef value, DILocalVariableRef localVariable, DILocationRef location, LLVMBasicBlockRef bb, int64_t *expr, uint64_t exprCount) {
  auto di_builder = llvm::unwrap(builder);
  std::vector<int64_t> expression;
  for (uint64_t i = 0; i < exprCount; ++i)
    expression.push_back(expr[i]);
  di_builder->insertDeclare(llvm::unwrap(value),
                            llvm::unwrap(localVariable),
                            di_builder->createExpression(expression),
                            llvm::unwrap(location),
                            llvm::unwrap(bb));
}

DILocationRef LLVMCreateLocation(LLVMContextRef contextRef, unsigned line,
                                 unsigned col, DIScopeOpaqueRef scope) {
  auto location = llvm::DILocation::get(*llvm::unwrap(contextRef), line, col, llvm::unwrap(scope), nullptr);
  return llvm::wrap(location);
}

DILocationRef LLVMCreateLocationInlinedAt(LLVMContextRef contextRef, unsigned line,
                                 unsigned col, DIScopeOpaqueRef scope, DILocationRef refLocationInlinedAt) {
  auto location = llvm::DILocation::get(*llvm::unwrap(contextRef), line, col, llvm::unwrap(scope), llvm::unwrap(refLocationInlinedAt));
  return llvm::wrap(location);
}

void LLVMBuilderSetDebugLocation(LLVMBuilderRef builder, DILocationRef refLocation) {
  llvm::unwrap(builder)->SetCurrentDebugLocation(llvm::unwrap(refLocation));
}

void LLVMBuilderResetDebugLocation(LLVMBuilderRef builder) {
  llvm::unwrap(builder)->SetCurrentDebugLocation(nullptr);
}

LLVMValueRef LLVMBuilderGetCurrentFunction(LLVMBuilderRef builder) {
  return llvm::wrap(llvm::unwrap(builder)->GetInsertBlock()->getParent());
}

const char* LLVMBuilderGetCurrentBbName(LLVMBuilderRef builder) {
  return llvm::unwrap(builder)->GetInsertBlock()->getName().str().c_str();
}


const char *DIGetSubprogramLinkName(DISubprogramRef sp) {
  return llvm::unwrap(sp)->getLinkageName().str().c_str();
}

int DISubprogramDescribesFunction(DISubprogramRef sp, LLVMValueRef fn) {
  return llvm::unwrap(sp)->describes(llvm::cast<llvm::Function>(llvm::unwrap(fn)));
}
} /* extern "C" */

