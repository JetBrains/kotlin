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
#ifndef __DEMO_COMMON_H__
#define __DEMO_COMMON_H__
typedef struct {
  LLVMModuleRef       module;
  DIBuilderRef        di_builder;
  LLVMBuilderRef      llvm_builder;
  DICompileUnitRef    di_compile_unit;
} codegen;

typedef struct {
  DIFileRef *file;
  int line;
} location;

codegen g_codegen;

#define SCOPE(x) ((DIScopeOpaqueRef)(x))
#define TYPE(x)  ((DITypeOpaqueRef)(x))

void codegen_init();
void codegen_destroy();
void create_function_with_entry(const char *, location, DISubroutineTypeRef, DISubprogramRef *);

#endif
