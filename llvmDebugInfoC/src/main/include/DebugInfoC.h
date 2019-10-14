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

#ifndef __DEBUG_INFO_C_H__
# define __DEBUG_INFO_C_H__
#include <llvm-c/Core.h>
# ifdef __cplusplus
extern "C" {
# endif

typedef struct LLVMOpaqueDIBuilder *DIBuilderRef;
//typedef struct DIBuilder          *DIBuilderRef;
typedef struct DICompileUnit      *DICompileUnitRef;
typedef struct DIFile             *DIFileRef;
typedef struct DIBasicType        *DIBasicTypeRef;
typedef struct DICompositeType    *DICompositeTypeRef;
typedef struct DIDerivedType      *DIDerivedTypeRef;
typedef struct DIType             *DITypeOpaqueRef;
typedef struct DISubprogram       *DISubprogramRef;
typedef struct DIModule           *DIModuleRef;
typedef struct DIScope            *DIScopeOpaqueRef;
typedef struct DISubroutineType   *DISubroutineTypeRef;
//typedef struct DISubprogram       *DISubprogramRef;
typedef struct DILocation         *DILocationRef;
typedef struct DILocalVariable    *DILocalVariableRef;
typedef struct DIExpression       *DIExpressionRef;

DIBuilderRef DICreateBuilder(LLVMModuleRef module);
void DIFinalize(DIBuilderRef builder);
void DIDispose(DIBuilderRef builder);

DICompileUnitRef DICreateCompilationUnit(DIBuilderRef builder, unsigned int lang, const char *File, const char* dir, const char * producer, int isOptimized, const char * flags, unsigned int rv);

DIFileRef DICreateFile(DIBuilderRef builder, const char *filename, const char *directory);

DIBasicTypeRef DICreateBasicType(DIBuilderRef builder, const char* name, uint64_t sizeInBits, uint64_t alignment, unsigned encoding);

DICompositeTypeRef DICreateStructType(DIBuilderRef refBuilder,
                                      DIScopeOpaqueRef scope, const char *name,
                                      DIFileRef file, unsigned lineNumber,
                                      uint64_t sizeInBits, uint64_t alignInBits,
                                      unsigned flags, DITypeOpaqueRef derivedFrom,
                                      DIDerivedTypeRef *elements,
                                      uint64_t elementsCount,
                                      DICompositeTypeRef refPlace);
DICompositeTypeRef DICreateArrayType(DIBuilderRef refBuilder,
                                      uint64_t size, uint64_t alignInBits,
                                      DITypeOpaqueRef type,
                                      uint64_t elementsCount);

DIDerivedTypeRef DICreateReferenceType(DIBuilderRef refBuilder, DITypeOpaqueRef refType);
DIDerivedTypeRef DICreatePointerType(DIBuilderRef refBuilder, DITypeOpaqueRef refType);
DICompositeTypeRef DICreateReplaceableCompositeType(DIBuilderRef refBuilder,
                                                    int tag,
                                                    const char *name,
                                                    DIScopeOpaqueRef refScope,
                                                    DIFileRef refFile,
                                                    unsigned line);
DIDerivedTypeRef DICreateMemberType(DIBuilderRef refBuilder,
                                    DIScopeOpaqueRef refScope,
                                    const char *name,
                                    DIFileRef file,
                                    unsigned lineNum,
                                    uint64_t sizeInBits,
                                    uint64_t alignInBits,
                                    uint64_t offsetInBits,
                                    unsigned flags,
                                    DITypeOpaqueRef type);


DIModuleRef DICreateModule(DIBuilderRef builder, DIScopeOpaqueRef scope,
                           const char* name, const char* configurationMacro,
                           const char* includePath, const char *iSysRoot);

DIScopeOpaqueRef DICreateLexicalBlockFile(DIBuilderRef builderRef, DIScopeOpaqueRef scopeRef, DIFileRef fileRef);

DIScopeOpaqueRef DICreateLexicalBlock(DIBuilderRef builderRef, DIScopeOpaqueRef scopeRef, DIFileRef fileRef, int line, int column);

DISubprogramRef DICreateFunction(DIBuilderRef builder, DIScopeOpaqueRef scope,
                                 const char* name, const char *linkageName,
                                 DIFileRef file, unsigned lineNo,
                                 DISubroutineTypeRef type, int isLocal,
                                 int isDefinition, unsigned scopeLine);

DISubroutineTypeRef DICreateSubroutineType(DIBuilderRef builder,
                                           DITypeOpaqueRef* types,
                                           unsigned typesCount);

DILocalVariableRef DICreateAutoVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, DIFileRef file, unsigned line, DITypeOpaqueRef type);
DILocalVariableRef DICreateParameterVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, unsigned argNo, DIFileRef file, unsigned line, DITypeOpaqueRef type);
void DIInsertDeclaration(DIBuilderRef builder, LLVMValueRef value, DILocalVariableRef localVariable, DILocationRef location, LLVMBasicBlockRef bb, int64_t *expr, uint64_t exprCount);
DIExpressionRef DICreateEmptyExpression(DIBuilderRef builder);
void DIFunctionAddSubprogram(LLVMValueRef fn, DISubprogramRef sp);
DILocationRef LLVMCreateLocation(LLVMContextRef contextRef, unsigned line, unsigned col, DIScopeOpaqueRef scope);
DILocationRef LLVMCreateLocationInlinedAt(LLVMContextRef contextRef, unsigned line, unsigned col, DIScopeOpaqueRef scope, DILocationRef refLocation);
void LLVMBuilderSetDebugLocation(LLVMBuilderRef builder, DILocationRef refLocation);
void LLVMBuilderResetDebugLocation(LLVMBuilderRef builder);
const char* LLVMBuilderGetCurrentBbName(LLVMBuilderRef builder);
const char *DIGetSubprogramLinkName(DISubprogramRef sp);
LLVMValueRef LLVMBuilderGetCurrentFunction(LLVMBuilderRef builder);
int DISubprogramDescribesFunction(DISubprogramRef sp, LLVMValueRef fn);
# ifdef __cplusplus
}
# endif
#endif
