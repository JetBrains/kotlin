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

#include <DebugInfoC.h>
#include <common.h>
//clang llvmDebugInfoC/test/demo.c -IllvmDebugInfoC/include/ -Idependencies/all/clang+llvm-3.9.0-darwin-macos/include  -c

//c++ demo.o -Idependencies/all/clang+llvm-3.9.0-darwin-macos/include -Ldependencies/all/clang+llvm-3.9.0-darwin-macos/lib  -lLLVMCore -lLLVMSupport -lncurses -o demo

//
//  0:b-backend-dwarf:minamoto@unit-703(0)# clang -S -xc -emit-llvm -g -o - -
//  int foo(int i) { return i; }
//  int main() {
//    int i = 0;
//    return foo(i);
//  }
//  ; ModuleID = '-'
//  source_filename = "-"
//  target datalayout = "e-m:o-i64:64-f80:128-n8:16:32:64-S128"
//  target triple = "x86_64-apple-macosx10.12.0"
//  
//  ; Function Attrs: nounwind ssp uwtable
//  define i32 @foo(i32) #0 !dbg !7 {
//    %2 = alloca i32, align 4
//    store i32 %0, i32* %2, align 4
//    call void @llvm.dbg.declare(metadata i32* %2, metadata !12, metadata !13), !dbg !14
//    %3 = load i32, i32* %2, align 4, !dbg !15
//    ret i32 %3, !dbg !16
//  }
//  
//  ; Function Attrs: nounwind readnone
//  declare void @llvm.dbg.declare(metadata, metadata, metadata) #1
//  
//  ; Function Attrs: nounwind ssp uwtable
//  define i32 @main() #0 !dbg !17 {
//    %1 = alloca i32, align 4
//    %2 = alloca i32, align 4
//    store i32 0, i32* %1, align 4
//    call void @llvm.dbg.declare(metadata i32* %2, metadata !20, metadata !13), !dbg !21
//    store i32 0, i32* %2, align 4, !dbg !21
//    %3 = load i32, i32* %2, align 4, !dbg !22
//    %4 = call i32 @foo(i32 %3), !dbg !23
//    ret i32 %4, !dbg !24
//  }
//  
//  attributes #0 = { nounwind ssp uwtable "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="penryn" "target-features"="+cx16,+fxsr,+mmx,+sse,+sse2,+sse3,+sse4.1,+ssse3" "unsafe-fp-math"="false" "use-soft-float"="false" }
//  attributes #1 = { nounwind readnone }
//  
//  !llvm.dbg.cu = !{!0}
//  !llvm.module.flags = !{!3, !4, !5}
//  !llvm.ident = !{!6}
//  
//  !0 = distinct !DICompileUnit(language: DW_LANG_C99, file: !1, producer: "Apple LLVM version 8.0.0 (clang-800.0.42.1)", isOptimized: false, runtimeVersion: 0, emissionKind: FullDebug, enums: !2)
//  !1 = !DIFile(filename: "-", directory: "/Users/minamoto/ws/.git-trees/backend-dwarf")
//  !2 = !{}
//  !3 = !{i32 2, !"Dwarf Version", i32 2}
//  !4 = !{i32 2, !"Debug Info Version", i32 700000003}
//  !5 = !{i32 1, !"PIC Level", i32 2}
//  !6 = !{!"Apple LLVM version 8.0.0 (clang-800.0.42.1)"}
//  !7 = distinct !DISubprogram(name: "foo", scope: !8, file: !8, line: 1, type: !9, isLocal: false, isDefinition: true, scopeLine: 1, flags: DIFlagPrototyped, isOptimized: false, unit: !0, variables: !2)
//  !8 = !DIFile(filename: "<stdin>", directory: "/Users/minamoto/ws/.git-trees/backend-dwarf")
//  !9 = !DISubroutineType(types: !10)
//  !10 = !{!11, !11}
//  !11 = !DIBasicType(name: "int", size: 32, align: 32, encoding: DW_ATE_signed)
//  !12 = !DILocalVariable(name: "i", arg: 1, scope: !7, file: !8, line: 1, type: !11)
//  !13 = !DIExpression()
//  !14 = !DILocation(line: 1, column: 13, scope: !7)
//  !15 = !DILocation(line: 1, column: 25, scope: !7)
//  !16 = !DILocation(line: 1, column: 18, scope: !7)
//  !17 = distinct !DISubprogram(name: "main", scope: !8, file: !8, line: 2, type: !18, isLocal: false, isDefinition: true, scopeLine: 2, isOptimized: false, unit: !0, variables: !2)
//  !18 = !DISubroutineType(types: !19)
//  !19 = !{!11}
//  !20 = !DILocalVariable(name: "i", scope: !17, file: !8, line: 3, type: !11)
//  !21 = !DILocation(line: 3, column: 7, scope: !17)
//  !22 = !DILocation(line: 4, column: 14, scope: !17)
//  !23 = !DILocation(line: 4, column: 10, scope: !17)
//  !24 = !DILocation(line: 4, column: 3, scope: !17)

int
main() {
  codegen_init();
  DIBasicTypeRef type0 = DICreateBasicType(g_codegen.di_builder, "int", 32, 4, 0);
  DISubroutineTypeRef subroutineType = DICreateSubroutineType(g_codegen.di_builder, (DITypeOpaqueRef *)&type0, 1);
  const char *functionName = "foo";
  DIFileRef file = DICreateFile(g_codegen.di_builder, "1.kt", "src");
  DISubprogramRef diFunction = DICreateFunction(g_codegen.di_builder, SCOPE(g_codegen.di_compile_unit), functionName, "foo:link", file, 66, subroutineType, 0, 1, 0);

  //function creation.

  LLVMTypeRef intType = LLVMInt32Type();
  LLVMValueRef functionType = LLVMFunctionType(intType, &intType, 1, 0);
  LLVMValueRef llvmFunction = LLVMAddFunction(g_codegen.module, functionName, functionType);
  DIFunctionAddSubprogram(llvmFunction, diFunction);
  LLVMBasicBlockRef bb = LLVMAppendBasicBlock(llvmFunction, "entry");
  LLVMPositionBuilderAtEnd(g_codegen.llvm_builder, bb);
  LLVMBuilderSetDebugLocation(g_codegen.llvm_builder, 42, 15, SCOPE(diFunction));
  LLVMValueRef ret = LLVMBuildRet(g_codegen.llvm_builder, LLVMGetParam(llvmFunction, 0));
  codegen_destroy();
  return 0;
}
