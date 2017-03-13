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

LLVMModuleRef       module;
DIBuilderRef        di_builder;
LLVMBuilderRef      llvm_builder;
DICompileUnitRef    di_compile_unit;
DIFileRef           file;
DISubroutineTypeRef subroutine_type;

static LLVMValueRef
create_function(const char* name) {
  LLVMTypeRef function_type = LLVMFunctionType(LLVMVoidType(), NULL, 0, 0);
  return LLVMAddFunction(module, name, function_type);
}

static DISubprogramRef 
create_function_with_entry(const char *name, int line) {
  LLVMValueRef function = create_function(name);
  LLVMBasicBlockRef bb = LLVMAppendBasicBlock(function, "entry");
  DISubprogramRef   di_function = DICreateFunction(di_builder, di_compile_unit, name, name, file, line,
                   subroutine_type, 0, 1, 0);
  LLVMPositionBuilderAtEnd(llvm_builder, bb);
  DIFunctionAddSubprogram(function, di_function);
  return di_function;
}

#define FOO_FUNCTION "foo"
#define MAIN_FUNCTION "main"
#define MAIN_CALLER_FOO_FUNCTION "main$caller$foo"


static void
create_foo() {
  DISubprogramRef di = create_function_with_entry(FOO_FUNCTION, 5);
  LLVMBuilderSetDebugLocation(llvm_builder, 5, 14, di);
  LLVMBuildRetVoid(llvm_builder);
}

static void
create_main_caller_foo() {
  DISubprogramRef di = create_function_with_entry(MAIN_CALLER_FOO_FUNCTION, 2);
  LLVMValueRef    fn = LLVMGetNamedFunction(module, FOO_FUNCTION);
  LLVMBuilderSetDebugLocation(llvm_builder, 5, 2, di);
  LLVMBuildCall(llvm_builder, fn, NULL, 0, "");
  LLVMBuilderResetDebugLocation(llvm_builder);
  LLVMBuildRetVoid(llvm_builder);
}

static void
create_main() {
  DISubprogramRef di = create_function_with_entry(MAIN_FUNCTION, 1);
  LLVMValueRef    fn = LLVMGetNamedFunction(module, MAIN_CALLER_FOO_FUNCTION);
  LLVMBuilderSetDebugLocation(llvm_builder, 2, 12, di);
  LLVMBuildCall(llvm_builder, fn, NULL, 0, "");
  LLVMBuilderSetDebugLocation(llvm_builder, 3, 4, di);
  LLVMBuildRetVoid(llvm_builder);
}


int
main() {
  module          = LLVMModuleCreateWithName("test");
  di_builder      = DICreateBuilder(module);
  di_compile_unit = DICreateCompilationUnit(di_builder, 4,
                                            "<stdin>", "",
                                            "konanc", 0, "", 0);
  llvm_builder    = LLVMCreateBuilderInContext(LLVMGetModuleContext(module));
  file            = DICreateFile(di_builder, "<stdin>", "");
  subroutine_type = DICreateSubroutineType(di_builder, NULL, 0);
  create_foo();
  create_main_caller_foo();
  create_main();
  DIFinalize(di_builder);

  LLVMVerifyModule(module, LLVMPrintMessageAction, NULL);
  LLVMDumpModule(module);
}
