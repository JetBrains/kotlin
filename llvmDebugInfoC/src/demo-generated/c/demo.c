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
/**
 * this demo produces bitcode for case when several functions generated from one source code.
 * e.g. for following source:
 * 0:123456789012345678901
 * 1: fun main():Int {
 * 2:   {foo()}()
 * 3:   return 0
 * 4: }
 * 5  fun foo() {}
 *
 * we produce following IR (line and column numbers are in parencies)
 * fun main$caller$foo(2:4)
 *     call foo(5:2)
 * fun main(1:2)
 *     call main$caller$foo(2:12)
 *     return0(3:4)
 * fun foo(5:2)
 *     return(5:14)
 */

static DIFileRef           file;
static DISubroutineTypeRef subroutine_type;

#define FOO_FUNCTION "foo"
#define MAIN_FUNCTION "main"
#define MAIN_CALLER_FOO_FUNCTION "main$caller$foo"


static void
create_foo() {
  DISubprogramRef di;
  location loc = {&file, 5};
  create_function_with_entry(FOO_FUNCTION, loc, subroutine_type, &di);
  LLVMBuilderSetDebugLocation(g_codegen.llvm_builder, 5, 14, SCOPE(di));
  LLVMBuildRetVoid(g_codegen.llvm_builder);
}

static void
create_main_caller_foo() {
  DISubprogramRef di;
  location loc = {&file, 2};
  create_function_with_entry(MAIN_CALLER_FOO_FUNCTION, loc, subroutine_type, &di);
  LLVMValueRef    fn = LLVMGetNamedFunction(g_codegen.module, FOO_FUNCTION);
  LLVMBuilderSetDebugLocation(g_codegen.llvm_builder, 5, 2, SCOPE(di));
  LLVMBuildCall(g_codegen.llvm_builder, fn, NULL, 0, "");
  LLVMBuilderResetDebugLocation(g_codegen.llvm_builder);
  LLVMBuildRetVoid(g_codegen.llvm_builder);
}

static void
create_main() {
  DISubprogramRef di;
  location loc = {&file, 1};
  create_function_with_entry(MAIN_FUNCTION, loc, subroutine_type, &di);
  LLVMValueRef    fn = LLVMGetNamedFunction(g_codegen.module, MAIN_CALLER_FOO_FUNCTION);
  LLVMBuilderSetDebugLocation(g_codegen.llvm_builder, 2, 12, SCOPE(di));
  LLVMBuildCall(g_codegen.llvm_builder, fn, NULL, 0, "");
  LLVMBuilderSetDebugLocation(g_codegen.llvm_builder, 3, 4, SCOPE(di));
  LLVMBuildRetVoid(g_codegen.llvm_builder);
}


int
main() {
  codegen_init();
  file            = DICreateFile(g_codegen.di_builder, "<stdin>", "");
  subroutine_type = DICreateSubroutineType(g_codegen.di_builder, NULL, 0);
  create_foo();
  create_main_caller_foo();
  create_main();
  codegen_destroy();
}
