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

#include <stdlib.h>
#include <llvm-c/Analysis.h>
#include <DebugInfoC.h>
#include <common.h>

codegen g_codegen;

void
codegen_init() {
  g_codegen.module          = LLVMModuleCreateWithName("test");
  g_codegen.di_builder      = DICreateBuilder(g_codegen.module);
  g_codegen.di_compile_unit = DICreateCompilationUnit(g_codegen.di_builder, 4,
                                            "<stdin>", "",
                                            "konanc", 0, "", 0);
  g_codegen.llvm_builder    = LLVMCreateBuilderInContext(LLVMGetModuleContext(g_codegen.module));
}

void
codegen_destroy() {
  DIFinalize(g_codegen.di_builder);
  LLVMVerifyModule(g_codegen.module, LLVMPrintMessageAction, NULL);
  LLVMDumpModule(g_codegen.module);
  LLVMDisposeModule(g_codegen.module);
  LLVMShutdown();
}


static LLVMValueRef
create_function(const char* name) {
  LLVMTypeRef function_type = LLVMFunctionType(LLVMVoidType(), NULL, 0, 0);
  return LLVMAddFunction(g_codegen.module, name, function_type);
}

void
create_function_with_entry(const char *name,
                           location loc,
                           DISubroutineTypeRef subroutine_type,
                           DISubprogramRef *subprogram) {
  LLVMValueRef function = create_function(name);
  LLVMBasicBlockRef bb = LLVMAppendBasicBlock(function, "entry");
  *subprogram = DICreateFunction(g_codegen.di_builder,
                                 SCOPE(g_codegen.di_compile_unit),
                                 name,
                                 name,
                                 *loc.file,
                                 loc.line,
                                 subroutine_type, 0, 1, 0);
  LLVMPositionBuilderAtEnd(g_codegen.llvm_builder, bb);
  DIFunctionAddSubprogram(function, *subprogram);
}

